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

import androidx.lifecycle.ViewModel
import org.eclipse.kuksa.testapp.collection.MaxElementSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val MAX_NUMBER_LOG_ENTRIES = 100

class OutputViewModel : ViewModel() {
    private val logEntries = MaxElementSet<String>(MAX_NUMBER_LOG_ENTRIES)

    var output: List<String> by mutableStateOf(listOf())
        private set

    fun appendOutput(text: String) {
        val emptyLines = if (logEntries.isEmpty()) "\n" else "\n\n"
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
        val date = LocalDateTime.now().format(dateFormatter)
        logEntries += "$emptyLines- $date\n $text"

        output = logEntries.toList()
    }

    fun clear() {
        logEntries.clear()

        output = logEntries.toList()
    }
}
