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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.instanceOf
import org.eclipse.kuksa.test.TestResourceFile
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MAX
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MIN
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_UNIT
import java.io.IOException

class JsonVssParserTest : BehaviorSpec({

    given("A JsonDefinitionParser") {
        val classUnderTest = JsonVssParser()

        `when`("Parsing the SpecModels of vss_rel_4.0.partial.json") {
            val partialVssFile = TestResourceFile("json/vss_rel_4.0.partial.json")
            val specModels = classUnderTest.parseNodes(partialVssFile)

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
                    "Vehicle.ADAS.ESC",
                    "Vehicle.ADAS.ESC.IsEnabled",
                    "Vehicle.ADAS.ESC.IsEngaged",
                    "Vehicle.ADAS.ESC.IsError",
                    "Vehicle.ADAS.ESC.IsStrongCrossWindDetected",
                    "Vehicle.ADAS.ESC.RoadFriction",
                    "Vehicle.ADAS.ESC.RoadFriction.LowerBound",
                    "Vehicle.ADAS.ESC.RoadFriction.MostProbable",
                    "Vehicle.ADAS.ESC.RoadFriction.UpperBound",
                    "Vehicle.Speed",
                    "Vehicle.AverageSpeed",
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

            then("A Leaf (Vehicle.ADAS.ESC.RoadFriction.LowerBound) with min, max and unit is correctly parsed") {
                val specModel = specModels.find { it.vssPath == "Vehicle.ADAS.ESC.RoadFriction.LowerBound" }
                    ?: fail("Could not find Vehicle.ADAS.ESC.RoadFriction.LowerBound")

                specModel.description shouldContain "Lower bound road friction,"
                specModel.type shouldBe "sensor"
                specModel.uuid shouldBe "634289f58b5d511ea9979f04a9d0f2ab"
                specModel.comment shouldBe ""
                specModel.datatype shouldBe "float"
                specModel.vssNodeProperties
                    .find {
                        it.nodePropertyName == KEY_DATA_MIN
                    }?.nodePropertyValue shouldBe "0"
                specModel.vssNodeProperties
                    .find {
                        it.nodePropertyName == KEY_DATA_MAX
                    }?.nodePropertyValue shouldBe "100"
                specModel.vssNodeProperties
                    .find {
                        it.nodePropertyName == KEY_DATA_UNIT
                    }?.nodePropertyValue shouldBe "percent"
            }
        }

        `when`("Parsing vss_rel_4.1.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_4.1.json")
            val specModels = classUnderTest.parseNodes(fullSpecFile)

            then("the correct number of models models should be parsed") {
                specModels.size shouldBe 1271 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_4.0.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_4.0.json")
            val specModels = classUnderTest.parseNodes(fullSpecFile)

            then("the correct number of models models should be parsed") {
                specModels.size shouldBe 1197 // counted occurrences of '"uuid":' in specFile
            }
        }

        `when`("Parsing vss_rel_3.1.1.json") {
            val fullSpecFile = TestResourceFile("json/vss_rel_3.1.1.json")
            val result = runCatching {
                classUnderTest.parseNodes(fullSpecFile)
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
                classUnderTest.parseNodes(fullSpecFile)
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
                classUnderTest.parseNodes(fullSpecFile)
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
                classUnderTest.parseNodes(fullSpecFile)
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
                classUnderTest.parseNodes(fullSpecFile)
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
                classUnderTest.parseNodes(incompatibleFile)
            }

            then("An IOException is thrown") {
                result.exceptionOrNull() shouldBe instanceOf(IOException::class)
            }
        }

        `when`("Parsing a non-json file") {
            val nonJsonFile = TestResourceFile("yaml/vss_rel_4.0.yaml")
            val result = runCatching {
                classUnderTest.parseNodes(nonJsonFile)
            }

            then("An IOException is thrown") {
                result.exceptionOrNull() shouldBe instanceOf(IOException::class)
            }
        }
    }
})
