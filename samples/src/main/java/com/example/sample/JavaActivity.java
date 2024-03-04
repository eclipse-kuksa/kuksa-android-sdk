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

import org.eclipse.kuksa.connectivity.authentication.JsonWebToken;
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnection;
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnector;
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener;
import org.eclipse.kuksa.connectivity.databroker.listener.PropertyListener;
import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener;
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest;
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest;
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest;
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest;
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest;
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeUpdateRequest;
import org.eclipse.kuksa.coroutine.CoroutineCallback;
import org.eclipse.kuksa.proto.v1.KuksaValV1;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.Types;
import org.eclipse.kuksa.proto.v1.Types.Datapoint;
import org.eclipse.kuksa.vss.VssVehicle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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
//@VssModelGenerator // Commented out to prevent conflicts with the Kotlin activity
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

    public void fetchProperty() {
        if (dataBrokerConnection == null) return;

        FetchRequest request = new FetchRequest("Vehicle.Speed", Types.Field.FIELD_VALUE);
        dataBrokerConnection.fetch(request, new CoroutineCallback<GetResponse>() {
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

    public void updateProperty() {
        if (dataBrokerConnection == null) return;

        Datapoint datapoint = Datapoint.newBuilder()
            .setFloat(50f)
            .build();
        UpdateRequest request = new UpdateRequest("Vehicle.Speed", datapoint, Types.Field.FIELD_VALUE);
        dataBrokerConnection.update(request, new CoroutineCallback<KuksaValV1.SetResponse>() {
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

    public void subscribeProperty() {
        if (dataBrokerConnection == null) return;

        SubscribeRequest request = new SubscribeRequest("Vehicle.Speed", Types.Field.FIELD_VALUE);
        dataBrokerConnection.subscribe(request, new PropertyListener() {
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

    // region: VSS generated models
    public void fetchNode() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
        VssNodeFetchRequest<VssVehicle.VssSpeed> request = new VssNodeFetchRequest<>(
            vssSpeed,
            Types.Field.FIELD_VALUE
        );
        dataBrokerConnection.fetch(
            request,
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

    public void updateNode() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed(100f);
        VssNodeUpdateRequest<VssVehicle.VssSpeed> request = new VssNodeUpdateRequest<>(
            vssSpeed,
            Types.Field.FIELD_VALUE
        );
        dataBrokerConnection.update(
            request,
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

    public void subscribeNode() {
        if (dataBrokerConnection == null) return;

        VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
        VssNodeSubscribeRequest<VssVehicle.VssSpeed> request = new VssNodeSubscribeRequest<>(
            vssSpeed,
            Types.Field.FIELD_VALUE
        );
        dataBrokerConnection.subscribe(
            request,
            new VssNodeListener<VssVehicle.VssSpeed>() {
                @Override
                public void onNodeChanged(@NonNull VssVehicle.VssSpeed vssNode) {
                    Float speed = vssNode.getValue();
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
