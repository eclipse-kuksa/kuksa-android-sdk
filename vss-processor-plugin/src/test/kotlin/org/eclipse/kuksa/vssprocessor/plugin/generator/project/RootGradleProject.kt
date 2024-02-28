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

package org.eclipse.kuksa.vssprocessor.plugin.generator.project

import kotlin.io.path.appendLines

abstract class RootGradleProject(name: String) : GradleProject(name) {
    protected val settingsFile = rootProjectDir.resolve("settings.gradle.kts")
    protected val rootBuildFile = rootProjectDir.resolve("build.gradle.kts")

    private val addedProjects = mutableListOf<GradleProject>()

    fun add(project: GradleProject) {
        if (addedProjects.isEmpty()) settingsFile.appendLines(setOf(""))

        val addedProject = setOf(
            """
            include(":${project.name}")
            """.trimIndent(),
        )
        settingsFile.appendLines(addedProject)

        addedProjects.add(project)
    }

    override fun refresh() {
        super.refresh()

        addedProjects.clear()
    }
}
