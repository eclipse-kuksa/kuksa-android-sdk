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
 * Vehicle Signal Specification definition file by the "vss-processor-plugin". Currently VSS files in
 * .yaml and .json format are supported by the vss-processor.
 * Use the "VSS Processor Plugin" to provide the Symbol Processor with the necessary VSS file(s).
 *
 * ### Plugin Example
 *
 * ```
 * // app/build.gradle.kts
 * plugins {
 *     id("org.eclipse.kuksa.vss-processor-plugin") version "<VERSION>"
 * }
 *
 * // Optional - See plugin documentation. Files inside the "$rootDir/vss" folder are used automatically.
 * vssProcessor {
 *     searchPath = "$rootDir/vss"
 * }
 * ```
 *
 * ### Annotation Example
 *
 * ```
 * @VssModelGenerator
 * class Activity
 * ```
 *
 * ### Important
 *
 * When using the KSP (Kotlin Symbol Processing) feature with this annotation in combination with android compose
 * then the incremental compiler for KSP needs to be disabled explicitly in the gradle properties.
 * ```
 * <ksp.incremental=false>
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class VssModelGenerator
