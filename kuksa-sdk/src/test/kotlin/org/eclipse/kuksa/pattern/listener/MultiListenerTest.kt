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

package org.eclipse.kuksa.pattern.listener

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.eclipse.kuksa.test.kotest.Unit

class MultiListenerTest : BehaviorSpec({
    tags(Unit)

    given("An Instance of a MultiListener with generic type TestListener") {
        val classUnderTest = MultiListener<TestListener>()

        `when`("Checking isEmpty on a newly instantiated MultiListener") {
            val isEmpty = classUnderTest.isEmpty()

            then("It should return true") {
                isEmpty shouldBe true
            }
        }

        `when`("Trying to register a TestListener") {
            val testListener = TestListener()
            classUnderTest.register(testListener)

            then("The registration is successful") {
                classUnderTest.count() shouldBe 1
            }

            then("isEmpty should return false") {
                classUnderTest.isEmpty() shouldBe false
            }

            `when`("Trying to register the same listener again") {
                classUnderTest.register(testListener)

                then("The same listener should not be added a second time") {
                    classUnderTest.count() shouldBe 1
                }
            }

            `when`("Trying to unregister the already registered listener") {
                classUnderTest.unregister(testListener)

                then("It should be correctly removed") {
                    classUnderTest.count() shouldBe 0
                }
            }
        }

        and("Multiple TestListeners are already registered") {
            val registeredListeners = ArrayList<TestListener>() // order is important

            repeat(100) {
                val testListener = TestListener()
                registeredListeners.add(testListener)

                classUnderTest.register(testListener)
            }

            `when`("Iterating over the Collection using the iterator") {
                val indexedValueIterator = classUnderTest.iterator().withIndex()

                then("The order should be left in tact") {
                    while (indexedValueIterator.hasNext()) {
                        val indexedValue = indexedValueIterator.next()

                        val index = indexedValue.index
                        val testListener = indexedValue.value
                        testListener shouldBe registeredListeners[index]
                    }
                }
            }
        }
    }
})

fun <T : Listener> MultiListener<T>.count(): Int {
    var count = 0

    val iterator = iterator()
    while (iterator.hasNext()) {
        count++
        iterator.next()
    }
    return count
}

class TestListener : Listener
