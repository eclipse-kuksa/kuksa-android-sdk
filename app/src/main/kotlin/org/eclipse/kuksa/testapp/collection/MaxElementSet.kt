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

package org.eclipse.kuksa.testapp.collection

class MaxElementSet<T>(private val maxNumberEntries: Int = Int.MAX_VALUE) : MutableSet<T> {

    private val map: LinkedHashMap<T, Boolean> = object : LinkedHashMap<T, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<T, Boolean>?): Boolean {
            return size > maxNumberEntries
        }
    }

    override val size: Int
        get() = map.size

    override fun contains(element: T): Boolean {
        return map[element] == true
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        var containsAll = true
        elements.forEach { element ->
            containsAll = containsAll && contains(element)
        }
        return containsAll
    }

    override fun add(element: T): Boolean {
        map[element] = true
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val associatedElements = elements.associateWith { true }

        map.putAll(associatedElements)
        return true
    }

    override fun clear() {
        map.clear()
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return map.keys.iterator()
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException("not supported")
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        elements.forEach { element ->
            map.remove(element)
        }
        return true
    }

    override fun remove(element: T): Boolean {
        return map.remove(element) != null
    }
}
