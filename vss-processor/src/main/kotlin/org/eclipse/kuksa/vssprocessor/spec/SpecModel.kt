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

package org.eclipse.kuksa.vssprocessor.spec

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.squareup.kotlinpoet.TypeSpec

/**
 * Is used by the [SymbolProcessor] to generate a class spec which can be written into a file.
 */
internal interface SpecModel {
    /**
     * @param nestedClasses which can be used to create a class spec with nested classes.
     * @param packageName to use for the generated class specs.
     * @param logger to use for log output.
     */
    fun createClassSpec(
        nestedClasses: Set<String> = emptySet(),
        packageName: String,
        logger: KSPLogger,
    ): TypeSpec
}
