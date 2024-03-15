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

import android.content.Context;

import androidx.annotation.NonNull;

import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnection;
import org.eclipse.kuksa.connectivity.databroker.DataBrokerConnector;
import org.eclipse.kuksa.connectivity.databroker.listener.DisconnectListener;
import org.eclipse.kuksa.connectivity.databroker.listener.VssNodeListener;
import org.eclipse.kuksa.connectivity.databroker.listener.VssPathListener;
import org.eclipse.kuksa.connectivity.databroker.request.FetchRequest;
import org.eclipse.kuksa.connectivity.databroker.request.SubscribeRequest;
import org.eclipse.kuksa.connectivity.databroker.request.UpdateRequest;
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeFetchRequest;
import org.eclipse.kuksa.connectivity.databroker.request.VssNodeSubscribeRequest;
import org.eclipse.kuksa.coroutine.CoroutineCallback;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse;
import org.eclipse.kuksa.testapp.databroker.connection.DataBrokerConnectorFactory;
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo;
import org.eclipse.kuksa.vsscore.model.VssNode;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public class JavaDataBrokerEngine implements DataBrokerEngine {
    @Nullable
    private DataBrokerConnection dataBrokerConnection = null;

    private final DataBrokerConnectorFactory connectorFactory = new DataBrokerConnectorFactory();
    private final Set<DisconnectListener> disconnectListeners = new HashSet<>();

    // Too many to usefully handle: Checked Exceptions: IOE, RuntimeExceptions: UOE, ISE, IAE, ...
    @SuppressWarnings("TooGenericExceptionCaught")
    public void connect(
        @NonNull Context context,
        @NonNull ConnectionInfo connectionInfo,
        @NonNull CoroutineCallback<DataBrokerConnection> callback
    ) {
        try {
            DataBrokerConnector connector = connectorFactory.create(context, connectionInfo);
            connect(connector, callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private void connect(
        @NonNull DataBrokerConnector connector,
        @NonNull CoroutineCallback<DataBrokerConnection> callback
    ) {
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
    public void fetch(@NonNull FetchRequest request, @NonNull CoroutineCallback<GetResponse> callback) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetch(request, callback);
    }

    @Override
    public <T extends VssNode> void fetch(
        @NonNull VssNodeFetchRequest<T> request,
        @NonNull CoroutineCallback<T> callback
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetch(request, callback);
    }

    @Override
    public void update(
        @NonNull UpdateRequest request,
        @NonNull CoroutineCallback<SetResponse> callback
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.update(request, callback);
    }

    @Override
    public void subscribe(@NonNull SubscribeRequest request, @NonNull VssPathListener listener) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.subscribe(request, listener);
    }

    @Override
    public <T extends VssNode> void subscribe(
        @NonNull VssNodeSubscribeRequest<T> request,
        @NonNull VssNodeListener<T> vssNodeListener
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.subscribe(request, vssNodeListener);
    }

    @Override
    public <T extends VssNode> void unsubscribe(
        @NonNull VssNodeSubscribeRequest<T> request,
        @NonNull VssNodeListener<T> vssNodeListener
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.unsubscribe(request, vssNodeListener);
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

    @Override
    public void unsubscribe(@NonNull SubscribeRequest request, @NonNull VssPathListener listener) {
        if (dataBrokerConnection != null) {
            dataBrokerConnection.unsubscribe(request, listener);
        }
    }
}
