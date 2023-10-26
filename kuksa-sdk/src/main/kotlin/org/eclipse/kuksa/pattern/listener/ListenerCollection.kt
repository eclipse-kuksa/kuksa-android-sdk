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
 * The ListenerCollection interface provides methods to register and unregister multiple listeners with a generic type.
 * The underlying collection decides if the same listener can be added only once or multiple times.
*/
interface ListenerCollection<T : Listener> : Iterable<T> {
    /**
     * Adds a new [listener] and returns true if the [listener] was successfully added, returns false otherwise.
     */
    fun register(listener: T): Boolean

    /**
     * Removes a [listener] and returns true if the [listener] was successfully removed, returns false otherwise.
     */
    fun unregister(listener: T): Boolean

    /**
     * isEmpty checks the number of registered listeners and returns true, if no listener is registered or false if at
     * least one listener is registered.
     */
    fun isEmpty(): Boolean
}
