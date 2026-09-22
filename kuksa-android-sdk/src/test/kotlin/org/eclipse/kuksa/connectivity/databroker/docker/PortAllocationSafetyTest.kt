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

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class PortAllocationSafetyTest : BehaviorSpec({
    given("Port allocation mechanism") {
        `when`("Allocating multiple ephemeral ports") {
            then("It returns valid available port numbers") {
                val ports = (1..10).map {
                    DataBrokerDockerContainer.findAvailablePort()
                }
                ports.size shouldBeGreaterThan 0
                ports.all { it > 1024 } shouldBe true
            }
        }
    }
})
