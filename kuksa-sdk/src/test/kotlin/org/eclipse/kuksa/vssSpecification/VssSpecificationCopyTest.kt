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

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.eclipse.kuksa.extension.copy
import org.eclipse.kuksa.extension.deepCopy
import org.eclipse.kuksa.kotest.Unit
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.BoolArray

class VssSpecificationCopyTest : BehaviorSpec({
    tags(Unit)

    given("A specification") {
        val driver = VssDriver()
        val heartRate = driver.heartRate

        and("a changed heritage line") {
            val newValue = 70
            val updatedHeartRate = VssHeartRate(value = newValue)
            val changedHeritageLine = listOf(updatedHeartRate)

            `when`("a deep copy is done with a changed heritage line") {
                val deepCopiedSpecification = driver.deepCopy(changedHeritageLine)

                then("it should return the new children as a copy") {
                    val heartRateValue = deepCopiedSpecification.heartRate.value

                    heartRateValue shouldBe newValue
                }
            }
        }

        and("a changed DataPoint") {
            val newValue = 50
            val datapoint = Types.Datapoint.newBuilder().setInt32(newValue).build()

            `when`("a copy is done") {
                val copiedSpecification = heartRate.copy(datapoint)

                then("it should return a copy with the updated value") {
                    val heartRateValue = copiedSpecification.value

                    heartRateValue shouldBe newValue
                }
            }

            and("a vssPath") {
                val vssPath = heartRate.vssPath

                `when`("a copy is done") {
                    val copiedSpecification = heartRate.copy(vssPath, datapoint)

                    then("it should return a copy with the updated value") {
                        val heartRateValue = copiedSpecification.value

                        heartRateValue shouldBe newValue
                    }
                }
            }
        }

        and("an incompatible DataPoint") {
            val boolArray = BoolArray.newBuilder().addValues(true).build()
            val datapoint = Types.Datapoint.newBuilder().setBoolArray(boolArray).build()

            `when`("a copy is done") {
                val copiedSpecification = heartRate.copy(datapoint)

                then("it should default to valid value") {
                    val heartRateValue = copiedSpecification.value

                    heartRateValue shouldBe 0
                }
            }
        }
    }
})
