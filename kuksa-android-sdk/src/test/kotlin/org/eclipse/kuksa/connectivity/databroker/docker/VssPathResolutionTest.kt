/*
 * Copyright (c) 2023 - 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.connectivity.databroker.docker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.FileNotFoundException

class VssPathResolutionTest : BehaviorSpec({
    given("A VSS path resolver") {
        `when`("Resolving the default VSS 6.0 file from project root or submodules") {
            then("It resolves the file and verifies existence") {
                val resolved = resolveVssFile("vss/vss_release_6.0.json")
                resolved.exists() shouldBe true
                resolved.name shouldBe "vss_release_6.0.json"
            }
        }

        `when`("Resolving a non-existent VSS file") {
            then("It throws FileNotFoundException with a descriptive message") {
                val exception = shouldThrow<FileNotFoundException> {
                    resolveVssFile("non_existent_folder/fake_vss.json")
                }
                exception.message?.contains("Could not resolve VSS file") shouldBe true
            }
        }
    }
})
