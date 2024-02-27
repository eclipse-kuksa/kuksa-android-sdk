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
import org.eclipse.kuksa.DisconnectListener;
import org.eclipse.kuksa.PropertyListener;
import org.eclipse.kuksa.VssSpecificationListener;
import org.eclipse.kuksa.authentication.JsonWebToken;
import org.eclipse.kuksa.model.Property;
import org.eclipse.kuksa.proto.v1.KuksaValV1;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.Types;
import org.eclipse.kuksa.proto.v1.Types.Datapoint;
import org.eclipse.kuksa.vss.VssVehicle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;

/**
 * @noinspection unused
 */
//@VssDefinition(vssDefinitionPath = "vss_rel_4.0.yaml") // Commented out to prevent conflicts with the Kotlin activity
public class JavaActivity extends AppCompatActivity {

    private final DisconnectListener disconnectListener = () -> {
        // connection closed manually or unexpectedly
    };

    @Nullable
    private DataBrokerConnection dataBrokerConnection = null;

    public void connectInsecure(String host, int port) {
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();

        // or jsonWebToken = null when authentication is disabled
        JsonWebToken jsonWebToken = new JsonWebToken("someValidToken");
        DataBrokerConnector connector = new DataBrokerConnector(managedChannel, jsonWebToken);
        connector.connect(new CoroutineCallback<DataBrokerConnection>() {
            @Override
            public void onSuccess(DataBrokerConnection result) {
                if (result == null) return;

                dataBrokerConnection = result;
                dataBrokerConnection.getDisconnectListeners().register(disconnectListener);
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

        // or jsonWebToken = null when authentication is disabled
        JsonWebToken jsonWebToken = new JsonWebToken("someValidToken");
        DataBrokerConnector connector = new DataBrokerConnector(managedChannel, jsonWebToken);
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

    public void disconnect() {
        if (dataBrokerConnection == null) return;

        dataBrokerConnection.getDisconnectListeners().unregister(disconnectListener);
        dataBrokerConnection.disconnect();
        dataBrokerConnection = null;
    }

    public void fetchProperty(Property property) {
        if (dataBrokerConnection == null) return;

        dataBrokerConnection.fetch(property, new CoroutineCallback<GetResponse>() {
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
        if (dataBrokerConnection == null) return;

        dataBrokerConnection.update(property, datapoint, new CoroutineCallback<KuksaValV1.SetResponse>() {
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

    public void subscribeProperty(Property property) {
        if (dataBrokerConnection == null) return;

        dataBrokerConnection.subscribe(property, new PropertyListener() {
            @Override
            public void onPropertyChanged(@NonNull List<KuksaValV1.EntryUpdate> entryUpdates) {
                for (KuksaValV1.EntryUpdate entryUpdate : entryUpdates) {
                    Types.DataEntry updatedValue = entryUpdate.getEntry();

                    // handle property change
                    //noinspection SwitchStatementWithTooFewBranches
                    switch (updatedValue.getPath()) {
                        case "VSS.Speed":
                            float speed = updatedValue.getValue().getFloat();

                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                // handle error
            }
        });
    }

    // region: Specifications
    public void fetchSpecification() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
        dataBrokerConnection.fetch(
            vssSpeed,
            Collections.singleton(Types.Field.FIELD_VALUE),
            new CoroutineCallback<VssVehicle.VssSpeed>() {
                @Override
                public void onSuccess(@Nullable VssVehicle.VssSpeed result) {
                    if (result == null) return;

                    Float speed = result.getValue();
                }

                @Override
                public void onError(@NonNull Throwable error) {
                    // handle error
                }
            }
        );
    }

    public void updateSpecification() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed(100f);
        dataBrokerConnection.update(
            vssSpeed,
            Collections.singleton(Types.Field.FIELD_VALUE),
            new CoroutineCallback<Collection<? extends KuksaValV1.SetResponse>>() {
                @Override
                public void onSuccess(@Nullable Collection<? extends KuksaValV1.SetResponse> result) {
                    // handle result
                }

                @Override
                public void onError(@NonNull Throwable error) {
                    // handle error
                }
            }
        );
    }

    public void subscribeSpecification() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
        dataBrokerConnection.subscribe(
            vssSpeed,
            Collections.singleton(Types.Field.FIELD_VALUE),
            new VssSpecificationListener<VssVehicle.VssSpeed>() {
                @Override
                public void onSpecificationChanged(@NonNull VssVehicle.VssSpeed vssSpecification) {
                    Float speed = vssSpecification.getValue();
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    // handle error
                }
            }
        );
    }
    // endregion
}
