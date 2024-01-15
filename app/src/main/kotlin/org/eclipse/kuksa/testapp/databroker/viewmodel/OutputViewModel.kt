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

package org.eclipse.kuksa.testapp.databroker.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.kuksa.testapp.collection.MaxElementSet
import java.time.LocalDateTime

private const val MAX_NUMBER_LOG_ENTRIES = 100

class OutputViewModel : ViewModel() {
    private val outputEntries = MaxElementSet<OutputEntry>(MAX_NUMBER_LOG_ENTRIES)

    var output: List<OutputEntry> by mutableStateOf(listOf())
        private set

    fun addOutputEntry(message: String) {
        val messages = listOf(message)
        val outputEntry = OutputEntry(messages = messages)

        addOutputEntry(outputEntry)
    }

    fun addOutputEntry(outputEntry: OutputEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                outputEntries.add(outputEntry)

                output = outputEntries.toList()
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                outputEntries.clear()

                output = outputEntries.toList()
            }
        }
    }
}

class OutputEntry(
    val localDateTime: LocalDateTime = LocalDateTime.now(),
    messages: List<String> = mutableListOf(),
) {
    private var _messages: MutableList<String> = messages.toMutableList()
    val messages: List<String>
        get() = _messages

    fun addMessage(message: String) {
        _messages.add(message)
    }
}
