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
import org.eclipse.kuksa.vssprocessor.parser.json.JsonDefinitionParser
import org.eclipse.kuksa.vssprocessor.parser.yaml.YamlDefinitionParser
import java.io.File

class VssDefinitionParserFactoryTest : BehaviorSpec({

    given("An instance of DefinitionParserFactory") {
        val classUnderTest = VssDefinitionParserFactory()

        `when`("Calling create with supported extension 'json'") {
            val vssDefinitionParser = classUnderTest.create("json")

            then("It should return a JsonDefinitionParser") {
                vssDefinitionParser shouldBe instanceOf(JsonDefinitionParser::class)
            }
        }

        `when`("Calling create with supported extension 'yaml'") {
            val vssDefinitionParser = classUnderTest.create("yaml")

            then("It should return a YamlDefinitionParser") {
                vssDefinitionParser shouldBe instanceOf(YamlDefinitionParser::class)
            }
        }

        `when`("Calling create with supported extension 'yml'") {
            val vssDefinitionParser = classUnderTest.create("yml")

            then("It should return a YamlDefinitionParser") {
                vssDefinitionParser shouldBe instanceOf(YamlDefinitionParser::class)
            }
        }

        `when`("Calling create with a File with a supported file extension") {
            val supportedFile = File("someVssFile.with-multiple-dots.json")
            val vssDefinitionParser = classUnderTest.create(supportedFile)

            then("It should correctly extract the extension and return the corresponding VssDefinitionParser") {
                vssDefinitionParser shouldBe instanceOf(JsonDefinitionParser::class)
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
