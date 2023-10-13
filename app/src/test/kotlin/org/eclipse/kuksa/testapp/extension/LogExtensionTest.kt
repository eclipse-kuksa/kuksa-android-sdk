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

package org.eclipse.kuksa.testapp.extension

import io.kotest.core.spec.style.BehaviorSpec
import org.eclipse.kuksa.test.kotest.Unit
import org.junit.jupiter.api.Assertions

class LogExtensionTest : BehaviorSpec({
    tags(Unit)

    val expectedLogTag = AnyClass::class.simpleName

    given("Any class") {
        val anyClass = AnyClass()

        `when`("a basic method generates a logTag") {
            val logTag = anyClass.createLogTag()

            then("the logTag should contain the class name") {
                Assertions.assertEquals(expectedLogTag, logTag)
            }
        }

        `when`("a companion method generates a logTag") {
            val logTag = AnyClass.createLogTag()

            then("the logTag should contain the class name") {
                Assertions.assertEquals(expectedLogTag, logTag)
            }
        }

        `when`("a lambda interface call generates a logTag") {
            var logTag = ""
            anyClass.createLogTag {
                logTag = anyClass.createLogTag()
            }

            then("the logTag should contain the class name") {
                Assertions.assertEquals(expectedLogTag, logTag)
            }
        }

        `when`("an anonymous object call generates a logTag") {
            var logTag = ""
            anyClass.createAnonymousLogTag {
                logTag = anyClass.createLogTag()
            }

            then("the logTag should contain the class name") {
                Assertions.assertEquals(expectedLogTag, logTag)
            }
        }
    }
})

class AnyClass {
    // Also used of wrapping the TAG call so the above test behaviour specs don't return something like "WhenSpec" as
    // TAG name. This reflects more the actual environment where an actual class is always used.
    fun createLogTag(): String {
        return TAG
    }

    fun createLogTag(anyFunInterface: AnyFunInterface) {
        anyFunInterface.onAnyMethodCall()
    }

    fun createAnonymousLogTag(onAnyMethodCall: () -> Unit) {
        onAnyMethodCall()
    }

    companion object {
        fun createLogTag(): String {
            return TAG
        }
    }
}

fun interface AnyFunInterface {
    fun onAnyMethodCall()
}
