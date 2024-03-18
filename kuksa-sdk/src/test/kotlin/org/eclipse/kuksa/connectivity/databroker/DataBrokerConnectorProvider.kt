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

import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import org.eclipse.kuksa.connectivity.authentication.JsonWebToken
import org.eclipse.kuksa.connectivity.authentication.JwtType
import org.eclipse.kuksa.model.TimeoutConfig
import org.eclipse.kuksa.test.TestResourceFile
import java.io.IOException
import java.io.InputStream

class DataBrokerConnectorProvider {
    lateinit var managedChannel: ManagedChannel
    fun createInsecure(
        host: String = DATABROKER_HOST,
        port: Int = DATABROKER_PORT,
        jwtFileStream: InputStream? = null,
    ): DataBrokerConnector {
        managedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

        val jsonWebToken = jwtFileStream?.let {
            val token = it.reader().readText()
            JsonWebToken(token)
        }

        return DataBrokerConnector(
            managedChannel,
            jsonWebToken,
        ).apply {
            timeoutConfig = TimeoutConfig(DATABROKER_TIMEOUT_SECONDS, DATABROKER_TIMEOUT_UNIT)
        }
    }

    fun createSecure(
        host: String = DATABROKER_HOST,
        port: Int = DATABROKER_PORT,
        overrideAuthority: String = "",
        rootCertFileStream: InputStream = TestResourceFile("tls/CA.pem").inputStream(),
        jwtFileStream: InputStream? = JwtType.READ_WRITE_ALL.asInputStream(),
    ): DataBrokerConnector {
        val tlsCredentials: ChannelCredentials
        try {
            tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(rootCertFileStream)
                .build()
        } catch (_: IOException) {
            // Handle error
            throw IOException("Could not create TLS credentials")
        }

        val channelBuilder = Grpc
            .newChannelBuilderForAddress(host, port, tlsCredentials)

        val hasOverrideAuthority = overrideAuthority.isNotEmpty()
        if (hasOverrideAuthority) {
            channelBuilder.overrideAuthority(overrideAuthority)
        }

        managedChannel = channelBuilder.build()

        val jsonWebToken = jwtFileStream?.let {
            val token = it.reader().readText()
            JsonWebToken(token)
        }

        return DataBrokerConnector(
            managedChannel,
            jsonWebToken,
        ).apply {
            timeoutConfig = TimeoutConfig(DATABROKER_TIMEOUT_SECONDS, DATABROKER_TIMEOUT_UNIT)
        }
    }
}
