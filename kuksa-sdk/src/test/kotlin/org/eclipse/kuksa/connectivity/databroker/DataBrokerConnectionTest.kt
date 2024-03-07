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
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest
import org.eclipse.kuksa.mocking.FriendlyVssNodeListener
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.DefaultDatabroker
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.vssNode.VssDriver
import org.junit.jupiter.api.Assertions
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class DataBrokerConnectionTest : BehaviorSpec({
    tags(Integration, DefaultDatabroker)

    given("A successfully established connection to the DataBroker") {
        val dataBrokerConnection = connectToDataBrokerBlocking()

        and("A request with a valid VSS Path") {
            val vssPath = "Vehicle.Acceleration.Lateral"
            val field = Types.Field.FIELD_VALUE
            val subscribeRequest = SubscribeRequest(vssPath, field)

            `when`("Subscribing to the VSS path") {
                val vssPathListener = mockk<VssPathListener>(relaxed = true)
                dataBrokerConnection.subscribe(subscribeRequest, vssPathListener)

                then("The #onEntryChanged method is triggered") {
                    val capturingSlot = slot<List<KuksaValV1.EntryUpdate>>()
                    verify(timeout = 100L) {
                        vssPathListener.onEntryChanged(capture(capturingSlot))
                    }

                    val entryUpdates = capturingSlot.captured
                    entryUpdates.size shouldBe 1
                    entryUpdates[0].entry.path shouldBe vssPath
                }

                `when`("The observed VSS path changes") {
                    clearMocks(vssPathListener)

                    val random = Random(System.currentTimeMillis())
                    val newValue = random.nextFloat()
                    val datapoint = Types.Datapoint.newBuilder().setFloat(newValue).build()
                    val updateRequest = UpdateRequest(vssPath, datapoint, field)
                    dataBrokerConnection.update(updateRequest)

                    then("The #onEntryChanged callback is triggered with the new value") {
                        val capturingSlot = slot<List<KuksaValV1.EntryUpdate>>()

                        verify(timeout = 100) {
                            vssPathListener.onEntryChanged(capture(capturingSlot))
                        }

                        val entryUpdates = capturingSlot.captured
                        val capturedDatapoint = entryUpdates[0].entry.value
                        val float = capturedDatapoint.float

                        Assertions.assertEquals(newValue, float, 0.0001f)
                    }
                }
            }

            val validDatapoint = createRandomFloatDatapoint()
            `when`("Updating the DataBroker property (VSS path) with a valid Datapoint") {
                // make sure that the value is set and known to us
                val updateRequest = UpdateRequest(vssPath, validDatapoint, field)
                val response = dataBrokerConnection.update(updateRequest)

                then("No error should appear") {
                    Assertions.assertFalse(response.hasError())
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

            `when`("Fetching the node") {

                and("The initial value is different from the default for a child") {
                    val newHeartRateValue = 60
                    val datapoint = Types.Datapoint.newBuilder().setUint32(newHeartRateValue).build()
                    val updateRequest = UpdateRequest(vssDriver.heartRate.vssPath, datapoint)

                    dataBrokerConnection.update(updateRequest)

                    val fetchRequest = VssNodeFetchRequest(vssDriver)
                    val updatedDriver = dataBrokerConnection.fetch(fetchRequest)

                    then("Every child node has been updated with the correct value") {
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }
            }

            `when`("Subscribing to the node") {
                val vssNodeListener = FriendlyVssNodeListener<VssDriver>()
                val subscribeRequest = VssNodeSubscribeRequest(vssDriver)
                dataBrokerConnection.subscribe(subscribeRequest, listener = vssNodeListener)

                then("The #onNodeChanged method is triggered") {
                    eventually(1.seconds) {
                        vssNodeListener.updatedVssNodes.size shouldBe 1
                    }
                }

                and("The initial value is different from the default for a child") {
                    val newHeartRateValue = 70
                    val datapoint = Types.Datapoint.newBuilder().setUint32(newHeartRateValue).build()
                    val updateRequest = UpdateRequest(vssDriver.heartRate.vssPath, datapoint)

                    dataBrokerConnection.update(updateRequest)

                    then("Every child node has been updated with the correct value") {
                        eventually(1.seconds) {
                            vssNodeListener.updatedVssNodes.size shouldBe 2
                        }

                        val updatedDriver = vssNodeListener.updatedVssNodes.last()
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }

                and("Any subscribed node was changed") {
                    val newHeartRateValue = 50
                    val datapoint = Types.Datapoint.newBuilder().setUint32(newHeartRateValue).build()
                    val updateRequest = UpdateRequest(vssDriver.heartRate.vssPath, datapoint)

                    dataBrokerConnection.update(updateRequest)

                    then("The subscribed vssNode should be updated") {
                        eventually(1.seconds) {
                            vssNodeListener.updatedVssNodes.size shouldBe 3
                        }

                        val updatedDriver = vssNodeListener.updatedVssNodes.last()
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }
            }
        }

        and("An INVALID VSS Path") {
            val invalidVssPath = "Vehicle.Some.Unknown.Path"

            `when`("Trying to subscribe to the INVALID VSS path") {
                val vssPathListener = mockk<VssPathListener>(relaxed = true)
                val subscribeRequest = SubscribeRequest(invalidVssPath)
                dataBrokerConnection.subscribe(subscribeRequest, vssPathListener)

                then("The VssPathListener#onError method should be triggered with 'NOT_FOUND' (Path not found)") {
                    val capturingSlot = slot<Throwable>()
                    verify(timeout = 100L) { vssPathListener.onError(capture(capturingSlot)) }
                    val capturedThrowable = capturingSlot.captured
                    capturedThrowable.message shouldContain "NOT_FOUND"
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

private fun connectToDataBrokerBlocking(): DataBrokerConnection {
    var connection: DataBrokerConnection

    runBlocking {
        val connector = DataBrokerConnectorProvider().createInsecure()
        try {
            connection = connector.connect()
        } catch (ignored: DataBrokerException) {
            val errorMessage = "Could not establish a connection to the DataBroker. " +
                "Check if the DataBroker is running and correctly configured in DataBrokerConfig."
            throw IllegalStateException(errorMessage)
        }
    }

    return connection
}
