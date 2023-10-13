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

package org.eclipse.kuksa.testapp.collection

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MaxElementSetTest : BehaviorSpec({
    given("An Instance of MaxElementSet with Type TestElement and maxNumberEntries of 100") {
        val classUnderTest = MaxElementSet<TestElement>(100)

        `when`("Adding 100 elements") {
            val testElements = mutableListOf<TestElement>()
            repeat(100) {
                val testElement = TestElement()

                testElements.add(testElement)
                classUnderTest.add(testElement)
            }

            then("All 100 elements are added successfully") {
                classUnderTest.size shouldBe 100
            }

            then("The order is kept intact") {
                classUnderTest.withIndex().forEach {
                    val index = it.index
                    val value = it.value

                    value shouldBe testElements[index]
                }
            }
        }
    }

    given("An Instance of MaxElementSet with Type TestElement and maxNumberEntries of 10") {
        val classUnderTest = MaxElementSet<TestElement>(10)

        `when`("Trying to add the same element twice") {
            val testElement = TestElement()
            classUnderTest.add(testElement)
            classUnderTest.add(testElement)

            then("It is only added once") {
                classUnderTest.size shouldBe 1
            }
        }

        `when`("Adding more than 10 elements (100)") {
            val testElements = mutableListOf<TestElement>()
            repeat(100) {
                val testElement = TestElement()

                testElements.add(testElement)
                classUnderTest.add(testElement)
            }

            then("Only the last 10 Elements are kept") {
                classUnderTest.size shouldBe 10

                val subList = testElements.subList(90, 100)
                val containsLastTenElements = classUnderTest.containsAll(subList)
                containsLastTenElements shouldBe true
            }
        }
    }
})

class TestElement
