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

package org.eclipse.kuksa.connectivity.authentication

/**
 * A JsonWebToken can be used to authenticate against the DataBroker. For authentication to work the DataBroker must be
 * started with authentication enabled first.
 *
 * The JsonWebToken is defined by an [authScheme] and [token]. The [authScheme] is set to "Bearer". The [token] should
 * contain a valid JsonWebToken.
 *
 * It will be send to the DataBroker as part of the Header Metadata in the following format:
 *
 * ```
 * Headers
 *  Authorization: [authScheme] [token]
 * ```
 */
data class JsonWebToken(
    val token: String,
) {
    val authScheme: String
        get() = DEFAULT_AUTH_SCHEME

    private companion object {
        private const val DEFAULT_AUTH_SCHEME = "Bearer"
    }
}
