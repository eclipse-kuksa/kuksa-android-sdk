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

package org.eclipse.kuksa.testapp.extension.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun rememberCountdown(
    initialMillis: Long,
    step: Long = 1000,
): MutableState<Long> {
    val timeLeft = remember { mutableStateOf(initialMillis) }

    LaunchedEffect(initialMillis, step) {
        while (isActive && timeLeft.value > 0) {
            val newTimeLeft = (timeLeft.value - step).coerceAtLeast(0)
            timeLeft.value = newTimeLeft

            val maximumDelay = step.coerceAtMost(newTimeLeft)
            delay(maximumDelay)
        }
    }

    return timeLeft
}
