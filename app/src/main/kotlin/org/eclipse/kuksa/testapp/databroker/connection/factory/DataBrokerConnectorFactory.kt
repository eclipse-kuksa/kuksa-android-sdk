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

package org.eclipse.kuksa.testapp.databroker.connection.factory

import android.content.Context
import android.net.Uri
import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import org.eclipse.kuksa.connectivity.authentication.JsonWebToken
import org.eclipse.kuksa.connectivity.databroker.v1.DataBrokerConnector
import org.eclipse.kuksa.model.TimeoutConfig
import org.eclipse.kuksa.testapp.databroker.connection.model.ConnectionInfo
import org.eclipse.kuksa.testapp.extension.readAsText
import java.io.IOException
import java.util.concurrent.TimeUnit

class DataBrokerConnectorFactory {
    private val timeoutConfig = TimeoutConfig(5, TimeUnit.SECONDS)

    @Throws(IOException::class)
    fun create(context: Context, connectionInfo: ConnectionInfo): DataBrokerConnector {
        val managedChannel = if (connectionInfo.isTlsEnabled) {
            createSecureManagedChannel(context, connectionInfo)
        } else {
            createInsecureManagedChannel(connectionInfo)
        }

        var jsonWebToken: JsonWebToken? = null
        if (connectionInfo.isAuthenticationEnabled) {
            jsonWebToken = loadJsonWebToken(context, connectionInfo.jwtUriPath)
        }

        return DataBrokerConnector(managedChannel, jsonWebToken).apply {
            timeoutConfig = this@DataBrokerConnectorFactory.timeoutConfig
        }
    }

    private fun createInsecureManagedChannel(connectionInfo: ConnectionInfo): ManagedChannel {
        val host = connectionInfo.host.trim()
        val port = connectionInfo.port

        return ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()
    }

    @Throws(IOException::class)
    private fun createSecureManagedChannel(context: Context, connectionInfo: ConnectionInfo): ManagedChannel {
        val certificate = connectionInfo.certificate
        val rootCertFile = context.contentResolver.openInputStream(certificate.uri)

        val tlsCredentials: ChannelCredentials = TlsChannelCredentials.newBuilder()
            .trustManager(rootCertFile)
            .build()

        val host = connectionInfo.host.trim()
        val port = connectionInfo.port
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
    private fun loadJsonWebToken(context: Context, jwtUriPath: String?): JsonWebToken? {
        if (jwtUriPath.isNullOrEmpty()) {
            return null
        }

        val uri: Uri = Uri.parse(jwtUriPath)
        val token = uri.readAsText(context)

        return JsonWebToken(token)
    }
}
