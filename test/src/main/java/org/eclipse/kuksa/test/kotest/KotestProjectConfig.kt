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

package org.eclipse.kuksa.test.kotest

import io.kotest.assertions.nondeterministic.continuallyConfig
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.config.AbstractProjectConfig
import kotlin.time.Duration.Companion.seconds

val eventuallyConfiguration = eventuallyConfig {
    duration = 1.seconds
}

val continuallyConfiguration = continuallyConfig<Any> {
    duration = 1.seconds
}

// https://kotest.io/docs/framework/project-config.html
object KotestProjectConfig : AbstractProjectConfig() {
    override var displayFullTestPath: Boolean? = true
}
