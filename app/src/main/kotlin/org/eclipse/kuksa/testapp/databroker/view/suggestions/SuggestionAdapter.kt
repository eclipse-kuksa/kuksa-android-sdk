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

package org.eclipse.kuksa.testapp.databroker.view.suggestions

interface SuggestionAdapter<T : Any> {
    val items: Collection<T>

    val startingItem: T

    fun toString(item: T): String = item.toString()
}

class DefaultSuggestionAdapter<T : Any>(override val items: Collection<T> = emptyList()) : SuggestionAdapter<T> {
    override val startingItem: T
        get() = items.first()

    override fun toString(item: T): String {
        return item.toString()
    }
}
