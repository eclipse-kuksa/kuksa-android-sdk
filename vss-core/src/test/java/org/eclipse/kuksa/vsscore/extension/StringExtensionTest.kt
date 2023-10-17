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
 *
 */

package org.eclipse.kuksa.vsscore.extension

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class StringExtensionTest : BehaviorSpec({
    given("Multiple strings") {
        val testStrings = listOf("VehicleId", "vehicleIDENTIFIER", "myGreatVEHICLE")
        `when`("converting it to camelcase") {
            val camelCaseStrings = testStrings.map { it.toCamelCase }
            then("it should be in a valid camelcase format") {
                val expectedCamelCases = listOf("vehicleId", "vehicleIdentifier", "myGreatVehicle")
                camelCaseStrings shouldBe expectedCamelCases
            }
        }
    }

    // Special kotlin camelcase rule to keep abbreviations with two characters
    given("Multiple strings with with two abbreviations in a row") {
        val testStrings = listOf("VSSVehicleID", "VVehicleABvolume")
        `when`("converting it to camelcase") {
            val camelCaseStrings = testStrings.map { it.toCamelCase }
            then("it should keep abbreviations with two characters") {
                val expectedCamelCases = listOf("vssVehicleID", "vVehicleABvolume")
                camelCaseStrings shouldBe expectedCamelCases
            }
        }
    }
})
