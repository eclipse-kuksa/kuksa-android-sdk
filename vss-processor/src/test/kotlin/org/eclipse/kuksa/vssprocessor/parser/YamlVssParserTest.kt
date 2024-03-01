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

package org.eclipse.kuksa.vssprocessor.parser

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.File

class YamlVssParserTest : BehaviorSpec({
    given("A parser for yaml files") {
        val parser = YamlVssParser()
        val classLoader = parser::class.java.classLoader!!

        and("a VSS file of version 4") {
            val resourceUrl = classLoader.getResource(VALID_VSS_FILE_NAME)!!
            val vssFile = File(resourceUrl.path)

            `when`("parsing the file") {
                val parsedNodes = parser.parseNodes(vssFile)

                then("the correct number of VSS models should be parsed") {
                    // These are exactly the VSS models defined in the 4.0 file
                    parsedNodes.size shouldBe 1197
                }
            }
        }
        and("an incompatible yaml file") {
            val incompatibleResourceUrl = classLoader.getResource(INCOMPATIBLE_VSS_FILE_NAME)!!
            val incompatibleFile = File(incompatibleResourceUrl.path)

            `when`("parsing the file") {
                val parsedNodes = parser.parseNodes(incompatibleFile)

                then("no entries should be returned") {
                    parsedNodes.size shouldBe 0
                }
            }
        }
        and("an invalid yaml file") {
            val invalidResourceUrl = classLoader.getResource(INVALID_VSS_FILE_NAME)!!
            val invalidFile = File(invalidResourceUrl.path)

            `when`("parsing the file") {
                val parsedNodes = parser.parseNodes(invalidFile)

                then("no entries should be returned") {
                    parsedNodes.size shouldBe 0
                }
            }
        }
    }
}) {
    companion object {
        private const val VALID_VSS_FILE_NAME = "vss_rel_4.0.yaml"
        private const val INVALID_VSS_FILE_NAME = "invalid.yaml"
        private const val INCOMPATIBLE_VSS_FILE_NAME = "incompatible.yaml"
    }
}
