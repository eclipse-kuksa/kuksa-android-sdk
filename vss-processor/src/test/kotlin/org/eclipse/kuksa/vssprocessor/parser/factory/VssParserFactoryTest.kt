/*
 * Copyright (c) 2023 - 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.vssprocessor.parser.factory

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.eclipse.kuksa.vssprocessor.parser.json.JsonVssParser
import org.eclipse.kuksa.vssprocessor.parser.yaml.YamlVssParser
import java.io.File

class VssParserFactoryTest : BehaviorSpec({

    given("An instance of VssParserFactory") {
        val classUnderTest = VssParserFactory()

        `when`("Calling create with supported extension 'json'") {
            val vssParser = classUnderTest.create("json")

            then("It should return a parser for JSON VSS files") {
                vssParser shouldBe instanceOf(JsonVssParser::class)
            }
        }

        `when`("Calling create with supported extension 'yaml'") {
            val vssParser = classUnderTest.create("yaml")

            then("It should return a parser for YAML VSS files") {
                vssParser shouldBe instanceOf(YamlVssParser::class)
            }
        }

        `when`("Calling create with supported extension 'yml'") {
            val vssParser = classUnderTest.create("yml")

            then("It should return a parser for YAML VSS files") {
                vssParser shouldBe instanceOf(YamlVssParser::class)
            }
        }

        `when`("Calling create with a File with a supported file extension") {
            val supportedFile = File("someVssFile.with-multiple-dots.json")
            val vssParser = classUnderTest.create(supportedFile)

            then("It should correctly extract the extension and return the corresponding VssParser") {
                vssParser shouldBe instanceOf(JsonVssParser::class)
            }
        }

        `when`("Calling create with unknown extension 'txt'") {
            val result = runCatching {
                classUnderTest.create("txt")
            }

            then("It should throw an IllegalStateException") {
                result.exceptionOrNull() shouldBe instanceOf(IllegalStateException::class)
            }
        }
    }
})
