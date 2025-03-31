/*
 * Copyright (c) 2023 - 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.mocking

import org.eclipse.kuksa.test.TestResourceFile
import java.io.InputStream

// The tokens provided here might need to be updated irregularly
// see: https://github.com/eclipse/kuksa.val/tree/master/jwt
// The tokens only work when the Databroker is started using the correct public key: jwt.key.pub
enum class JwtType(private val fileName: String) {
    READ_WRITE_ALL("authentication/actuate-provide-all.token"), // ACTUATOR_TARGET and VALUE
    READ_WRITE_ALL_VALUES_ONLY("authentication/provide-all.token"), // VALUE
    READ_ALL("authentication/read-all.token"),
    ;

    fun asInputStream(): InputStream {
        val resourceFile = TestResourceFile(fileName)
        return resourceFile.inputStream()
    }
}
