/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.kuksa.connectivity.databroker

import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.InsecureDataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeUpdateRequest
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.mocking.FriendlyVssNodeListener
import org.eclipse.kuksa.mocking.FriendlyVssPathListener
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.extension.equals
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.InsecureDataBroker
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.test.kotest.eventuallyConfiguration
import org.eclipse.kuksa.vssNode.VssDriver
import org.junit.jupiter.api.Assertions
import kotlin.random.Random

class DataBrokerConnectionTest : BehaviorSpec({
    tags(Integration, Insecure, InsecureDataBroker)

    var databrokerContainer: DataBrokerDockerContainer? = null
    beforeSpec {
        databrokerContainer = InsecureDataBrokerDockerContainer()
            .apply {
                start()
            }
    }

    afterSpec {
        databrokerContainer?.stop()
    }

    given("A successfully established connection to the DataBroker") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val connector = dataBrokerConnectorProvider.createInsecure()
        val dataBrokerConnection = connector.connect()

        val dataBrokerTransporter = DataBrokerTransporter(dataBrokerConnectorProvider.managedChannel)

        and("A request with a valid VSS Path") {
            val vssPath = "Vehicle.Acceleration.Lateral"
            val field = Types.Field.FIELD_VALUE

            val initialValue = dataBrokerTransporter.updateRandomFloatValue(vssPath)

            val subscribeRequest = SubscribeRequest(vssPath, field)
            `when`("Subscribing to the VSS path") {
                val vssPathListener = FriendlyVssPathListener()
                dataBrokerConnection.subscribe(subscribeRequest, vssPathListener)

                then("The #onEntryChanged method is triggered") {
                    eventually(eventuallyConfiguration) {
                        vssPathListener.updates.flatten().count {
                            val entry = it.entry
                            val value = entry.value
                            entry.path == vssPath && value.float.equals(initialValue, 0.0001f)
                        } shouldBe 1
                    }
                }

                `when`("The observed VSS path changes") {
                    vssPathListener.reset()

                    val random = Random(System.currentTimeMillis())
                    val updatedValue = random.nextFloat()
                    val datapoint = Types.Datapoint.newBuilder().setFloat(updatedValue).build()
                    val updateRequest = UpdateRequest(vssPath, datapoint, field)
                    dataBrokerConnection.update(updateRequest)

                    then("The #onEntryChanged callback is triggered with the new value") {
                        eventually(eventuallyConfiguration) {
                            vssPathListener.updates.flatten().count {
                                val entry = it.entry
                                val value = entry.value
                                entry.path == vssPath && value.float.equals(updatedValue, 0.0001f)
                            } shouldBe 1
                        }
                    }
                }
            }

            val validDatapoint = createRandomFloatDatapoint()
            `when`("Updating the DataBroker property (VSS path) with a valid Datapoint") {
                // make sure that the value is set and known to us
                val updateRequest = UpdateRequest(vssPath, validDatapoint, field)
                val response = dataBrokerConnection.update(updateRequest)

                then("No error should appear") {
                    response.hasError() shouldBe false
                }

                and("When fetching it afterwards") {
                    val fetchRequest = FetchRequest(vssPath)
                    val response1 = dataBrokerConnection.fetch(fetchRequest)

                    then("The response contains the correctly set value") {
                        val entriesList = response1.entriesList
                        val first = entriesList.first()
                        val capturedValue = first.value
                        Assertions.assertEquals(validDatapoint.float, capturedValue.float, 0.0001F)
                    }
                }
            }

            `when`("Updating the DataBroker property (VSS path) with a Datapoint of a wrong/different type") {
                val datapoint = createRandomIntDatapoint()
                val updateRequest = UpdateRequest(vssPath, datapoint)
                val response = dataBrokerConnection.update(updateRequest)

                then("It should fail with an errorCode 400 (type mismatch)") {
                    val errorsList = response.errorsList
                    Assertions.assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 400
                }

                and("Fetching it afterwards") {
                    val fetchRequest = FetchRequest(vssPath)
                    val getResponse = dataBrokerConnection.fetch(fetchRequest)

                    then("The response contains the correctly set value") {
                        val entriesList = getResponse.entriesList
                        val first = entriesList.first()
                        val capturedValue = first.value
                        Assertions.assertEquals(validDatapoint.float, capturedValue.float, 0.0001F)
                    }
                }
            }
        }

        and("A VssNode") {
            val vssDriver = VssDriver()

            and("A default HeartRate") {
                val newHeartRateValue = 60
                val datapoint = Types.Datapoint.newBuilder().setUint32(newHeartRateValue).build()
                val defaultUpdateRequest = UpdateRequest(vssDriver.heartRate.vssPath, datapoint)

                dataBrokerConnection.update(defaultUpdateRequest)

                `when`("Fetching the node") {

                    and("The initial value is different from the default for a child") {
                        val fetchRequest = VssNodeFetchRequest(vssDriver)
                        val updatedDriver = dataBrokerConnection.fetch(fetchRequest)

                        then("Every child node has been updated with the correct value") {
                            val heartRate = updatedDriver.heartRate

                            heartRate.value shouldBe newHeartRateValue
                        }
                    }
                }

                `when`("Updating the node with an invalid value") {
                    val invalidHeartRate = VssDriver.VssHeartRate(-5) // UInt on DataBroker side
                    val vssNodeUpdateRequest = VssNodeUpdateRequest(invalidHeartRate)
                    val response = dataBrokerConnection.update(vssNodeUpdateRequest)

                    then("the update response should contain an error") {
                        val errorResponse = response.firstOrNull { it.errorsCount >= 1 }
                        errorResponse shouldNotBe null
                    }
                }
            }

            `when`("Subscribing to the node") {
                val vssNodeListener = FriendlyVssNodeListener<VssDriver>()
                val subscribeRequest = VssNodeSubscribeRequest(vssDriver)
                dataBrokerConnection.subscribe(subscribeRequest, listener = vssNodeListener)

                then("The #onNodeChanged method is triggered") {
                    eventually(eventuallyConfiguration) {
                        vssNodeListener.updatedVssNodes.size shouldBe 1
                    }
                }

                and("The initial value is different from the default for a child") {
                    val newHeartRateValue = 70
                    val datapoint = Types.Datapoint.newBuilder().setUint32(newHeartRateValue).build()
                    val updateRequest = UpdateRequest(vssDriver.heartRate.vssPath, datapoint)

                    dataBrokerConnection.update(updateRequest)

                    then("Every child node has been updated with the correct value") {
                        eventually(eventuallyConfiguration) {
                            vssNodeListener.updatedVssNodes.count {
                                val heartRate = it.heartRate
                                heartRate.value == newHeartRateValue
                            } shouldBe 1
                        }
                    }
                }

                and("Any subscribed uInt node was changed") {
                    val newHeartRateValue = 50
                    val newVssHeartRate = VssDriver.VssHeartRate(newHeartRateValue)
                    val updateRequest = VssNodeUpdateRequest(newVssHeartRate)

                    dataBrokerConnection.update(updateRequest)

                    then("The subscribed vssNode should be updated") {
                        eventually(eventuallyConfiguration) {
                            vssNodeListener.updatedVssNodes.count {
                                val heartRate = it.heartRate
                                heartRate.value == newHeartRateValue
                            } shouldBe 1
                        }
                    }
                }
            }
        }

        and("An INVALID VSS Path") {
            val invalidVssPath = "Vehicle.Some.Unknown.Path"

            `when`("Trying to subscribe to the INVALID VSS path") {
                val vssPathListener = FriendlyVssPathListener()
                val subscribeRequest = SubscribeRequest(invalidVssPath)
                dataBrokerConnection.subscribe(subscribeRequest, vssPathListener)

                then("The VssPathListener#onError method should be triggered with 'NOT_FOUND' (Path not found)") {
                    eventually(eventuallyConfiguration) {
                        vssPathListener.errors.count {
                            it.message?.contains("NOT_FOUND") == true
                        } shouldBe 1
                    }
                }
            }

            `when`("Trying to update the INVALID VSS Path") {
                // make sure that the value is set and known to us
                val datapoint = createRandomFloatDatapoint()
                val updateRequest = UpdateRequest(invalidVssPath, datapoint)
                val response = dataBrokerConnection.update(updateRequest)

                then("It should fail with an errorCode 404 (path not found)") {
                    val errorsList = response.errorsList
                    Assertions.assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 404
                }
            }

            `when`("Trying to fetch the INVALID VSS path") {
                val fetchRequest = FetchRequest(invalidVssPath)
                val response = dataBrokerConnection.fetch(fetchRequest)

                then("The response should not contain any entries") {
                    Assertions.assertEquals(0, response.entriesList.size)
                }

                then("The response should contain an error with errorCode 404 (path not found)") {
                    val errorsList = response.errorsList
                    Assertions.assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 404
                }
            }
        }

        // this test closes the connection, the connection can't be used afterward anymore
        `when`("A DisconnectListener is registered successfully") {
            val disconnectListener = mockk<DisconnectListener>(relaxed = true)
            val disconnectListeners = dataBrokerConnection.disconnectListeners
            disconnectListeners.register(disconnectListener)

            `when`("The Connection is closed manually") {
                dataBrokerConnection.disconnect()

                then("The DisconnectListener is triggered") {
                    verify { disconnectListener.onDisconnect() }
                }
            }
        }
        // connection is closed at this point
    }
    given("A DataBrokerConnection with a mocked ManagedChannel") {
        val managedChannel = mockk<ManagedChannel>(relaxed = true)
        every { managedChannel.getState(any()) }.returns(ConnectivityState.READY)
        val dataBrokerConnection = DataBrokerConnection(managedChannel)

        `when`("Disconnect is called") {
            dataBrokerConnection.disconnect()

            then("The Channel is shutDown") {
                verify { managedChannel.shutdownNow() }
            }
        }
    }
})

private fun createRandomFloatDatapoint(): Types.Datapoint {
    val random = Random(System.currentTimeMillis())
    val newValue = random.nextFloat()
    return Types.Datapoint.newBuilder().setFloat(newValue).build()
}

private fun createRandomIntDatapoint(): Types.Datapoint {
    val random = Random(System.currentTimeMillis())
    val newValue = random.nextInt()
    return Types.Datapoint.newBuilder().setInt32(newValue).build()
}
