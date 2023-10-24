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
import org.eclipse.kuksa.PropertyObserver
import org.eclipse.kuksa.VssSpecificationObserver
import org.eclipse.kuksa.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.extensions.setRandomFloatValue
import org.eclipse.kuksa.extensions.setRandomUint32Value
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
                val propertyObserverMock = mockk<PropertyObserver>(relaxed = true)
                classUnderTest.subscribe(vssPath, fieldValue, propertyObserverMock)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    databrokerApiInteraction.setRandomFloatValue(vssPath)

                    then("The PropertyObserver is notified about the change") {
                        verify(timeout = 100L) {
                            propertyObserverMock.onPropertyChanged(vssPath, fieldValue, any())
                        }
                    }
                }

                `when`("Subscribing the same PropertyObserver to a different vssPath") {
                    clearMocks(propertyObserverMock)

                    val otherVssPath = "Vehicle.ADAS.CruiseControl.SpeedSet"
                    classUnderTest.subscribe(otherVssPath, fieldValue, propertyObserverMock)

                    and("Both values are updated") {
                        databrokerApiInteraction.setRandomFloatValue(vssPath)
                        databrokerApiInteraction.setRandomFloatValue(otherVssPath)

                        then("The Observer is notified about both changes") {
                            verify(timeout = 100L) {
                                propertyObserverMock.onPropertyChanged(vssPath, fieldValue, any())
                            }
                            verify(timeout = 100L) {
                                propertyObserverMock.onPropertyChanged(otherVssPath, fieldValue, any())
                            }
                        }
                    }
                }

                `when`("Subscribing multiple (different) PropertyObserver to $vssPath") {
                    clearMocks(propertyObserverMock)

                    val propertyObserverMocks = mutableListOf<PropertyObserver>()
                    repeat(10) {
                        val otherPropertyObserverMock = mockk<PropertyObserver>(relaxed = true)
                        propertyObserverMocks.add(otherPropertyObserverMock)

                        classUnderTest.subscribe(vssPath, fieldValue, otherPropertyObserverMock)
                    }

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        val randomFloatValue = databrokerApiInteraction.setRandomFloatValue(vssPath)

                        then("The PropertyObserver is only notified once") {
                            propertyObserverMocks.forEach { propertyObserverMock ->
                                val dataEntries = mutableListOf<DataEntry>()

                                verify(timeout = 100L) {
                                    propertyObserverMock.onPropertyChanged(vssPath, fieldValue, capture(dataEntries))
                                }

                                val count = dataEntries.count { it.value.float == randomFloatValue }
                                count shouldBe 1
                            }
                        }
                    }
                }

                `when`("Unsubscribing the previously registered PropertyObserver") {
                    clearMocks(propertyObserverMock)
                    classUnderTest.unsubscribe(vssPath, fieldValue, propertyObserverMock)

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        databrokerApiInteraction.setRandomFloatValue(vssPath)

                        then("The PropertyObserver is not notified") {
                            verify(timeout = 100L, exactly = 0) {
                                propertyObserverMock.onPropertyChanged(vssPath, fieldValue, any())
                            }
                        }
                    }
                }
            }

            `when`("Subscribing the same PropertyObserver twice using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val propertyObserverMock = mockk<PropertyObserver>(relaxed = true)
                classUnderTest.subscribe(vssPath, fieldValue, propertyObserverMock)
                classUnderTest.subscribe(vssPath, fieldValue, propertyObserverMock)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val randomFloatValue = databrokerApiInteraction.setRandomFloatValue(vssPath)

                    then("The PropertyObserver is only notified once") {
                        val dataEntries = mutableListOf<DataEntry>()

                        verify(timeout = 100L) {
                            propertyObserverMock.onPropertyChanged(vssPath, fieldValue, capture(dataEntries))
                        }

                        val count = dataEntries.count { it.value.float == randomFloatValue }
                        count shouldBe 1
                    }
                }
            }

            val specification = VssHeartRate()

            `when`("Subscribing using VssSpecification to Vehicle.Driver.HeartRate with Field FIELD_VALUE") {
                val specificationObserverMock = mockk<VssSpecificationObserver<VssHeartRate>>(relaxed = true)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)

                and("The value of Vehicle.Driver.HeartRate changes") {
                    databrokerApiInteraction.setRandomFloatValue(specification.vssPath)

                    then("The Observer should be triggered") {
                        verify(timeout = 100) { specificationObserverMock.onSpecificationChanged(any()) }
                    }
                }
            }

            `when`("Subscribing the same SpecificationObserver twice to Vehicle.Driver.HeartRate") {
                val specificationObserverMock = mockk<VssSpecificationObserver<VssHeartRate>>(relaxed = true)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)
                classUnderTest.subscribe(specification, Types.Field.FIELD_VALUE, specificationObserverMock)

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue = databrokerApiInteraction.setRandomUint32Value(specification.vssPath)

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
        val multiListener = MultiListener<PropertyObserver>()
        every { dataBrokerApiInteractionMock.subscribe(any(), any()) } returns subscriptionMock
        every { subscriptionMock.observers } returns multiListener
        val classUnderTest = DataBrokerSubscriber(dataBrokerApiInteractionMock)

        `when`("Subscribing for the first time to a vssPath and field") {
            val vssPath = "Vehicle.Speed"
            val field = Types.Field.FIELD_VALUE
            val propertyObserverMock1 = mockk<PropertyObserver>()
            val propertyObserverMock2 = mockk<PropertyObserver>()
            classUnderTest.subscribe(vssPath, field, propertyObserverMock1)

            then("A new Subscription is created and the PropertyObserver is added to the list of Observers") {
                verify {
                    dataBrokerApiInteractionMock.subscribe(vssPath, field)
                }
                multiListener.count() shouldBe 1
            }

            `when`("Another PropertyObserver subscribes to the same vssPath and field") {
                clearMocks(dataBrokerApiInteractionMock)

                classUnderTest.subscribe(vssPath, field, propertyObserverMock2)

                then("No new Subscription is created and the PropertyObserver is added to the list of Observers") {
                    verify(exactly = 0) {
                        dataBrokerApiInteractionMock.subscribe(vssPath, field)
                    }
                    multiListener.count() shouldBe 2
                }
            }

            `when`("One of two PropertyObservers unsubscribes") {
                classUnderTest.unsubscribe(vssPath, field, propertyObserverMock1)

                then("The Subscription is not canceled") {
                    verify(exactly = 0) {
                        subscriptionMock.cancel()
                    }
                    multiListener.count() shouldBe 1
                }

                `when`("The last PropertyObserver unsubscribes as well") {
                    classUnderTest.unsubscribe(vssPath, field, propertyObserverMock2)

                    then("There should be no more observers registered") {
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
