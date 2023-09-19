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

package org.eclipse.kuksa.extension

/**
 * Creates Log Tags for the Android Logging Method. It will take care of the max allowed length of 23 characters by
 * removing all subsequent characters.
 */
internal inline val <reified T> T.TAG: String
    get() = logTag

/**
 * Creates Log Tags for the Android Logging Method. It will take care of the max allowed length of 23 characters by
 * removing all subsequent characters.
 */
internal inline val <reified T> T.logTag: String
    get() = T::class.java.name
        .substringAfterLast(".")
        .substringBefore("$")
        .take(MAX_CHARS_LOG_TAG)

internal const val MAX_CHARS_LOG_TAG = 23
