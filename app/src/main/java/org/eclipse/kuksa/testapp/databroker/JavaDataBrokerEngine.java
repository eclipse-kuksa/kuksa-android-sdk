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

package org.eclipse.kuksa.testapp.databroker;

import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import org.eclipse.kuksa.CoroutineCallback;
import org.eclipse.kuksa.DataBrokerConnection;
import org.eclipse.kuksa.DataBrokerConnector;
import org.eclipse.kuksa.DisconnectListener;
import org.eclipse.kuksa.PropertyObserver;
import org.eclipse.kuksa.TimeoutConfig;
import org.eclipse.kuksa.VssSpecificationObserver;
import org.eclipse.kuksa.model.Property;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse;
import org.eclipse.kuksa.proto.v1.Types;
import org.eclipse.kuksa.proto.v1.Types.Datapoint;
import org.eclipse.kuksa.testapp.databroker.model.Certificate;
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo;
import org.eclipse.kuksa.testapp.extension.AssetManagerExtensionKt;
import org.eclipse.kuksa.vsscore.model.VssSpecification;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;

public class JavaDataBrokerEngine implements DataBrokerEngine {
    private static final String TAG = JavaDataBrokerEngine.class.getSimpleName();
    private static final long TIMEOUT_CONNECTION = 5;

    @NonNull
    private final AssetManager assetManager;

    @Nullable
    private DataBrokerConnection dataBrokerConnection = null;

    private final Set<DisconnectListener> disconnectListeners = new HashSet<>();

    public JavaDataBrokerEngine(@NonNull AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void connect(
        @NonNull ConnectionInfo connectionInfo,
        @NonNull CoroutineCallback<DataBrokerConnection> callback
    ) {
        if (connectionInfo.isTlsEnabled()) {
            connectSecure(connectionInfo, callback);
        } else {
            connectInsecure(connectionInfo, callback);
        }
    }

    private void connectInsecure(
        @NonNull ConnectionInfo connectInfo,
        @NonNull CoroutineCallback<DataBrokerConnection> callback
    ) {
        try {
            ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress(connectInfo.getHost(), connectInfo.getPort())
                .usePlaintext()
                .build();

            connect(managedChannel, callback);
        } catch (IllegalArgumentException e) {
            callback.onError(e);
        }
    }

    private void connectSecure(
        @NotNull ConnectionInfo connectInfo,
        @NotNull CoroutineCallback<DataBrokerConnection> callback
    ) {
        Certificate rootCert = connectInfo.getCertificate();

        ChannelCredentials tlsCredentials = null;
        try {
            InputStream rootCertFile = AssetManagerExtensionKt.open(assetManager, rootCert);
            tlsCredentials = TlsChannelCredentials.newBuilder()
                .trustManager(rootCertFile)
                .build();
        } catch (IOException e) {
            Log.w(TAG, "Could not find file for certificate: " + rootCert);
        }

        try {
            ManagedChannelBuilder<?> channelBuilder = Grpc
                .newChannelBuilderForAddress(connectInfo.getHost(), connectInfo.getPort(), tlsCredentials);

            String overrideAuthority = rootCert.getOverrideAuthority().trim();
            boolean hasOverrideAuthority = !overrideAuthority.isEmpty();
            if (hasOverrideAuthority) {
                channelBuilder.overrideAuthority(overrideAuthority);
            }

            ManagedChannel managedChannel = channelBuilder.build();
            connect(managedChannel, callback);
        } catch (IllegalArgumentException e) {
            callback.onError(e);
        }
    }

    private void connect(
        @NonNull ManagedChannel managedChannel,
        @NonNull CoroutineCallback<DataBrokerConnection> callback
    ) {
        DataBrokerConnector connector = new DataBrokerConnector(managedChannel);
        connector.setTimeoutConfig(new TimeoutConfig(TIMEOUT_CONNECTION, TimeUnit.SECONDS));
        connector.connect(new CoroutineCallback<>() {
            @Override
            public void onSuccess(@Nullable DataBrokerConnection result) {
                if (result == null) return;

                JavaDataBrokerEngine.this.dataBrokerConnection = result;
                for (DisconnectListener listener : disconnectListeners) {
                    result.getDisconnectListeners().register(listener);
                }

                callback.onSuccess(result);
            }

            @Override
            public void onError(@NonNull Throwable error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void fetch(@NonNull Property property, @NonNull CoroutineCallback<GetResponse> callback) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetch(property, callback);
    }

    @Override
    public <T extends VssSpecification> void fetch(
        @NonNull T specification,
        @NonNull CoroutineCallback<T> callback
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetch(specification, callback);
    }

    @Override
    public void update(
        @NonNull Property property,
        @NonNull Datapoint datapoint,
        @NonNull CoroutineCallback<SetResponse> callback
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.update(property, datapoint, callback);
    }

    @Override
    public void subscribe(@NonNull Property property, @NonNull PropertyObserver propertyObserver) {
        if (dataBrokerConnection == null) {
            return;
        }

        List<Property> properties = Collections.singletonList(property);
        dataBrokerConnection.subscribe(properties, propertyObserver);
    }

    @Override
    public <T extends VssSpecification> void subscribe(
        @NonNull T specification,
        @NonNull VssSpecificationObserver<T> propertyObserver
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        ArrayList<Types.Field> fields = new ArrayList<>() {
            {
                add(Types.Field.FIELD_VALUE);
                add(Types.Field.FIELD_METADATA);
            }
        };

        dataBrokerConnection.subscribe(specification, fields, propertyObserver);
    }

    public void disconnect() {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.disconnect();
        dataBrokerConnection = null;
    }

    @Nullable
    @Override
    public DataBrokerConnection getDataBrokerConnection() {
        return dataBrokerConnection;
    }

    @Override
    public void setDataBrokerConnection(@Nullable DataBrokerConnection dataBrokerConnection) {
        this.dataBrokerConnection = dataBrokerConnection;
    }

    @Override
    public void registerDisconnectListener(@NonNull DisconnectListener listener) {
        disconnectListeners.add(listener);
        if (dataBrokerConnection != null) {
            dataBrokerConnection.getDisconnectListeners().register(listener);
        }
    }

    @Override
    public void unregisterDisconnectListener(@NonNull DisconnectListener listener) {
        disconnectListeners.remove(listener);
        if (dataBrokerConnection != null) {
            dataBrokerConnection.getDisconnectListeners().unregister(listener);
        }
    }
}
