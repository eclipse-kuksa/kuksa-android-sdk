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

import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

/**
 * Uses reflection to create a copy with any constructor parameter which matches the given [paramToValue] map.
 * It is recommend to only use data classes.
 *
 * @throws [IllegalArgumentException] if the copied types do not match.
 * @throws [NoSuchElementException] if no copy method was found for the class.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> T.copy(paramToValue: Map<String, Any?> = emptyMap()): T {
    val instanceClass = this::class

    val copyFunction = instanceClass::memberFunctions.get().first { it.name == "copy" }
    val instanceParameter = copyFunction.instanceParameter ?: return this

    val valueArgs = copyFunction.valueParameters
        .mapNotNull { parameter ->
            paramToValue[parameter.name]?.let { value -> parameter to value }
        }

    val parameterToInstance = mapOf(instanceParameter to this)
    val parameterToValue = parameterToInstance + valueArgs

    val copy: Any
    try {
        copy = copyFunction.callBy(parameterToValue) ?: this
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("${this::class.simpleName} copy parameters do not match: $paramToValue", e)
    }

    return copy as T
}
