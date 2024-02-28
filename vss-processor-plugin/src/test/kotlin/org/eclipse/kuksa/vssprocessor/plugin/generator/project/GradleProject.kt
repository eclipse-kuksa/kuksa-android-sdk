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

import java.nio.file.Path
import kotlin.io.path.createDirectories

abstract class GradleProject(val name: String, testFolder: String = TEST_FOLDER_NAME_DEFAULT) : AutoCloseable {
    val rootProjectDir = Path.of(testFolder).createDirectories()
    open val projectDir: Path = rootProjectDir

    val buildDir: Path
        get() = projectDir.resolve("build")

    abstract fun generate(appendix: String = "")

    open fun refresh() {
        generate()
    }

    override fun close() {
        rootProjectDir.toFile().deleteRecursively()
    }

    companion object {
        const val TEST_FOLDER_NAME_DEFAULT = "build/functionalTest/"
    }
}

val dollar: String
    get() = "\$"
