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

package org.eclipse.kuksa.vssNode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.eclipse.kuksa.extension.vss.div
import org.eclipse.kuksa.extension.vss.minus
import org.eclipse.kuksa.extension.vss.plus
import org.eclipse.kuksa.extension.vss.times
import org.eclipse.kuksa.test.kotest.Unit

class VssNodeArithmeticTest : BehaviorSpec({
    tags(Unit)

    given("VssNode values for arithmetic operations") {
        val valueInt = VssValueInt(value = 100)
        val valueFloat = VssValueFloat(value = 100f)
        val valueLong = VssValueLong(value = 100L)
        val valueDouble = VssValueDouble(value = 100.0)

        val value = 5

        `when`("a plus operation is done") {
            val newValues = listOf(
                valueInt + value,
                valueFloat + value,
                valueLong + value,
                valueDouble + value,
            )

            then("it should correctly add the values") {
                newValues.forEach {
                    it.value shouldBe 105
                }
            }
        }

        `when`("a minus operation is done") {
            val newValues = listOf(
                valueInt - value,
                valueFloat - value,
                valueLong - value,
                valueDouble - value,
            )

            then("it should correctly subtract the values") {
                newValues.forEach {
                    it.value shouldBe 95
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
            val newValues = listOf(
                valueInt / value,
                valueFloat / value,
                valueLong / value,
                valueDouble / value,
            )

            then("it should correctly divide the values") {
                newValues.forEach {
                    it.value shouldBe 20
                }
            }
        }

        `when`("a multiply operation is done") {
            val newValues = listOf(
                valueInt * value,
                valueFloat * value,
                valueLong * value,
                valueDouble * value,
            )

            then("it should correctly multiply the values") {
                newValues.forEach {
                    it.value shouldBe 500
                }
            }
        }
    }
})
