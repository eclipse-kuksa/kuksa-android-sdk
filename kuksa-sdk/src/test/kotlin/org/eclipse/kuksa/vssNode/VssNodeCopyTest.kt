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

package org.eclipse.kuksa.vssNode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.eclipse.kuksa.extension.vss.copy
import org.eclipse.kuksa.extension.vss.deepCopy
import org.eclipse.kuksa.extension.vss.invoke
import org.eclipse.kuksa.extension.vss.not
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.Unit
import org.eclipse.kuksa.vsscore.model.VssSignal

class VssNodeCopyTest : BehaviorSpec({
    tags(Unit)

    given("A VssNode") {
        val vehicle = VssVehicle()
        val driverHeartRate: VssSignal<Int> = vehicle.driver.heartRate

        and("a changed heritage line") {
            val newValue = 70
            val updatedHeartRate = VssDriver.VssHeartRate(value = newValue)

            `when`("a deep copy is done with a changed heritage line") {
                val deepCopiedNode = vehicle.deepCopy(updatedHeartRate)

                then("it should return the new children as a copy") {
                    val heartRate = deepCopiedNode.driver.heartRate

                    heartRate shouldBeSameInstanceAs updatedHeartRate
                }
            }
        }

        `when`("a value copy is done via the not operator") {
            val invertedNode = !vehicle.driver.isEyesOnRoad

            then("it should return a copy with the inverted value") {
                invertedNode.value shouldBe true
            }
        }

        and("a changed value") {
            val newValue = 40

            `when`("a deep copy is done via the invoke operator") {
                val copiedHeartRate = driverHeartRate(newValue)
                val copiedNode = vehicle(copiedHeartRate)

                then("it should return a copy with the updated value") {
                    val heartRate = copiedNode.driver.heartRate

                    heartRate shouldBeSameInstanceAs copiedHeartRate
                }
            }
        }

        and("a changed invalid DataPoint") {
            val datapoint = Types.Datapoint.newBuilder().setBool(false).build()

            `when`("a copy is done") {
                val exception = shouldThrow<IllegalArgumentException> {
                    driverHeartRate.copy(datapoint)
                }

                then("it should throw an IllegalArgumentException") {
                    val signalName = driverHeartRate::class.simpleName
                    exception.message shouldStartWith "$signalName copy parameters do not match"
                }
            }
        }

        and("a changed DataPoint") {
            val newValue = 50
            val datapoint = Types.Datapoint.newBuilder().setInt32(newValue).build()

            `when`("a copy is done") {
                val copiedNode = driverHeartRate.copy(datapoint)

                then("it should return a copy with the updated value") {
                    val heartRateValue = copiedNode.value

                    heartRateValue shouldBe newValue
                }
            }

            and("a vssPath") {
                val vssPath = driverHeartRate.vssPath

                `when`("a copy is done") {
                    val copiedNode = driverHeartRate.copy(vssPath, datapoint)

                    then("it should return a copy with the updated value") {
                        val heartRateValue = copiedNode.value

                        heartRateValue shouldBe newValue
                    }
                }
            }
        }

        and("an empty DataPoint") {
            val datapoint = Types.Datapoint.newBuilder().build()

            and("an invalid VssSignal") {
                val vssInvalid = VssInvalid()

                `when`("a copy is done") {
                    val exception = shouldThrow<NoSuchFieldException> {
                        vssInvalid.copy(datapoint)
                    }

                    then("it should throw an IllegalArgumentException") {
                        exception.message shouldStartWith "Could not convert value"
                    }
                }
            }
        }
    }
})
