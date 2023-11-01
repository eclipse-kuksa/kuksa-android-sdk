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

package org.eclipse.kuksa.vssSpecification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.extension.deepCopy
import org.eclipse.kuksa.extension.invoke
import org.eclipse.kuksa.extension.vssProperty.div
import org.eclipse.kuksa.extension.vssProperty.minus
import org.eclipse.kuksa.extension.vssProperty.not
import org.eclipse.kuksa.extension.vssProperty.plus
import org.eclipse.kuksa.extension.vssProperty.times
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.test.kotest.Unit
import org.eclipse.kuksa.vsscore.model.VssProperty

class VssSpecificationCopyTest : BehaviorSpec({
    tags(Unit)

    given("A specification") {
        val vehicle = VssVehicle()
        val driverHeartRate: VssProperty<Int> = vehicle.driver.heartRate

        and("a changed heritage line") {
            val newValue = 70
            val updatedHeartRate = VssDriver.VssHeartRate(value = newValue)

            `when`("a deep copy is done with a changed heritage line") {
                val deepCopiedSpecification = vehicle.deepCopy(0, updatedHeartRate)

                then("it should return the new children as a copy") {
                    val heartRate = deepCopiedSpecification.driver.heartRate

                    heartRate shouldBeSameInstanceAs updatedHeartRate
                }
            }
        }

        `when`("a value copy is done via the not operator") {
            val invertedSpecification = !vehicle.driver.isEyesOnRoad

            then("it should return a copy with the inverted value") {
                invertedSpecification.value shouldBe false
            }
        }

        and("values for arithmetic operations") {
            val valueInt = VssValueInt(value = 100)
            val valueFloat = VssValueFloat(value = 100f)
            val valueLong = VssValueLong(value = 100L)
            val valueDouble = VssValueDouble(value = 100.0)

            val values = listOf<Number>(5, 5L, 5f, 5.0)

            `when`("a plus operation is done") {
                val newValues = values.map {
                    listOf(
                        valueInt + it,
                        valueFloat + it,
                        valueLong + it,
                        valueDouble + it,
                    )
                }

                then("it should correctly add the values") {
                    newValues.forEach { properties ->
                        properties.forEach {
                            it.value shouldBe 105
                        }
                    }
                }
            }

            `when`("a minus operation is done") {
                val newValues = values.map {
                    listOf(
                        valueInt - it,
                        valueFloat - it,
                        valueLong - it,
                        valueDouble - it,
                    )
                }

                then("it should correctly subtract the values") {
                    newValues.forEach { properties ->
                        properties.forEach {
                            it.value shouldBe 95
                        }
                    }
                }
            }

            `when`("a divide operation with zero is done") {
                val exception = shouldThrow<ArithmeticException> {
                    valueInt / 0
                }

                then("it should throw an exception") {
                    exception.message shouldStartWith "/ by zero"
                }
            }

            `when`("a divide operation is done") {
                val newValues = values.map {
                    listOf(
                        valueInt / it,
                        valueFloat / it,
                        valueLong / it,
                        valueDouble / it,
                    )
                }

                then("it should correctly divide the values") {
                    newValues.forEach { properties ->
                        properties.forEach {
                            it.value shouldBe 20
                        }
                    }
                }
            }

            `when`("a multiply operation is done") {
                val newValues = values.map {
                    listOf(
                        valueInt * it,
                        valueFloat * it,
                        valueLong * it,
                        valueDouble * it,
                    )
                }

                then("it should correctly multiply the values") {
                    newValues.forEach { properties ->
                        properties.forEach {
                            it.value shouldBe 500
                        }
                    }
                }
            }
        }

        and("a changed value") {
            val newValue = 40

            `when`("a deep copy is done via the invoke operator") {
                val copiedHeartRate = driverHeartRate(newValue)
                val copiedSpecification = vehicle(copiedHeartRate)

                then("it should return a copy with the updated value") {
                    val heartRate = copiedSpecification.driver.heartRate

                    heartRate shouldBeSameInstanceAs copiedHeartRate
                }
            }
        }

        and("a changed DataPoint") {
            val newValue = 50
            val datapoint = Types.Datapoint.newBuilder().setInt32(newValue).build()

            `when`("a copy is done") {
                val copiedSpecification = driverHeartRate.copy(datapoint)

                then("it should return a copy with the updated value") {
                    val heartRateValue = copiedSpecification.value

                    heartRateValue shouldBe newValue
                }
            }

            and("a vssPath") {
                val vssPath = driverHeartRate.vssPath

                `when`("a copy is done") {
                    val copiedSpecification = driverHeartRate.copy(vssPath, datapoint)

                    then("it should return a copy with the updated value") {
                        val heartRateValue = copiedSpecification.value

                        heartRateValue shouldBe newValue
                    }
                }
            }
        }

        and("an empty DataPoint") {
            val datapoint = Types.Datapoint.newBuilder().build()

            and("an invalid VssSpecification") {
                val invalidSpecification = VssInvalid()

                `when`("a copy is done") {
                    val exception = shouldThrow<NoSuchFieldException> {
                        invalidSpecification.copy(datapoint)
                    }

                    then("it should throw an IllegalArgumentException") {
                        exception.message shouldStartWith "Could not convert value"
                    }
                }
            }
        }
    }
})
