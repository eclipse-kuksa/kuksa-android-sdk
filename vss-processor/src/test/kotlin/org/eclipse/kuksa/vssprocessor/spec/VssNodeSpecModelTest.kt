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

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_DATATYPE
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MAX
import org.eclipse.kuksa.vssprocessor.parser.KEY_DATA_MIN

private const val TYPE_ANY = "any"
private const val TYPE_STRING = "string"
private const val TYPE_STRING_ARRAY = "string[]"
private const val TYPE_BOOLEAN = "boolean"
private const val TYPE_BOOLEAN_ARRAY = "boolean[]"
private const val TYPE_FLOAT = "float"
private const val TYPE_INT64 = "int64"
private const val TYPE_UINT32 = "uint32"
private const val TYPE_INT32 = "int32"
private const val TYPE_UINT64_ARRAY = "uint64[]"
private const val TYPE_INVALID = "invalid"

class VssNodeSpecModelTest : BehaviorSpec({
    given("String spec model") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_STRING, TYPE_STRING),
        )
        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.String = \"\""
            }
        }
    }

    given("int64 spec model with min and max values") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_INT64, TYPE_INT64),
            createSignalProperty(KEY_DATA_MIN, "0", TYPE_INT64),
            createSignalProperty(KEY_DATA_MAX, "100", TYPE_INT64),
        )
        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Long = 0L"
            }

            then("it's min values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val minSpec = classSpec.propertySpecs.find { it.name == "min" }
                minSpec?.type shouldBe valueSpec?.type
            }

            then("it's max values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val maxSpec = classSpec.propertySpecs.find { it.name == "max" }
                maxSpec?.type shouldBe valueSpec?.type
            }
        }
    }

    given("uint32 spec model (inline class)") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_UINT32, TYPE_UINT32),
            createSignalProperty(KEY_DATA_MIN, "0", TYPE_UINT32),
            createSignalProperty(KEY_DATA_MAX, "100", TYPE_UINT32),
        )
        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct data type") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Int = 0"
            }

            then("it should have the correct inline class data type") {
                val propertySpec = classSpec.propertySpecs.find { it.name == "dataType" }

                propertySpec?.getter.toString() shouldContain "kotlin.UInt::class"
            }

            then("it's min values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val minSpec = classSpec.propertySpecs.find { it.name == "min" }
                minSpec?.type shouldBe valueSpec?.type
            }

            then("it's max values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val maxSpec = classSpec.propertySpecs.find { it.name == "max" }
                maxSpec?.type shouldBe valueSpec?.type
            }
        }
    }

    given("int32 spec model with min and max values") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_INT32, TYPE_INT32),
            createSignalProperty(KEY_DATA_MIN, "0", TYPE_INT32),
            createSignalProperty(KEY_DATA_MAX, "100", TYPE_INT32),
        )
        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Int = 0"
            }

            then("it's min values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val minSpec = classSpec.propertySpecs.find { it.name == "min" }
                minSpec?.type shouldBe valueSpec?.type
            }

            then("it's max values should have the same type as value") {
                val valueSpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                val maxSpec = classSpec.propertySpecs.find { it.name == "max" }
                maxSpec?.type shouldBe valueSpec?.type
            }
        }
    }

    given("uint64[] spec model") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_UINT64_ARRAY, TYPE_UINT64_ARRAY),
        )

        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.LongArray = LongArray(0)"
            }

            then("it should have the correct inline class data type") {
                val propertySpec = classSpec.propertySpecs.find { it.name == "dataType" }

                propertySpec?.getter.toString() shouldContain "kotlin.ULongArray::class"
            }
        }
    }

    given("String[] spec model") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_STRING_ARRAY, TYPE_STRING_ARRAY),
        )
        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Array<kotlin.String> = emptyArray<String>()"
            }
        }
    }

    given("Boolean[] spec model") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_BOOLEAN_ARRAY, TYPE_BOOLEAN_ARRAY),
        )

        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with the correct datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.BooleanArray = BooleanArray(0)"
            }
        }
    }

    given("Any spec model") {
        val vssNodeProperties = setOf(
            createSignalProperty(KEY_DATA_DATATYPE, TYPE_ANY, TYPE_ANY),
        )

        val specModel = VssNodeSpecModel(vssPath = "Vehicle.IgnitionType", vssNodeProperties)

        `when`("creating a class spec") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with an UNKNOWN (Any) datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Any = null"
            }
        }
    }

    given("Parent Spec model") {
        val specModel = VssNodeSpecModel(vssPath = "Vehicle", setOf())

        `when`("creating a class spec without children and nested classes") {
            val classSpec = specModel.createClassSpec("test")

            then("it should have a value with an UNKNOWN (Any) datatype") {
                val propertySpec = classSpec.primaryConstructor?.parameters?.find { it.name == "value" }

                propertySpec.toString() shouldContain "kotlin.Any = null"
            }
        }
        and("related nodes") {
            val vehicleSpeedSpecModel =
                VssNodeSpecModel(
                    vssPath = "Vehicle.Speed",
                    vssNodeProperties = setOf(createSignalProperty(KEY_DATA_DATATYPE, TYPE_FLOAT, TYPE_FLOAT)),
                )
            val relatedVssNodes = listOf(
                VssNodeSpecModel(vssPath = "Vehicle.SmartphoneProjection", setOf()),
                VssNodeSpecModel(
                    vssPath = "Vehicle.IsBrokenDown",
                    vssNodeProperties = setOf(
                        createSignalProperty(
                            KEY_DATA_DATATYPE,
                            TYPE_BOOLEAN,
                            TYPE_BOOLEAN,
                        ),
                    ),
                ),
                vehicleSpeedSpecModel,
                VssNodeSpecModel(
                    vssPath = "Vehicle.SupportedMode",
                    vssNodeProperties = setOf(
                        createSignalProperty(KEY_DATA_DATATYPE, TYPE_STRING_ARRAY, TYPE_STRING_ARRAY),
                    ),
                ),
                VssNodeSpecModel(
                    vssPath = "Vehicle.AreSeatsHeated",
                    vssNodeProperties = setOf(
                        createSignalProperty(
                            KEY_DATA_DATATYPE,
                            TYPE_BOOLEAN_ARRAY,
                            TYPE_BOOLEAN_ARRAY,
                        ),
                    ),
                ),
                VssNodeSpecModel(
                    vssPath = "Vehicle.Invalid",
                    vssNodeProperties = setOf(createSignalProperty(KEY_DATA_DATATYPE, TYPE_INVALID, TYPE_INVALID)),
                ),
            )

            `when`("creating a class spec with children") {
                val rootClassSpec = specModel.createClassSpec("test", relatedVssNodes)

                then("it should contain the child properties") {
                    val isBrokenDownPropertySpec = rootClassSpec.propertySpecs.find { it.name == "isBrokenDown" }
                    val childrenPropertySpec = rootClassSpec.propertySpecs.find { it.name == "children" }

                    rootClassSpec.name shouldBe "VssVehicle"
                    rootClassSpec.propertySpecs.size shouldBe 13 // 8 interface props + 5 children

                    isBrokenDownPropertySpec shouldNotBe null
                    childrenPropertySpec?.getter.toString() shouldContain "smartphoneProjection, isBrokenDown"
                }

                then("it should have no parent") {
                    val parentPropertySpec = rootClassSpec.propertySpecs.find { it.name == "parentClass" }

                    parentPropertySpec?.getter.toString() shouldContain "null"
                }

                and("a child class spec is created") {
                    val vehicleSpeedClassSpec = vehicleSpeedSpecModel.createClassSpec("test")

                    then("it should have the root class as parent") {
                        val parentPropertySpec = vehicleSpeedClassSpec.propertySpecs.find { it.name == "parentClass" }

                        parentPropertySpec?.getter.toString() shouldContain "VssVehicle"
                    }
                }
            }
            and("nested nodes") {
                val nestedVssNodes = listOf("Speed")

                `when`("creating a child class spec with nested children") {
                    val classSpec = specModel.createClassSpec(
                        "test",
                        relatedVssNodes,
                        nestedVssNodes,
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

private fun createSignalProperty(
    propertyName: String,
    propertyValue: String,
    propertyType: String,
): VssNodeProperty {
    val vssDataType = VssDataType.find(propertyType)
    return VssSignalProperty(
        vssPath = "Vehicle.ADAS.ABS.IsEnabled",
        nodePropertyName = propertyName,
        nodePropertyValue = propertyValue,
        dataType = vssDataType.valueDataType,
    )
}
