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

package org.eclipse.kuksa.vsscore.model

import kotlin.reflect.KClass

/**
 * Some [VssNode] may have an additional [value] property. These are children [VssSignal] which do not have other
 * children.
 */
interface VssSignal<out T : Any> : VssNode {
    /**
     * A primitive type value.
     */
    val value: T

    /**
     * The VSS data type which is compatible with the Databroker. This may differ from the [value] type because
     * Java compatibility needs to be ensured and inline classes like [UInt] (Kotlin) are not known to Java.
     *
     * ### Example
     *     Vehicle.Driver.HeartRate:
     *     datatype: uint16
     *
     * generates -->
     *
     *     public data class VssHeartRate (
     *         override val `value`: Int = 0,
     *     ) : VssSignal<Int> {
     *         override val dataType: KClass<*>
     *             get() = UInt:class
     *     }
     *
     * To ensure java compatibility [UInt] is not used here for Kotlin (inline class).
     */
    val dataType: KClass<*>
        get() = value::class
}

/**
 * Finds the given [signal] inside the current [VssNode].
 */
inline fun <reified T : VssSignal<V>, V : Any> VssNode.findSignal(signal: T): VssNode {
    return heritage
        .first { it.uuid == signal.uuid }
}

/**
 * Finds all [VssSignal] which matches the given [KClass.simpleName]. This is useful when multiple nested objects
 * with the same Name exists but are pretty much the same besides the [VssNode.vssPath] etc.
 */
inline fun <reified T : VssSignal<V>, V : Any> VssNode.findSignal(type: KClass<T>): Map<String, VssNode> {
    return heritage
        .filter { it::class.simpleName == type.simpleName }
        .associateBy { it.vssPath }
}
