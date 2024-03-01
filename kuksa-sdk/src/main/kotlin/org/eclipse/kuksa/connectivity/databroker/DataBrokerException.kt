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
 */

package org.eclipse.kuksa.connectivity.databroker

/**
 * An Exception which will be thrown when there are problems with the connection to the DataBroker.
 *
 * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
 * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
 * value is permitted, and indicates that the cause is nonexistent or unknown.)
 */
class DataBrokerException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Exception(message, cause)
