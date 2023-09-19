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

package org.eclipse.kuksa.vsscore.model

import kotlin.reflect.KClass

/**
 * Represents a node inside a VSS specification file.
 */
interface VssNode {
    /**
     * A collection of all initialized children.
     */
    val children: Set<VssSpecification>
        get() = emptySet()

    /**
     * The [KClass] of the parent. Can be used to reconstruct the node tree from a child perspective.
     */
    val parentClass: KClass<*>?
        get() = null
}

interface VssSpecification : VssNode {
    val uuid: String
    val vssPath: String
    val description: String
    val type: String
    val comment: String
}

interface VssProperty<T : Any> : VssSpecification {
    val value: T
}

/**
 * Splits the [VssSpecification.vssPath] into its parts.
 */
val VssSpecification.specificationKeys: List<String>
    get() = vssPath.split(".")

/**
 * Parses a name from the [VssSpecification.vssPath].
 */
val VssSpecification.name: String
    get() = vssPath.substringAfterLast(".")

/**
 * Iterates through all nested children which also may have children and aggregates them into one big collection.
 */
val VssSpecification.heritage: List<VssSpecification>
    get() = children.toList() + children.flatMap { it.heritage }

fun VssSpecification.findHeritageLine(heir: VssSpecification): List<VssSpecification> {
    val specificationKeys = heir.specificationKeys
    return heritage.filter { child ->
        specificationKeys.contains(child.name)
    }
}
