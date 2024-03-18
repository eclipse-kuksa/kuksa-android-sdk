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
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.connectivity.databroker.DataBrokerTransporter
import org.eclipse.kuksa.connectivity.databroker.docker.DataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.docker.InsecureDataBrokerDockerContainer
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener
import org.eclipse.kuksa.extensions.toggleBoolean
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.extensions.updateRandomUint32Value
import org.eclipse.kuksa.mocking.FriendlyVssNodeListener
import org.eclipse.kuksa.mocking.FriendlyVssPathListener
import org.eclipse.kuksa.pattern.listener.MultiListener
import org.eclipse.kuksa.pattern.listener.count
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.InsecureDataBroker
import org.eclipse.kuksa.test.kotest.Integration
import org.eclipse.kuksa.test.kotest.continuallyConfiguration
import org.eclipse.kuksa.test.kotest.eventuallyConfiguration
import org.eclipse.kuksa.vssNode.VssDriver

class DataBrokerSubscriberTest : BehaviorSpec({
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
                val vssPathListener = FriendlyVssPathListener()
                classUnderTest.subscribe(vssPath, fieldValue, vssPathListener)

                then("The VssPathListener should send out an update containing ALL children") {
                    eventually(eventuallyConfiguration) {
                        val foundUpdate = vssPathListener.updates.find { entryUpdates ->
                            entryUpdates.size == 3 // all children: IsEnabled, IsEngaged, IsError
                        }

                        foundUpdate shouldNotBe null
                        foundUpdate?.size shouldBe 3
                        foundUpdate?.count { it.entry.path.endsWith(".IsEnabled") } shouldBe 1
                        foundUpdate?.count { it.entry.path.endsWith(".IsEngaged") } shouldBe 1
                        foundUpdate?.count { it.entry.path.endsWith(".IsError") } shouldBe 1
                    }
                }

                `when`("Any child changes it's value") {
                    vssPathListener.reset()

                    val vssPathIsError = "Vehicle.ADAS.ABS.IsError"
                    val newValueIsError = databrokerTransporter.toggleBoolean(vssPathIsError)
                    val vssPathIsEngaged = "Vehicle.ADAS.ABS.IsEngaged"
                    val newValueIsEngaged = databrokerTransporter.toggleBoolean(vssPathIsEngaged)

                    then("The VssPathListener should be notified about it") {
                        eventually(eventuallyConfiguration) {
                            val entryUpdates = vssPathListener.updates.flatten()
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
            }

            `when`("Subscribing using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val vssPathListener = FriendlyVssPathListener()
                classUnderTest.subscribe(vssPath, fieldValue, vssPathListener)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val updateRandomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                    then("The VssPathListener is notified about the change") {
                        eventually(eventuallyConfiguration) {
                            vssPathListener.updates.flatten()
                                .count {
                                    val dataEntry = it.entry
                                    val datapoint = dataEntry.value
                                    dataEntry.path == vssPath && datapoint.float == updateRandomFloatValue
                                } shouldBe 1
                        }
                    }
                }

                `when`("Subscribing the same VssPathListener to a different vssPath") {
                    vssPathListener.reset()

                    val otherVssPath = "Vehicle.ADAS.CruiseControl.SpeedSet"
                    classUnderTest.subscribe(otherVssPath, fieldValue, vssPathListener)

                    and("Both values are updated") {
                        val updatedValueVssPath = databrokerTransporter.updateRandomFloatValue(vssPath)
                        val updatedValueOtherVssPath = databrokerTransporter.updateRandomFloatValue(otherVssPath)

                        then("The Observer is notified about both changes") {
                            eventually(eventuallyConfiguration) {
                                val entryUpdates = vssPathListener.updates.flatten()
                                entryUpdates
                                    .count {
                                        val path = it.entry.path
                                        val value = it.entry.value
                                        path == vssPath && value.float == updatedValueVssPath
                                    } shouldBe 1

                                entryUpdates
                                    .count {
                                        val path = it.entry.path
                                        val value = it.entry.value
                                        path == otherVssPath && value.float == updatedValueOtherVssPath
                                    } shouldBe 1
                            }
                        }
                    }
                }

                `when`("Subscribing multiple (different) VssPathListener to $vssPath") {
                    val friendlyVssPathListeners = mutableListOf<FriendlyVssPathListener>()
                    repeat(10) {
                        val otherVssPathListenerMock = FriendlyVssPathListener()
                        friendlyVssPathListeners.add(otherVssPathListenerMock)

                        classUnderTest.subscribe(vssPath, fieldValue, otherVssPathListenerMock)
                    }

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        val randomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                        then("Each VssPathListener is only notified once") {
                            friendlyVssPathListeners.forEach { listener ->
                                eventually(eventuallyConfiguration) {
                                    listener.updates.flatten()
                                        .count {
                                            val path = it.entry.path
                                            val value = it.entry.value
                                            path == vssPath && value.float == randomFloatValue
                                        } shouldBe 1
                                }
                            }
                        }
                    }
                }

                `when`("Unsubscribing the previously registered VssPathListener") {
                    vssPathListener.reset()
                    classUnderTest.unsubscribe(vssPath, fieldValue, vssPathListener)

                    and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                        databrokerTransporter.updateRandomFloatValue(vssPath)

                        then("The VssPathListener is not notified") {
                            continually(continuallyConfiguration) {
                                vssPathListener.updates.size shouldBe 0
                            }
                        }
                    }
                }
            }

            `when`("Subscribing the same VssPathListener twice using VSS_PATH to Vehicle.Speed with FIELD_VALUE") {
                val vssPath = "Vehicle.Speed"
                val fieldValue = Types.Field.FIELD_VALUE
                val vssPathListener = FriendlyVssPathListener()
                classUnderTest.subscribe(vssPath, fieldValue, vssPathListener)
                classUnderTest.subscribe(vssPath, fieldValue, vssPathListener)

                and("When the FIELD_VALUE of Vehicle.Speed is updated") {
                    val randomFloatValue = databrokerTransporter.updateRandomFloatValue(vssPath)

                    then("The VssPathListener is only notified once") {
                        eventually(eventuallyConfiguration) {
                            val count = vssPathListener.updates.flatten()
                                .count {
                                    val path = it.entry.path
                                    val value = it.entry.value
                                    path == vssPath && value.float == randomFloatValue
                                }
                            count shouldBe 1
                        }
                    }
                }
            }

            val vssHeartRate = VssDriver.VssHeartRate()

            `when`("Subscribing using a VSS node to Vehicle.Driver.HeartRate with Field FIELD_VALUE") {
                val friendlyVssNodeListener = FriendlyVssNodeListener<VssDriver.VssHeartRate>()
                classUnderTest.subscribe(
                    vssHeartRate,
                    Types.Field.FIELD_VALUE,
                    friendlyVssNodeListener,
                )

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue =
                        databrokerTransporter.updateRandomUint32Value(vssHeartRate.vssPath)

                    then("The Observer should be triggered") {
                        eventually(eventuallyConfiguration) {
                            friendlyVssNodeListener.updatedVssNodes.count {
                                val vssPath = it.vssPath
                                val value = it.value
                                vssPath == vssHeartRate.vssPath && value == randomIntValue
                            } shouldBe 1
                        }
                    }
                }
            }

            `when`("Subscribing the same VssNodeObserver twice to Vehicle.Driver.HeartRate") {
                val nodeListenerMock = FriendlyVssNodeListener<VssDriver.VssHeartRate>()
                classUnderTest.subscribe(
                    vssHeartRate,
                    Types.Field.FIELD_VALUE,
                    nodeListenerMock,
                )
                classUnderTest.subscribe(
                    vssHeartRate,
                    Types.Field.FIELD_VALUE,
                    nodeListenerMock,
                )

                and("The value of Vehicle.Driver.HeartRate changes") {
                    val randomIntValue =
                        databrokerTransporter.updateRandomUint32Value(vssHeartRate.vssPath)

                    then("The Observer is only notified once") {
                        eventually(eventuallyConfiguration) {
                            nodeListenerMock.updatedVssNodes.count {
                                val vssPath = it.vssPath
                                val value = it.value
                                vssPath == vssHeartRate.vssPath && value == randomIntValue
                            } shouldBe 1
                        }
                    }
                }
            }
        }
    }

    given("An Instance of DataBrokerSubscriber with a mocked DataBrokerTransporter") {
        val subscriptionMock = mockk<DataBrokerSubscription>(relaxed = true)
        val dataBrokerTransporterMock = mockk<DataBrokerTransporter>(relaxed = true)
        val multiListener = MultiListener<VssPathListener>()
        every { dataBrokerTransporterMock.subscribe(any(), any()) } returns subscriptionMock
        every { subscriptionMock.listeners } returns multiListener
        val classUnderTest = DataBrokerSubscriber(dataBrokerTransporterMock)

        `when`("Subscribing for the first time to a vssPath and field") {
            val vssPath = "Vehicle.Speed"
            val field = Types.Field.FIELD_VALUE
            val vssPathListenerMock1 = mockk<VssPathListener>()
            val vssPathListenerMock2 = mockk<VssPathListener>()
            classUnderTest.subscribe(vssPath, field, vssPathListenerMock1)

            then("A new Subscription is created and the VssPathListener is added to the list of Listeners") {
                verify {
                    dataBrokerTransporterMock.subscribe(vssPath, field)
                }
                multiListener.count() shouldBe 1
            }

            `when`("Another VssPathListener subscribes to the same vssPath and field") {
                clearMocks(dataBrokerTransporterMock)

                classUnderTest.subscribe(vssPath, field, vssPathListenerMock2)

                then("No new Subscription is created and the VssPathListener is added to the list of Listeners") {
                    verify(exactly = 0) {
                        dataBrokerTransporterMock.subscribe(vssPath, field)
                    }
                    multiListener.count() shouldBe 2
                }
            }

            `when`("One of two VssPathListeners unsubscribes") {
                classUnderTest.unsubscribe(vssPath, field, vssPathListenerMock1)

                then("The Subscription is not canceled") {
                    verify(exactly = 0) {
                        subscriptionMock.cancel()
                    }
                    multiListener.count() shouldBe 1
                }

                `when`("The last VssPathListener unsubscribes as well") {
                    classUnderTest.unsubscribe(vssPath, field, vssPathListenerMock2)

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
