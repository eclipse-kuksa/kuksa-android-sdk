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

package org.eclipse.kuksa.test.databroker

import io.grpc.ChannelCredentials
import io.grpc.Grpc
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import org.eclipse.kuksa.DataBrokerConnector
import org.eclipse.kuksa.TimeoutConfig
import java.io.IOException
import java.io.InputStream

class DataBrokerConnectorProvider {
    fun createInsecure(
        host: String = DataBrokerConfig.HOST,
        port: Int = DataBrokerConfig.PORT,
    ): DataBrokerConnector {
        val managedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()

        return DataBrokerConnector(managedChannel).apply {
            timeoutConfig = TimeoutConfig(DataBrokerConfig.TIMEOUT_SECONDS, DataBrokerConfig.TIMEOUT_UNIT)
        }
    }

    fun createSecure(
        host: String = DataBrokerConfig.HOST,
        port: Int = DataBrokerConfig.PORT,
        rootCertFileStream: InputStream,
        overrideAuthority: String = "",
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

        val managedChannel = channelBuilder.build()
        return DataBrokerConnector(managedChannel).apply {
            timeoutConfig = TimeoutConfig(DataBrokerConfig.TIMEOUT_SECONDS, DataBrokerConfig.TIMEOUT_UNIT)
        }
    }
}
