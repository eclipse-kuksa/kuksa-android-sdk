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

import org.eclipse.kuksa.CoroutineCallback;
import org.eclipse.kuksa.DataBrokerConnection;
import org.eclipse.kuksa.DataBrokerConnector;
import org.eclipse.kuksa.DisconnectListener;
import org.eclipse.kuksa.PropertyListener;
import org.eclipse.kuksa.VssNodeListener;
import org.eclipse.kuksa.model.Property;
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse;
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse;
import org.eclipse.kuksa.proto.v1.Types;
import org.eclipse.kuksa.proto.v1.Types.Datapoint;
import org.eclipse.kuksa.testapp.databroker.connection.DataBrokerConnectorFactory;
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo;
import org.eclipse.kuksa.vsscore.model.VssNode;

import java.util.ArrayList;
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
    public void fetch(@NonNull Property property, @NonNull CoroutineCallback<GetResponse> callback) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.fetch(property, callback);
    }

    @Override
    public <T extends VssNode> void fetch(
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
    public void subscribe(@NonNull Property property, @NonNull PropertyListener propertyListener) {
        if (dataBrokerConnection == null) {
            return;
        }

        dataBrokerConnection.subscribe(property, propertyListener);
    }

    @Override
    public <T extends VssNode> void subscribe(
        @NonNull T specification,
        @NonNull VssNodeListener<T> specificationListener
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        ArrayList<Types.Field> fields = new ArrayList<>() {
            {
                add(Types.Field.FIELD_VALUE);
            }
        };

        dataBrokerConnection.subscribe(specification, fields, specificationListener);
    }

    @Override
    public <T extends VssNode> void unsubscribe(
        @NonNull T specification,
        @NonNull VssNodeListener<T> specificationListener
    ) {
        if (dataBrokerConnection == null) {
            return;
        }

        ArrayList<Types.Field> fields = new ArrayList<>() {
            {
                add(Types.Field.FIELD_VALUE);
            }
        };

        dataBrokerConnection.unsubscribe(specification, fields, specificationListener);
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
    public void unsubscribe(@NonNull Property property, @NonNull PropertyListener propertyListener) {
        if (dataBrokerConnection != null) {
            dataBrokerConnection.unsubscribe(property, propertyListener);
        }
    }
}
