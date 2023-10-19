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
import java.time.format.DateTimeFormatter

private const val MAX_NUMBER_LOG_ENTRIES = 100

class OutputViewModel : ViewModel() {
    private val logEntries = MaxElementSet<String>(MAX_NUMBER_LOG_ENTRIES)

    var output: List<String> by mutableStateOf(listOf())
        private set

    fun appendOutput(text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                val sanitizedText = sanitizeString(text)

                val emptyLines = if (logEntries.isEmpty()) "\n" else "\n\n"
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
                val date = LocalDateTime.now().format(dateFormatter)
                logEntries += "$emptyLines- $date\n $sanitizedText"

                output = logEntries.toList()
            }
        }
    }

    // fixes a crash when outputting VssPath(Vehicle). The ScrollBar can't handle input with more than 3971 line breaks
    private fun sanitizeString(text: String): String {
        var sanitizedText = text
        val isTextTooLong = sanitizedText.length >= MAX_LENGTH_LOG_ENTRY
        if (isTextTooLong) {
            sanitizedText = sanitizedText.substring(0, MAX_LENGTH_LOG_ENTRY) + "…"
            sanitizedText += System.lineSeparator()
            sanitizedText += System.lineSeparator()
            sanitizedText += "Text is too long and was truncated"
        }

        return sanitizedText
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                logEntries.clear()

                output = logEntries.toList()
            }
        }
    }

    private companion object {
        private const val MAX_LENGTH_LOG_ENTRY = 90_000
    }
}
