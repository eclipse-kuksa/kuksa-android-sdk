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

package org.eclipse.kuksa.testapp.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class ConnectionInfoTest : BehaviorSpec({
    given("A custom ConnectionInfo with a custom Certificate") {
        // Uri is an android specific class, which does not exist in plain JUnit. Methods like Uri.parse will simply
        // return null due to the addition of testOptions.unitTests.isReturnDefaultValues true
        val certificate = Certificate(
            uriPath = "content://com.android.providers.downloads.documents/document/msf%3A1000000116",
            overrideAuthority = "server",
        )
        val classUnderTest = ConnectionInfo(
            host = "someHost",
            port = 12345,
            certificate = certificate,
            isTlsEnabled = true,
        )
        and("it has been serialized to file") {
            val tempFile = withContext(Dispatchers.IO) {
                File.createTempFile("connection_info", "tmp")
            }

            tempFile.outputStream().use {
                ConnectionInfoSerializer.writeTo(classUnderTest, it)
            }

            `when`("Trying to de-serialize it") {
                tempFile.inputStream().use {
                    val deserializedConnectionInfo = ConnectionInfoSerializer.readFrom(it)

                    then("It should be deserialized correctly") {
                        deserializedConnectionInfo.host shouldBe classUnderTest.host
                        deserializedConnectionInfo.port shouldBe classUnderTest.port
                        deserializedConnectionInfo.isTlsEnabled shouldBe classUnderTest.isTlsEnabled
                        deserializedConnectionInfo.certificate shouldBe classUnderTest.certificate
                    }
                }
            }
        }
    }

    given("An invalid serialized ConnectionInfo") {
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("connection_info", "tmp")
        }

        withContext(Dispatchers.IO) {
            FileWriter(tempFile).use { writer ->
                writer.write("{ invalid }")
            }
        }

        `when`("Trying to de-serialize it") {
            var isExceptionThrown = false
            var deserializedConnectionInfo: ConnectionInfo? = null
            tempFile.inputStream().use {
                try {
                    deserializedConnectionInfo = ConnectionInfoSerializer.readFrom(it)
                } catch (_: Exception) {
                    isExceptionThrown = true
                }
            }

            then("No Exception is thrown") {
                isExceptionThrown shouldBe false
            }

            then("We are falling back to the defaultValue") {
                deserializedConnectionInfo shouldBe ConnectionInfoSerializer.defaultValue
            }
        }
    }
})
