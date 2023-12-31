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

package org.eclipse.kuksa.vsscore.annotation

/**
 * Add this annotation to any class to trigger the model generation (Kotlin Symbol Processing) for the given
 * Vehicle Signal Specification definition file. Only .yaml files are currently supported. The searched root folder
 * is the assets folder (example path: app/src/main/assets).
 *
 * ### Example
 *
 * ```
 * @VssDefinition("vss_rel_4.0.yaml")
 * ```
 *
 * ### Important
 *
 * When using the KSP (Kotlin Symbol Processing) feature with this annotation in combination with android compose
 * then the incremental compiler for KSP needs to be disabled explicitly in the gradle properties.
 * ```
 * <ksp.incremental=false>
 *
 * @param vssDefinitionPath the path to the definition file
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class VssDefinition(val vssDefinitionPath: String)
