/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.testapp.extension

import android.content.Context
import android.net.Uri
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import org.eclipse.kuksa.authentication.JsonWebToken
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import java.io.IOException

fun ConnectionInfo.createInsecureManagedChannel(): ManagedChannel {
    val host = host.trim()

    return ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .build()
}

@Throws(IOException::class)
fun ConnectionInfo.createSecureManagedChannel(context: Context): ManagedChannel {
    val rootCertFile = context.contentResolver.openInputStream(certificate.uri)

    val tlsCredentials: ChannelCredentials = TlsChannelCredentials.newBuilder()
        .trustManager(rootCertFile)
        .build()

    val host = host.trim()
    val port = port
    val channelBuilder = Grpc
        .newChannelBuilderForAddress(host, port, tlsCredentials)

    val overrideAuthority = certificate.overrideAuthority.trim()
    val hasOverrideAuthority = overrideAuthority.isNotEmpty()
    if (hasOverrideAuthority) {
        channelBuilder.overrideAuthority(overrideAuthority)
    }

    return channelBuilder.build()
}

@Throws(IOException::class)
fun ConnectionInfo.loadJsonWebToken(context: Context): JsonWebToken? {
    if (!isAuthenticationEnabled || jwtUriPath == null) {
        return null
    }

    val uri: Uri = Uri.parse(jwtUriPath)
    val token = uri.readAsText(context)

    return JsonWebToken(token)
}
