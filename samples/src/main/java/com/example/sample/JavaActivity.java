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

package com.example.sample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.kuksa.CoroutineCallback;
import org.eclipse.kuksa.DataBrokerConnection;
import org.eclipse.kuksa.DataBrokerConnector;
import org.eclipse.kuksa.model.Property;
import org.eclipse.kuksa.proto.v1.KuksaValV1;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.Types.Datapoint;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;

public class JavaActivity extends AppCompatActivity {
    @Nullable
    private DataBrokerConnection dataBrokerConnection = null;

    public void connectInsecure(String host, int port) {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        DataBrokerConnector connector = new DataBrokerConnector(managedChannel);
        connector.connect(new CoroutineCallback<DataBrokerConnection>() {
            @Override
            public void onSuccess(DataBrokerConnection result) {
                dataBrokerConnection = result;

                // handle result
            }

            @Override
            public void onError(@NonNull Throwable error) {
                // handle error
            }
        });
    }

    public void connectSecure(String host, int port, String overrideAuthority) {
        ChannelCredentials tlsCredentials = null;
        try {
            InputStream rootCertFile = getAssets().open("CA.pem");
            tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(rootCertFile)
                .build();
        } catch (IOException e) {
            // handle error
        }

        ManagedChannelBuilder<?> channelBuilder = Grpc
            .newChannelBuilderForAddress(host, port, tlsCredentials);

        boolean hasOverrideAuthority = !overrideAuthority.isEmpty();
        if (hasOverrideAuthority) {
            channelBuilder.overrideAuthority(overrideAuthority);
        }

        ManagedChannel managedChannel = channelBuilder.build();
        DataBrokerConnector connector = new DataBrokerConnector(managedChannel);
        connector.connect(new CoroutineCallback<DataBrokerConnection>() {
            @Override
            public void onSuccess(DataBrokerConnection result) {
                dataBrokerConnection = result;

                // handle result
            }

            @Override
            public void onError(@NonNull Throwable error) {
                // handle error
            }
        });
    }

    public void fetchProperty(Property property) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetchProperty(property, new CoroutineCallback<GetResponse>() {
            @Override
            public void onSuccess(GetResponse result) {
                // handle result
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                // handle error
            }
        });
    }

    public void updateProperty(Property property, Datapoint datapoint) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.updateProperty(property, datapoint, new CoroutineCallback<KuksaValV1.SetResponse>() {
            @Override
            public void onSuccess(KuksaValV1.SetResponse result) {
                // handle result
            }

            @Override
            public void onError(@NonNull Throwable error) {
                // handle error
            }
        });

    }
}
