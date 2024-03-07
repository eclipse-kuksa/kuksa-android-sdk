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

package org.eclipse.kuksa.vssprocessor.parser.yaml

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.eclipse.kuksa.test.TestResourceFile

class YamlVssParserTest : BehaviorSpec({
    given("A parser for yaml files") {
        val parser = YamlVssParser()

        and("a specification file of version 4") {
            val fullSpecificationFile = TestResourceFile("yaml/vss_rel_4.0.yaml")

            `when`("parsing the file") {
                val parsedSpecifications = parser.parseNodes(fullSpecificationFile)

                then("the correct number of specification models should be parsed") {
                    parsedSpecifications.size shouldBe 1197 // counted occurrences of '"uuid":' in specFile
                }
            }
        }
        and("an incompatible yaml file") {
            val incompatibleFile = TestResourceFile("yaml/incompatible.yaml")

            `when`("parsing the file") {
                val parsedSpecifications = parser.parseNodes(incompatibleFile)

                then("no entries should be returned") {
                    parsedSpecifications.size shouldBe 0
                }
            }
        }
        and("an invalid yaml file") {
            val invalidFile = TestResourceFile("yaml/invalid.yaml")

            `when`("parsing the file") {
                val parsedSpecifications = parser.parseNodes(invalidFile)

                then("no entries should be returned") {
                    parsedSpecifications.size shouldBe 0
                }
            }
        }
    }
})
