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

package org.eclipse.kuksa.pattern.listener

/**
 * The MultiListener supports registering and unregistering of multiple different listeners.
 * The ListenerCollection is backed by a LinkedHashSet to prevent the same listener from being registered multiple
 * times. The order of registered elements is kept in tact.
 */
class MultiListener<T : Listener>(
    private val onRegistered: ((T) -> Unit)? = null,
    private val onUnregistered: ((T) -> Unit)? = null,
) : ListenerCollection<T> {
    private var listeners: MutableSet<T> = LinkedHashSet()

    override fun register(listener: T): Boolean {
        val isAdded = listeners.add(listener)
        if (isAdded) {
            onRegistered?.invoke(listener)
        }
        return isAdded
    }

    override fun unregister(listener: T): Boolean {
        val isRemoved = listeners.remove(listener)
        if (isRemoved) {
            onUnregistered?.invoke(listener)
        }
        return isRemoved
    }

    override fun isEmpty(): Boolean {
        return listeners.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return listeners.iterator()
    }
}
