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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.eclipse.kuksa.extension.lib

val baselineFile = project.file("$rootDir/config/detekt/baseline.xml")

plugins {
    id("io.gitlab.arturbosch.detekt") // see https://github.com/detekt/detekt
}

dependencies {
    detektPlugins(lib("detekt-formatting"))
}

detekt {
    buildUponDefaultConfig = true
    allRules = false // only stable rules
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    parallel = true
    setSource(projectDir)
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**", "**/node_modules/**")
    config.setFrom(project.file("$rootDir/config/detekt/config.yml"))
    baseline.set(baselineFile)

    jvmTarget = "1.8"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"

    setSource(projectDir)
    baseline.set(baselineFile)
    include("**/*.kt", "**/*.kts")
    exclude("**/resources/**", "**/build/**")
    config.setFrom(project.file("$rootDir/config/detekt/config.yml"))
}
