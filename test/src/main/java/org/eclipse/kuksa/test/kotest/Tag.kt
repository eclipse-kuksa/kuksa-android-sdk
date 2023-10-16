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

<<<<<<<< HEAD:kuksa-sdk/src/test/kotlin/org/eclipse/kuksa/kotest/Tag.kt
package org.eclipse.kuksa.kotest
========
package org.eclipse.kuksa.test.kotest
>>>>>>>> Eclipse/main:test/src/main/java/org/eclipse/kuksa/test/kotest/Tag.kt

import io.kotest.core.NamedTag

val Integration = NamedTag("Integration")
val Unit = NamedTag("Unit")

val Secure = NamedTag("Secure")
val Insecure = NamedTag("Insecure")
