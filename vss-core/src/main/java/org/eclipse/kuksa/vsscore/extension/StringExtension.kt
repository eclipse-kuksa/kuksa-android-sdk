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
