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

package org.eclipse.kuksa

/**
 * Generic Listener interface, to support multiple listeners.
 */
class MultiListener<T> {
    private var listeners: MutableSet<T> = mutableSetOf()

    /**
     * Adds a new [listener] and returns true if the [listener] was successfully added, returns false otherwise.
     * A [listener] can only be added once.
     */
    fun register(listener: T): Boolean {
        return listeners.add(listener)
    }

    /**
     * Removes a [listener] and returns true if the [listener] was successfully removed, returns false otherwise.
     */
    fun unregister(listener: T): Boolean {
        return listeners.remove(listener)
    }

    /**
     * Retrieves a defensive copy of the underlying list of listeners.
     */
    @JvmSynthetic
    internal fun get(): List<T> {
        return listeners.toList()
    }
}
