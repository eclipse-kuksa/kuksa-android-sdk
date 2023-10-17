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

package org.eclipse.kuksa.vsscore.extension

/**
 * Converts a string into the CamelCase convention.
 */
internal val String.toCamelCase: String
    get() {
        val regex = "(?=[A-Z])".toRegex()
        val words = split(regex)
        val result = StringBuilder()

        val mergedWords = mutableListOf<String>()

        for ((index, word) in words.withIndex()) {
            if (index == 0) {
                if (word.isNotEmpty()) mergedWords.add(word)

                continue
            }

            val isOneLetterWord = word.count() == 1
            val previousWord = words.getOrNull(index - 1) ?: ""
            val isPreviousWordOneLetter = previousWord.count() == 1
            if (isOneLetterWord && isPreviousWordOneLetter) {
                val mergedWord = mergedWords.removeLast() + word
                mergedWords.add(mergedWord)
            } else {
                mergedWords.add(word)
            }
        }

        for ((index, word) in mergedWords.withIndex()) {
            if (index == 0) {
                result.append(word.lowercase())
                continue
            }

            if (word.length <= 2) {
                result.append(word)
            } else {
                result.append(word.first())
                result.append(word.substring(1).lowercase())
            }
        }

        return result.toString()
    }
