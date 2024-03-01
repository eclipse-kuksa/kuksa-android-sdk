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

package org.eclipse.kuksa.coroutine

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The CoroutineCallback can be used when calling kotlin suspend functions of our public API.
 */
abstract class CoroutineCallback<T> : Continuation<T> {

    override val context: CoroutineContext = EmptyCoroutineContext

    /**
     * Will be called with the [result] when the coroutine finished.
     */
    abstract fun onSuccess(result: T?)

    /**
     * Will be called with the [error] when an exception is thrown during the execution of the coroutine.
     */
    abstract fun onError(error: Throwable)

    override fun resumeWith(result: Result<T>) {
        val value: T? = result.getOrNull()
        val exception: Throwable? = result.exceptionOrNull()

        if (exception != null) {
            onError(exception)
        } else {
            onSuccess(value)
        }
    }
}
