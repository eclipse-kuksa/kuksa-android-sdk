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

package org.eclipse.kuksa.vsscore.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class VssNodeTest : BehaviorSpec({
    given("The root level VssNode") {
        val vssVehicle = VssVehicle()
        `when`("finding the whole heritage") {
            val heritage = vssVehicle.heritage
            then("it should return all possible heirs") {
                heritage shouldBe listOf(
                    vssVehicle.driver,
                    vssVehicle.passenger,
                    vssVehicle.body,
                    vssVehicle.driver.heartRate,
                    vssVehicle.passenger.heartRate,
                )
            }
        }
        `when`("finding a heritage line") {
            val heritageLine = vssVehicle.findHeritageLine(VssDriver.VssHeartRate())
            then("it should return the correct line") {
                heritageLine shouldBe listOf(vssVehicle.driver, vssVehicle.driver.heartRate)
            }
        }
        `when`("finding specific leafs") {
            val leafs = vssVehicle.findSignal(VssDriver.VssHeartRate::class)
            then("it should return all leafs which fit the class") {
                leafs.size shouldBe 2
            }
        }
        `when`("getting the variable name") {
            val variableName = vssVehicle.variableName
            then("it should be correct") {
                variableName shouldBe "vehicle"
            }
        }
        `when`("getting the class name") {
            val variableName = vssVehicle.className
            then("it should be correct") {
                variableName shouldBe "VssVehicle"
            }
        }
    }

    given("A child VssNode") {
        val vssDriver = VssDriver()
        `when`("splitting it into path components") {
            val pathComponents = vssDriver.vssPathComponents
            then("it should be correctly split") {
                pathComponents shouldBe listOf("Vehicle", "Driver")
            }
        }
        `when`("generating the heritage line") {
            val heritageLine = vssDriver.vssPathHeritageLine
            then("it should be correct") {
                heritageLine shouldBe listOf("Vehicle", "Vehicle.Driver")
            }
        }
        `when`("parsing the name") {
            val name = vssDriver.name
            then("it should be correct") {
                name shouldBe "Driver"
            }
        }
        `when`("parsing the parent vss path") {
            val parentVssPath = vssDriver.parentVssPath
            then("it should be correct") {
                parentVssPath shouldBe "Vehicle"
            }
        }
        `when`("parsing the parent key") {
            val parentKey = vssDriver.parentKey
            then("it should be correct") {
                parentKey shouldBe "Vehicle"
            }
        }
    }
})
