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

package org.eclipse.kuksa.vssprocessor.parser.json

import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import org.eclipse.kuksa.test.TestResourceFile
import java.io.IOException

class JsonDefinitionParserTest : BehaviorSpec({

    given("A JsonDefinitionParser") {
        val classUnderTest = JsonDefinitionParser()

        `when`("Parsing the SpecModels of vss_rel_4.0.partial.json") {
            val partialSpecFile = TestResourceFile("json/vss_rel_4.0.partial.json")
            val specModels = classUnderTest.parseSpecifications(partialSpecFile)

            then("The following SpecModels should be parsed") {
                val validVssPaths = listOf(
                    "Vehicle",
                    "Vehicle.ADAS",
                    "Vehicle.ADAS.ABS",
                    "Vehicle.ADAS.ABS.IsEnabled",
                    "Vehicle.ADAS.ABS.IsEngaged",
                    "Vehicle.ADAS.ABS.IsError",
                    "Vehicle.ADAS.ActiveAutonomyLevel",
                    "Vehicle.ADAS.CruiseControl",
                    "Vehicle.ADAS.CruiseControl.IsActive",
                    "Vehicle.ADAS.CruiseControl.IsEnabled",
                    "Vehicle.ADAS.CruiseControl.IsError",
                    "Vehicle.ADAS.CruiseControl.SpeedSet",
                    "Vehicle.ADAS.DMS",
                    "Vehicle.ADAS.DMS.IsEnabled",
                    "Vehicle.ADAS.DMS.IsError",
                    "Vehicle.ADAS.DMS.IsWarning",
                    "Vehicle.AverageSpeed",
                    "Vehicle.Speed",
                )

                validVssPaths.forEach { vssPath ->
                    specModels.find { specModel -> specModel.vssPath == vssPath }
                        ?: fail("Could not find '$vssPath'")
                }
            }

            then("A Branch (Vehicle.ADAS.ABS) is correctly parsed") {
                val absSpecModel = specModels.find { it.vssPath == "Vehicle.ADAS.ABS" }
                    ?: fail("Could not find Vehicle.ADAS.ABS")

                absSpecModel.description shouldBe "Antilock Braking System signals."
                absSpecModel.type shouldBe "branch"
                absSpecModel.uuid shouldBe "219270ef27c4531f874bbda63743b330"
                absSpecModel.comment shouldBe ""
                absSpecModel.datatype shouldBe ""
            }

            then("A Leaf (Vehicle.ADAS.ABS.IsEnabled) is correctly parsed") {
                val absSpecModel = specModels.find { it.vssPath == "Vehicle.ADAS.ABS.IsEnabled" }
                    ?: fail("Could not find Vehicle.ADAS.ABS.IsEnabled")

                absSpecModel.description shouldBe "Indicates if ABS is enabled. True = Enabled. False = Disabled."
                absSpecModel.type shouldBe "actuator"
                absSpecModel.uuid shouldBe "cad374fbfdc65df9b777508f04d5b073"
                absSpecModel.comment shouldBe ""
                absSpecModel.datatype shouldBe "boolean"
            }
        }

        `when`("Parsing vss_rel_4.0.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_4.0.json")
            val specModels = classUnderTest.parseSpecifications(fullSpecFile)

            then("the correct number of specification models should be parsed") {
                specModels.size shouldBe 1197 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_3.1.1.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_3.1.1.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(fullSpecFile)
            }

            then("No Exception should be thrown") {
                result.exceptionOrNull() shouldBe null
            }
            then("The correct number of results should be parsed") {
                result.getOrNull()?.size shouldBe 1138 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_3.0.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_3.0.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(fullSpecFile)
            }

            then("No Exception should be thrown") {
                result.exceptionOrNull() shouldBe null
            }
            then("The correct number of results should be parsed") {
                result.getOrNull()?.size shouldBe 1079 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_2.2.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_2.2.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(fullSpecFile)
            }

            then("No Exception should be thrown") {
                result.exceptionOrNull() shouldBe null
            }
            then("The correct number of results should be parsed") {
                result.getOrNull()?.size shouldBe 968 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_2.1.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_2.1.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(fullSpecFile)
            }

            then("No Exception should be thrown") {
                result.exceptionOrNull() shouldBe null
            }
            then("The correct number of results should be parsed") {
                result.getOrNull()?.size shouldBe 967 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_2.0.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_2.0.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(fullSpecFile)
            }

            then("No Exception should be thrown") {
                result.exceptionOrNull() shouldBe null
            }
            then("The correct number of results should be parsed") {
                result.getOrNull()?.size shouldBe 1712 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing an incompatible / non-vss json file") {
            val incompatibleFile = TestResourceFile("json/incompatible.json")
            val result = runCatching {
                classUnderTest.parseSpecifications(incompatibleFile)
            }

            then("An IOException is thrown") {
                result.exceptionOrNull() shouldBe instanceOf(IOException::class)
            }
        }

        `when`("Parsing a non-json file") {
            val nonJsonFile = TestResourceFile("yaml/vss_rel_4.0.yaml")
            val result = runCatching {
                classUnderTest.parseSpecifications(nonJsonFile)
            }

            then("An IOException is thrown") {
                result.exceptionOrNull() shouldBe instanceOf(IOException::class)
            }
        }
    }
})
