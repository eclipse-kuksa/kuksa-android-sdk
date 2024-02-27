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

package org.eclipse.kuksa.test.kotest

import io.kotest.core.NamedTag

val Integration = NamedTag("Integration")
val DefaultDatabroker = NamedTag("DefaultDatabroker") // unsecure => no tls, no authentication
val CustomDatabroker = NamedTag("CustomDatabroker")
val Secure = NamedTag("Secure") // tls enabled, no authentication
val Authentication = NamedTag("Authentication") // no tls, authentication enabled
val Unit = NamedTag("Unit")

val Insecure = NamedTag("Insecure")
