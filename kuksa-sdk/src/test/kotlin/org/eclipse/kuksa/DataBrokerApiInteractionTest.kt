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

import io.grpc.ManagedChannelBuilder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.instanceOf
import io.mockk.mockk
import io.mockk.verify
import org.eclipse.kuksa.databroker.DataBrokerConnectorProvider
import org.eclipse.kuksa.extensions.updateRandomFloatValue
import org.eclipse.kuksa.proto.v1.KuksaValV1
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.test.kotest.Insecure
import org.eclipse.kuksa.test.kotest.Integration
import kotlin.random.Random

class DataBrokerApiInteractionTest : BehaviorSpec({
    tags(Integration, Insecure)

    given("An active Connection to the DataBroker") {
        val dataBrokerConnectorProvider = DataBrokerConnectorProvider()
        val connector = dataBrokerConnectorProvider.createInsecure()
        connector.connect()

        and("An Instance of DataBrokerApiInteraction") {
            val classUnderTest = DataBrokerApiInteraction(dataBrokerConnectorProvider.managedChannel)

            and("Some VSS-related data") {
                val vssPath = "Vehicle.ADAS.CruiseControl.SpeedSet"
                val fields = listOf(Types.Field.FIELD_VALUE)
                val random = Random(System.currentTimeMillis())
                val valueToSet = random.nextInt(250).toFloat()

                `when`("Updating the $fields of $vssPath to $valueToSet km/h") {
                    val updatedDatapoint = Datapoint.newBuilder().setFloat(valueToSet).build()
                    val result = kotlin.runCatching {
                        classUnderTest.update(vssPath, fields, updatedDatapoint)
                    }

                    then("No Exception should be thrown") {
                        result.exceptionOrNull() shouldBe null
                    }

                    then("It should return a valid SetResponse") {
                        val response = result.getOrNull()
                        response shouldNotBe null
                        response shouldBe instanceOf(SetResponse::class)
                    }
                }

                `when`("Fetching the Value of Vehicle.ADAS.CruiseControl.SpeedSet") {
                    val property = classUnderTest.fetch(vssPath, fields)

                    then("It should return the correct value") {
                        val dataEntry = property.getEntries(0)
                        val value = dataEntry.value.float
                        value shouldBe valueToSet
                    }
                }

                `when`("Trying to fetch the $fields from an invalid VSS Path") {
                    val invalidVssPath = "Vehicle.This.Path.Is.Invalid"

                    val result = kotlin.runCatching {
                        classUnderTest.fetch(invalidVssPath, fields)
                    }

                    then("No Exception should be thrown") {
                        result.exceptionOrNull() shouldBe null
                    }

                    then("It should return a GetResponse with no entries and one error") {
                        val response = result.getOrNull()
                        response shouldNotBe null
                        response shouldBe instanceOf(KuksaValV1.GetResponse::class)
                        response?.entriesList?.size shouldBe 0
                        response?.errorsList?.size shouldBe 1
                    }
                }

                `when`("Subscribing to the vssPath using FIELD_VALUE") {
                    val subscription = classUnderTest.subscribe(vssPath, Types.Field.FIELD_VALUE)

                    val propertyListener = mockk<PropertyListener>(relaxed = true)
                    subscription.listeners.register(propertyListener)

                    and("The value of the vssPath is updated") {
                        classUnderTest.updateRandomFloatValue(vssPath)

                        then("The PropertyListener should be notified") {
                            verify {
                                propertyListener.onPropertyChanged(vssPath, Types.Field.FIELD_VALUE, any())
                            }
                        }
                    }
                }

                `when`("Subscribing to an invalid vssPath") {
                    val subscription = classUnderTest.subscribe("Vehicle.Some.Invalid.Path", Types.Field.FIELD_VALUE)

                    val propertyListener = mockk<PropertyListener>(relaxed = true)
                    subscription.listeners.register(propertyListener)

                    then("An Error should be triggered") {
                        verify(timeout = 100L) {
                            propertyListener.onError(any())
                        }
                    }
                }
            }
        }
    }

    given("An inactive Connection to the DataBroker") {
        val inactiveManagedChannel = ManagedChannelBuilder.forAddress("someHost", 12345).build()

        `when`("Trying to instantiate the DataBrokerApiInteraction ") {
            val result = kotlin.runCatching {
                DataBrokerApiInteraction(inactiveManagedChannel)
            }

            then("An IllegalStateException is thrown") {
                val exceptionOrNull = result.exceptionOrNull()
                exceptionOrNull shouldNotBe null
                exceptionOrNull shouldBe instanceOf(IllegalStateException::class)
            }
        }
    }
})
