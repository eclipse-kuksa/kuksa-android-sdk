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

package org.eclipse.kuksa.vssprocessor.spec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class VssSpecificationSpecModelTest : BehaviorSpec({
    given("String spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "string", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.String = \"\""
            }
        }
    }

    given("int64 spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "int64", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Long = 0L"
            }
        }
    }

    given("uint64[] spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "uint64[]", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.LongArray = LongArray(0)"
            }
        }
    }

    given("String[] spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "string[]", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Array<kotlin.String> = emptyArray<String>()"
            }
        }
    }

    given("Boolean[] spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "boolean[]", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.BooleanArray = BooleanArray(0)"
            }
        }
    }

    given("Any spec model") {
        val specModel = VssSpecificationSpecModel(datatype = "any", vssPath = "Vehicle.IgnitionType")

        `when`("creating a class spec") {
            val exception = shouldThrow<IllegalArgumentException> {
                specModel.createClassSpec("test")
            }

            then("it should throw an exception") {
                exception shouldNotBe null
            }
        }
    }

    given("Parent Spec model") {
        val specModel = VssSpecificationSpecModel(vssPath = "Vehicle")

        `when`("creating a class spec without children and nested classes") {
            val exception = shouldThrow<IllegalArgumentException> {
                specModel.createClassSpec("test")
            }

            then("it should throw an exception because it is missing a value") {
                exception shouldNotBe null
            }
        }
        and("related specifications") {
            val relatedSpecifications = listOf(
                VssSpecificationSpecModel(vssPath = "Vehicle.SmartphoneProjection"),
                VssSpecificationSpecModel(datatype = "boolean", vssPath = "Vehicle.IsBrokenDown"),
                VssSpecificationSpecModel(datatype = "float", vssPath = "Vehicle.Speed"),
                VssSpecificationSpecModel(datatype = "string[]", vssPath = "Vehicle.SupportedMode"),
                VssSpecificationSpecModel(datatype = "boolean[]", vssPath = "Vehicle.AreSeatsHeated"),
                VssSpecificationSpecModel(datatype = "invalid", vssPath = "Vehicle.Invalid"),
            )

            `when`("creating a child class spec with children") {
                val classSpec = specModel.createClassSpec("test", relatedSpecifications)

                then("it should contain the child properties") {
                    val isBrokenDownPropertySpec = classSpec.propertySpecs.find { it.name == "isBrokenDown" }
                    val childrenPropertySpec = classSpec.propertySpecs.find { it.name == "children" }

                    classSpec.name shouldBe "VssVehicle"
                    classSpec.propertySpecs.size shouldBe 13 // 8 interface props + 5 children

                    isBrokenDownPropertySpec shouldNotBe null
                    childrenPropertySpec?.getter.toString() shouldContain "smartphoneProjection, isBrokenDown"
                }
            }
            and("nested specifications") {
                val nestedSpecifications = listOf("Speed")

                `when`("creating a child class spec with nested children") {
                    val classSpec = specModel.createClassSpec(
                        "test",
                        relatedSpecifications,
                        nestedSpecifications,
                    )

                    then("it should contain the nested children") {
                        val nestedPropertySpec = classSpec.typeSpecs.find { it.name == "VssSpeed" }

                        nestedPropertySpec shouldNotBe null
                    }
                }
            }
        }
    }
})
