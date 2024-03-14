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

import com.google.common.net.HttpHeaders
import io.grpc.ClientInterceptor
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.eclipse.kuksa.proto.v1.VALGrpc.VALBlockingStub
import org.eclipse.kuksa.proto.v1.VALGrpc.VALStub

internal fun VALBlockingStub.withAuthenticationInterceptor(jsonWebToken: JsonWebToken?): VALBlockingStub {
    if (jsonWebToken == null) return this

    val authenticationInterceptor = clientInterceptor(jsonWebToken)
    return withInterceptors(authenticationInterceptor)
}

internal fun VALStub.withAuthenticationInterceptor(jsonWebToken: JsonWebToken?): VALStub {
    if (jsonWebToken == null) return this

    val authenticationInterceptor = clientInterceptor(jsonWebToken)
    return withInterceptors(authenticationInterceptor)
}

private fun clientInterceptor(jsonWebToken: JsonWebToken): ClientInterceptor? {
    val authorizationHeader = Metadata.Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER)

    val metadata = Metadata()
    metadata.put(authorizationHeader, "${jsonWebToken.authScheme} ${jsonWebToken.token}")

    return MetadataUtils.newAttachHeadersInterceptor(metadata)
}
