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

class YamlDefinitionParserTest : BehaviorSpec({
    given("A parser for yaml files") {
        val parser = YamlDefinitionParser()
        val classLoader = parser::class.java.classLoader!!

        and("a specification file of version 4") {
            val resourceUrl = classLoader.getResource("vss_rel_4.0.yaml")!!
            val specificationFile = File(resourceUrl.path)

            `when`("parsing the file") {
                val parsedSpecifications = parser.parseSpecifications(specificationFile)

                then("the correct number of specification models should be parsed") {
                    parsedSpecifications.size shouldBe 1197
                }
            }
        }
        and("an incompatible yaml file") {
            val invalidResourceUrl = classLoader.getResource("invalid.yaml")!!
            val invalidFile = File(invalidResourceUrl.path)

            `when`("parsing the file") {
                val parsedSpecifications = parser.parseSpecifications(invalidFile)

                then("no entries should be returned") {
                    parsedSpecifications.size shouldBe 0
                }
            }
        }
    }
})
