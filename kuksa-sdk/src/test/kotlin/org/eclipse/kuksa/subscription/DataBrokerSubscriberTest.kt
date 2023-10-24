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

package org.eclipse.kuksa.subscription

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.kuksa.DataBrokerApiInteraction
import org.eclipse.kuksa.PropertyListener
import org.eclipse.kuksa.VssSpecificationListener
import org.eclipse.kuksa.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.extensions.updateRandomUint32Value
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.pattern.listener.count
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.DataEntry
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.vssSpecification.VssHeartRate

class DataBrokerSubscriberTest : BehaviorSpec({
    tags(Integration, Insecure)

    given("An active Connection to the DataBroker") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val connector = dataBrokerConnectorProvider.createInsecure()
        connector.connect()

        and("An Instance of DataBrokerSubscriber") {
            val databrokerApiInteraction = DataBrokerApiInteraction(dataBrokerConnectorProvider.managedChannel)
            val classUnderTest = DataBrokerSubscriber(databrokerApiInteraction)

            `when`("Subscribing using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val propertyListener = mockk<PropertyListener>(relaxed = true)
                classUnderTest.subscribe(vssPath, fieldValue, propertyListener)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    databrokerApiInteraction.updateRandomFloatValue(vssPath)

                    then("The PropertyListener is notified about the change") {
                        verify(timeout = 100L) {
                            propertyListener.onPropertyChanged(vssPath, fieldValue, any())
                        }
                    }
                }

                `when`("Subscribing the same PropertyListener to a different vssPath") {
                    clearMocks(propertyListener)

                    val otherVssPath = "Vehicle.ADAS.CruiseControl.SpeedSet"
                    classUnderTest.subscribe(otherVssPath, fieldValue, propertyListener)

                    and("Both values are updated") {
                        databrokerApiInteraction.updateRandomFloatValue(vssPath)
                        databrokerApiInteraction.updateRandomFloatValue(otherVssPath)

                        then("The Observer is notified about both changes") {
                            verify(timeout = 100L) {
                                propertyListener.onPropertyChanged(vssPath, fieldValue, any())
                            }
                            verify(timeout = 100L) {
                                propertyListener.onPropertyChanged(otherVssPath, fieldValue, any())
                            }
                        }
                    }
                }

                `when`("Subscribing multiple (different) PropertyListener to $vssPath") {
                    clearMocks(propertyListener)

                    val propertyListenerMocks = mutableListOf<PropertyListener>()
                    repeat(10) {
                        val otherPropertyListenerMock = mockk<PropertyListener>(relaxed = true)
                        propertyListenerMocks.add(otherPropertyListenerMock)

                        classUnderTest.subscribe(vssPath, fieldValue, otherPropertyListenerMock)
                    }

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        val randomFloatValue = databrokerApiInteraction.updateRandomFloatValue(vssPath)

                        then("The PropertyListener is only notified once") {
                            propertyListenerMocks.forEach { propertyListenerMock ->
                                val dataEntries = mutableListOf<DataEntry>()

                                verify(timeout = 100L) {
                                    propertyListenerMock.onPropertyChanged(vssPath, fieldValue, capture(dataEntries))
                                }

                                val count = dataEntries.count { it.value.float == randomFloatValue }
                                count shouldBe 1
                            }
                        }
                    }
                }

                `when`("Unsubscribing the previously registered PropertyListener") {
                    clearMocks(propertyListener)
                    classUnderTest.unsubscribe(vssPath, fieldValue, propertyListener)

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        databrokerApiInteraction.updateRandomFloatValue(vssPath)

                        then("The PropertyListener is not notified") {
                            verify(timeout = 100L, exactly = 0) {
                                propertyListener.onPropertyChanged(vssPath, fieldValue, any())
                            }
                        }
                    }
                }
            }

            `when`("Subscribing the same PropertyListener twice using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val propertyListenerMock = mockk<PropertyListener>(relaxed = true)
                classUnderTest.subscribe(vssPath, fieldValue, propertyListenerMock)
                classUnderTest.subscribe(vssPath, fieldValue, propertyListenerMock)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val randomFloatValue = databrokerApiInteraction.updateRandomFloatValue(vssPath)

                    then("The PropertyListener is only notified once") {
                        val dataEntries = mutableListOf<DataEntry>()

                        verify(timeout = 100L) {
                            propertyListenerMock.onPropertyChanged(vssPath, fieldValue, capture(dataEntries))
                        }

                        val count = dataEntries.count { it.value.float == randomFloatValue }
                        count shouldBe 1
                    }
                }
            }

            val specification = VssHeartRate()

            `when`("Subscribing using VssSpecification to Vehicle.Driver.HeartRate with Field FIELD_VALUE") {
                val specificationObserverMock = mockk<VssSpecificationListener<VssHeartRate>>(relaxed = true)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)

                and("The value of Vehicle.Driver.HeartRate changes") {
                    databrokerApiInteraction.updateRandomFloatValue(specification.vssPath)

                    then("The Observer should be triggered") {
                        verify(timeout = 100) { specificationObserverMock.onSpecificationChanged(any()) }
                    }
                }
            }

            `when`("Subscribing the same SpecificationObserver twice to Vehicle.Driver.HeartRate") {
                val specificationObserverMock = mockk<VssSpecificationListener<VssHeartRate>>(relaxed = true)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue = databrokerApiInteraction.updateRandomUint32Value(specification.vssPath)

                    then("The Observer is only notified once") {
                        val heartRates = mutableListOf<VssHeartRate>()

                        verify(timeout = 100) { specificationObserverMock.onSpecificationChanged(capture(heartRates)) }

                        val count = heartRates.count { it.value == randomIntValue }
                        count shouldBe 1
                    }
                }
            }
        }
    }

    given("An Instance of DataBrokerSubscriber with a mocked DataBrokerApiInteraction") {
        val subscriptionMock = mockk<Subscription>(relaxed = true)
        val dataBrokerApiInteractionMock = mockk<DataBrokerApiInteraction>(relaxed = true)
        val multiListener = MultiListener<PropertyListener>()
        every { dataBrokerApiInteractionMock.subscribe(any(), any()) } returns subscriptionMock
        every { subscriptionMock.observers } returns multiListener
        val classUnderTest = DataBrokerSubscriber(dataBrokerApiInteractionMock)

        `when`("Subscribing for the first time to a vssPath and field") {
            val vssPath = "Vehicle.Speed"
            val field = Types.Field.FIELD_VALUE
            val propertyListenerMock1 = mockk<PropertyListener>()
            val propertyListenerMock2 = mockk<PropertyListener>()
            classUnderTest.subscribe(vssPath, field, propertyListenerMock1)

            then("A new Subscription is created and the PropertyListener is added to the list of Observers") {
                verify {
                    dataBrokerApiInteractionMock.subscribe(vssPath, field)
                }
                multiListener.count() shouldBe 1
            }

            `when`("Another PropertyLisetner subscribes to the same vssPath and field") {
                clearMocks(dataBrokerApiInteractionMock)

                classUnderTest.subscribe(vssPath, field, propertyListenerMock2)

                then("No new Subscription is created and the PropertyListener is added to the list of Observers") {
                    verify(exactly = 0) {
                        dataBrokerApiInteractionMock.subscribe(vssPath, field)
                    }
                    multiListener.count() shouldBe 2
                }
            }

            `when`("One of two PropertyListeners unsubscribes") {
                classUnderTest.unsubscribe(vssPath, field, propertyListenerMock1)

                then("The Subscription is not canceled") {
                    verify(exactly = 0) {
                        subscriptionMock.cancel()
                    }
                    multiListener.count() shouldBe 1
                }

                `when`("The last PropertyListener unsubscribes as well") {
                    classUnderTest.unsubscribe(vssPath, field, propertyListenerMock2)

                    then("There should be no more listeners registered") {
                        multiListener.count() shouldBe 0
                    }

                    then("The Subscription is canceled") {
                        verify { subscriptionMock.cancel() }
                    }
                }
            }
        }
    }
})
