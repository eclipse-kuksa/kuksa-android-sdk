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
 *
 */

package org.eclipse.kuksa

import io.grpc.ManagedChannel
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.eclipse.kuksa.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.kotest.Integration
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.vssSpecification.VssDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.random.Random

class DataBrokerConnectionTest : BehaviorSpec({
    tags(Integration)

    given("A successfully established connection to the DataBroker") {
        val dataBrokerConnection = connectToDataBrokerBlocking()

        and("A Property with a valid VSS Path") {
            val fields = listOf(Types.Field.FIELD_VALUE)
            val property = Property("Vehicle.Acceleration.Lateral", fields)

            `when`("Subscribing to the Property") {
                val properties = listOf(property)

                val propertyObserver = mockk<PropertyObserver>(relaxed = true)
                dataBrokerConnection.subscribe(properties, propertyObserver)

                then("The #onPropertyChanged method is triggered") {
                    verify { propertyObserver.onPropertyChanged(any(), any()) }
                }

                `when`("The observed Property changes") {
                    clearMocks(propertyObserver)

                    val random = Random(System.currentTimeMillis())
                    val newValue = random.nextFloat()
                    val datapoint = Datapoint.newBuilder().setFloat(newValue).build()
                    dataBrokerConnection.update(property, datapoint)

                    then("The #onPropertyChanged callback is triggered with the new value") {
                        val capturingSlot = slot<Types.DataEntry>()

                        verify { propertyObserver.onPropertyChanged(any(), capture(capturingSlot)) }

                        val dataEntry = capturingSlot.captured
                        val capturedDatapoint = dataEntry.value
                        val float = capturedDatapoint.float

                        assertEquals(newValue, float, 0.0001f)
                    }

                    `when`("The same value is set again") {
                        clearMocks(propertyObserver)

                        dataBrokerConnection.update(property, datapoint)

                        then("The #onPropertyChangedCallback should NOT be triggered again") {
                            verify(exactly = 0) { propertyObserver.onPropertyChanged(any(), any()) }
                        }
                    }
                }
            }

            val validDatapoint = createRandomFloatDatapoint()
            `when`("Updating the Property with a valid Datapoint") {
                // make sure that the value is set and known to us
                val response = dataBrokerConnection.update(property, validDatapoint)

                then("No error should appear") {
                    assertFalse(response.hasError())
                }

                and("When fetching it afterwards") {
                    val response1 = dataBrokerConnection.fetch(property)

                    then("The response contains the correctly set value") {
                        val entriesList = response1.entriesList
                        val first = entriesList.first()
                        val capturedValue = first.value
                        assertEquals(validDatapoint.float, capturedValue.float, 0.0001F)
                    }
                }
            }

            `when`("Updating the Property with a Datapoint of a wrong/different type") {
                val datapoint = createRandomIntDatapoint()
                val response = dataBrokerConnection.update(property, datapoint)

                then("It should fail with an errorCode 400 (type mismatch)") {
                    val errorsList = response.errorsList
                    assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 400
                }

                and("Fetching it afterwards") {
                    val getResponse = dataBrokerConnection.fetch(property)

                    then("The response contains the correctly set value") {
                        val entriesList = getResponse.entriesList
                        val first = entriesList.first()
                        val capturedValue = first.value
                        assertEquals(validDatapoint.float, capturedValue.float, 0.0001F)
                    }
                }
            }
        }

        and("A Specification") {
            val specification = VssDriver()
            val property = Property(specification.heartRate.vssPath)

            `when`("Fetching the specification") {

                and("The initial value is different from the default for a child") {
                    val newHeartRateValue = 60
                    val datapoint = Datapoint.newBuilder().setUint32(newHeartRateValue).build()

                    dataBrokerConnection.update(property, datapoint)

                    then("Every child property has been updated with the correct value") {
                        val updatedDriver = dataBrokerConnection.fetch(specification)
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }
            }

            `when`("Subscribing to the specification") {
                val propertyObserver = mockk<VssSpecificationObserver<VssDriver>>(relaxed = true)
                dataBrokerConnection.subscribe(specification, observer = propertyObserver)

                then("The #onSpecificationChanged method is triggered") {
                    verify { propertyObserver.onSpecificationChanged(any()) }
                }

                and("The initial value is different from the default for a child") {
                    val newHeartRateValue = 70
                    val datapoint = Datapoint.newBuilder().setUint32(newHeartRateValue).build()

                    dataBrokerConnection.update(property, datapoint)

                    then("Every child property has been updated with the correct value") {
                        val capturingSlots = mutableListOf<VssDriver>()

                        verify(exactly = 2) { propertyObserver.onSpecificationChanged(capture(capturingSlots)) }

                        val updatedDriver = capturingSlots[1]
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }

                and("Any subscribed Property was changed") {
                    val newHeartRateValue = 50
                    val datapoint = Datapoint.newBuilder().setUint32(newHeartRateValue).build()

                    dataBrokerConnection.update(property, datapoint)

                    then("The subscribed Specification should be updated") {
                        val capturingSlots = mutableListOf<VssDriver>()

                        verify(exactly = 3) { propertyObserver.onSpecificationChanged(capture(capturingSlots)) }

                        val updatedDriver = capturingSlots[2]
                        val heartRate = updatedDriver.heartRate

                        heartRate.value shouldBe newHeartRateValue
                    }
                }
            }
        }

        and("A Property with an INVALID VSS Path") {
            val fields = listOf(Types.Field.FIELD_VALUE)
            val property = Property("Vehicle.Some.Unknown.Path", fields)

            `when`("Trying to subscribe to the INVALID Property") {
                val properties = listOf(property)

                val propertyObserver = mockk<PropertyObserver>(relaxed = true)
                dataBrokerConnection.subscribe(properties, propertyObserver)

                then("No crash should happen") {
                    // ignored
                }
            }

            `when`("Trying to update the INVALID property") {
                // make sure that the value is set and known to us
                val datapoint = createRandomFloatDatapoint()
                val response = dataBrokerConnection.update(property, datapoint)

                then("It should fail with an errorCode 404 (path not found)") {
                    val errorsList = response.errorsList
                    assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 404
                }
            }

            `when`("Trying to fetch the INVALID property") {
                val response = dataBrokerConnection.fetch(property)

                then("The response should not contain any entries") {
                    assertEquals(0, response.entriesList.size)
                }

                then("The response should contain an error with errorCode 404 (path not found)") {
                    val errorsList = response.errorsList
                    assertTrue(errorsList.size > 0)

                    val error = errorsList[0].error

                    error.code shouldBe 404
                }
            }
        }
    }
    given("A DataBrokerConnection with a mocked ManagedChannel") {
        val managedChannel = mockk<ManagedChannel>(relaxed = true)
        val dataBrokerConnection = DataBrokerConnection(managedChannel)

        `when`("Disconnect is called") {
            dataBrokerConnection.disconnect()

            then("The Channel is shutDown") {
                verify { managedChannel.shutdown() }
            }
        }
    }
})

private fun createRandomFloatDatapoint(): Datapoint {
    val random = Random(System.currentTimeMillis())
    val newValue = random.nextFloat()
    return Datapoint.newBuilder().setFloat(newValue).build()
}

private fun createRandomIntDatapoint(): Datapoint {
    val random = Random(System.currentTimeMillis())
    val newValue = random.nextInt()
    return Datapoint.newBuilder().setInt32(newValue).build()
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
