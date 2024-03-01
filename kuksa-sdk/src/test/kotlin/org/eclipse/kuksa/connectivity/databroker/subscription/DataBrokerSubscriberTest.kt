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

package org.eclipse.kuksa.connectivity.databroker.subscription

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import org.eclipse.kuksa.connectivity.databroker.DataBrokerTransporter
import org.eclipse.kuksa.connectivity.databroker.listener.PropertyListener
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.extensions.toggleBoolean
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.extensions.updateRandomUint32Value
import org.eclipse.kuksa.mocking.FriendlyPropertyListener
import org.eclipse.kuksa.mocking.FriendlyVssNodeListener
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.pattern.listener.count
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.DefaultDatabroker
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.vssNode.VssDriver
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DataBrokerSubscriberTest : BehaviorSpec({
    tags(Integration, Insecure, DefaultDatabroker)

    given("An active Connection to the DataBroker") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val connector = dataBrokerConnectorProvider.createInsecure()
        connector.connect()

        and("An Instance of DataBrokerSubscriber") {
            val databrokerTransporter =
                DataBrokerTransporter(dataBrokerConnectorProvider.managedChannel)
            val classUnderTest = DataBrokerSubscriber(databrokerTransporter)

            `when`("Subscribing to VSS_PATH (Branch) 'Vehicle.ADAS.ABS'") {
                val vssPath = "Vehicle.ADAS.ABS"
                val fieldValue = Types.Field.FIELD_VALUE
                val propertyListener = FriendlyPropertyListener()
                classUnderTest.subscribe(vssPath, fieldValue, propertyListener)

                then("The PropertyListener should send out ONE update containing ALL children") {
                    eventually(1.seconds) {
                        propertyListener.updates.size shouldBe 1
                    }

                    val entryUpdates = propertyListener.updates[0]
                    propertyListener.updates.size shouldBe 1 // ONE update
                    entryUpdates.size shouldBe 3 // all children: IsEnabled, IsEngaged, IsError
                    entryUpdates.all { it.entry.path.startsWith(vssPath) } shouldBe true
                }

                `when`("Any child changes it's value") {
                    propertyListener.reset()

                    val vssPathIsError = "Vehicle.ADAS.ABS.IsError"
                    val newValueIsError = databrokerTransporter.toggleBoolean(vssPathIsError)
                    val vssPathIsEngaged = "Vehicle.ADAS.ABS.IsEngaged"
                    val newValueIsEngaged = databrokerTransporter.toggleBoolean(vssPathIsEngaged)

                    then("The PropertyListener should be notified about it") {
                        eventually(1.seconds) {
                            propertyListener.updates.size shouldBe 2
                        }

                        val entryUpdates = propertyListener.updates.flatten()
                        entryUpdates.count {
                            val path = it.entry.path
                            val entry = it.entry
                            val value = entry.value
                            path == vssPathIsError && value.bool == newValueIsError
                        } shouldBe 1
                        entryUpdates.count {
                            val path = it.entry.path
                            val entry = it.entry
                            val value = entry.value
                            path == vssPathIsEngaged && value.bool == newValueIsEngaged
                        } shouldBe 1
                    }
                }
            }

            `when`("Subscribing using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val propertyListener = FriendlyPropertyListener()
                classUnderTest.subscribe(vssPath, fieldValue, propertyListener)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val updateRandomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                    then("The PropertyListener is notified about the change") {
                        eventually(1.seconds) {
                            propertyListener.updates.size shouldBe 2
                        }
                        propertyListener.updates.flatten()
                            .count {
                                val dataEntry = it.entry
                                val datapoint = dataEntry.value
                                dataEntry.path == vssPath && datapoint.float == updateRandomFloatValue
                            } shouldBe 1

                        propertyListener.updates.clear()
                    }
                }

                `when`("Subscribing the same PropertyListener to a different vssPath") {

                    val otherVssPath = "Vehicle.ADAS.CruiseControl.SpeedSet"
                    classUnderTest.subscribe(otherVssPath, fieldValue, propertyListener)

                    and("Both values are updated") {
                        val updatedValueVssPath = databrokerTransporter.updateRandomFloatValue(vssPath)
                        val updatedValueOtherVssPath = databrokerTransporter.updateRandomFloatValue(otherVssPath)

                        then("The Observer is notified about both changes") {
                            eventually(1.seconds) {
                                propertyListener.updates.size shouldBe 3 // 1 from subscribe(otherVssPath) + 2 updates
                            }

                            val entryUpdates = propertyListener.updates.flatten()
                            entryUpdates
                                .count {
                                    val path = it.entry.path
                                    val entry = it.entry
                                    val value = entry.value
                                    path == vssPath && value.float == updatedValueVssPath
                                } shouldBe 1
                            entryUpdates
                                .count {
                                    val path = it.entry.path
                                    val entry = it.entry
                                    val value = entry.value
                                    path == otherVssPath && value.float == updatedValueOtherVssPath
                                } shouldBe 1
                        }
                    }
                }

                `when`("Subscribing multiple (different) PropertyListener to $vssPath") {
                    val friendlyPropertyListeners = mutableListOf<FriendlyPropertyListener>()
                    repeat(10) {
                        val otherPropertyListenerMock = FriendlyPropertyListener()
                        friendlyPropertyListeners.add(otherPropertyListenerMock)

                        classUnderTest.subscribe(vssPath, fieldValue, otherPropertyListenerMock)
                    }

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        val randomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                        then("Each PropertyListener is only notified once") {
                            friendlyPropertyListeners.forEach { friendlyPropertyListener ->
                                eventually(1.seconds) {
                                    friendlyPropertyListener.updates.size shouldBe 2
                                }

                                val count = friendlyPropertyListener.updates
                                    .count { it[0].entry.value.float == randomFloatValue }
                                count shouldBe 1
                            }
                        }
                    }
                }

                `when`("Unsubscribing the previously registered PropertyListener") {
                    propertyListener.reset()
                    classUnderTest.unsubscribe(vssPath, fieldValue, propertyListener)

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        databrokerTransporter.updateRandomFloatValue(vssPath)
                        delay(100)

                        then("The PropertyListener is not notified") {
                            continually(100.milliseconds) {
                                propertyListener.updates.size shouldBe 0
                            }
                        }
                    }
                }
            }

            `when`("Subscribing the same PropertyListener twice using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val friendlyPropertyListener = FriendlyPropertyListener()
                classUnderTest.subscribe(vssPath, fieldValue, friendlyPropertyListener)
                classUnderTest.subscribe(vssPath, fieldValue, friendlyPropertyListener)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val randomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                    then("The PropertyListener is only notified once") {
                        eventually(1.seconds) {
                            friendlyPropertyListener.updates.size shouldBe 2
                        }

                        val count = friendlyPropertyListener.updates
                            .count { it[0].entry.value.float == randomFloatValue }
                        count shouldBe 1
                    }
                }
            }

            val specification = VssDriver.VssHeartRate()

            `when`("Subscribing using a VSS node to Vehicle.Driver.HeartRate with Field FIELD_VALUE") {
                val friendlyVssNodeListener = FriendlyVssNodeListener<VssDriver.VssHeartRate>()
                classUnderTest.subscribe(
                    specification,
                    Types.Field.FIELD_VALUE,
                    friendlyVssNodeListener,
                )

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue =
                        databrokerTransporter.updateRandomUint32Value(specification.vssPath)

                    then("The Observer should be triggered") {
                        eventually(1.seconds) {
                            friendlyVssNodeListener.updatedSpecifications.size shouldBe 2
                        }

                        val count = friendlyVssNodeListener.updatedSpecifications
                            .count { it.value == randomIntValue }
                        count shouldBe 1
                    }
                }
            }

            `when`("Subscribing the same SpecificationObserver twice to Vehicle.Driver.HeartRate") {
                val specificationObserverMock = FriendlyVssNodeListener<VssDriver.VssHeartRate>()
                classUnderTest.subscribe(
                    specification,
                    Types.Field.FIELD_VALUE,
                    specificationObserverMock,
                )
                classUnderTest.subscribe(
                    specification,
                    Types.Field.FIELD_VALUE,
                    specificationObserverMock,
                )

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue =
                        databrokerTransporter.updateRandomUint32Value(specification.vssPath)

                    then("The Observer is only notified once") {
                        eventually(1.seconds) {
                            specificationObserverMock.updatedSpecifications.size shouldBe 2
                        }

                        val count = specificationObserverMock.updatedSpecifications.count { it.value == randomIntValue }
                        count shouldBe 1
                    }
                }
            }
        }
    }

    given("An Instance of DataBrokerSubscriber with a mocked DataBrokerTransporter") {
        val subscriptionMock = mockk<DataBrokerSubscription>(relaxed = true)
        val dataBrokerTransporterMock = mockk<DataBrokerTransporter>(relaxed = true)
        val multiListener = MultiListener<PropertyListener>()
        every { dataBrokerTransporterMock.subscribe(any(), any()) } returns subscriptionMock
        every { subscriptionMock.listeners } returns multiListener
        val classUnderTest = DataBrokerSubscriber(dataBrokerTransporterMock)

        `when`("Subscribing for the first time to a vssPath and field") {
            val vssPath = "Vehicle.Speed"
            val field = Types.Field.FIELD_VALUE
            val propertyListenerMock1 = mockk<PropertyListener>()
            val propertyListenerMock2 = mockk<PropertyListener>()
            classUnderTest.subscribe(vssPath, field, propertyListenerMock1)

            then("A new Subscription is created and the PropertyListener is added to the list of Listeners") {
                verify {
                    dataBrokerTransporterMock.subscribe(vssPath, field)
                }
                multiListener.count() shouldBe 1
            }

            `when`("Another PropertyListener subscribes to the same vssPath and field") {
                clearMocks(dataBrokerTransporterMock)

                classUnderTest.subscribe(vssPath, field, propertyListenerMock2)

                then("No new Subscription is created and the PropertyListener is added to the list of Listeners") {
                    verify(exactly = 0) {
                        dataBrokerTransporterMock.subscribe(vssPath, field)
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
