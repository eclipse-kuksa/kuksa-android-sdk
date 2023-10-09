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

package org.eclipse.kuksa.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DisconnectListener
import org.eclipse.kuksa.PropertyObserver
import org.eclipse.kuksa.extension.metadata
import org.eclipse.kuksa.model.Property
import org.eclipse.kuksa.proto.v1.KuksaValV1.GetResponse
import org.eclipse.kuksa.proto.v1.KuksaValV1.SetResponse
import org.eclipse.kuksa.proto.v1.Types
import org.eclipse.kuksa.proto.v1.Types.Datapoint
import org.eclipse.kuksa.proto.v1.Types.Datapoint.ValueCase
import org.eclipse.kuksa.testapp.databroker.DataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.JavaDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.KotlinDataBrokerEngine
import org.eclipse.kuksa.testapp.databroker.view.DataBrokerView
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel.ConnectionViewState
import org.eclipse.kuksa.testapp.databroker.viewmodel.OutputViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.TopAppBarViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.VSSPropertiesViewModel
import org.eclipse.kuksa.testapp.extension.TAG
import org.eclipse.kuksa.testapp.extension.valueType
import org.eclipse.kuksa.testapp.model.ConnectionInfo
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme

class KuksaDataBrokerActivity : ComponentActivity() {
    private val topAppBarViewModel: TopAppBarViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val vssPropertiesViewModel: VSSPropertiesViewModel by viewModels()
    private val outputViewModel: OutputViewModel by viewModels()

    private val dataBrokerConnectionCallback = object : CoroutineCallback<DataBrokerConnection>() {
        override fun onSuccess(result: DataBrokerConnection?) {
            outputViewModel.appendOutput("Connection to DataBroker successful established")
            connectionViewModel.updateConnectionState(ConnectionViewState.CONNECTED)
        }

        override fun onError(error: Throwable) {
            outputViewModel.appendOutput("Connection to DataBroker failed: ${error.message}")
            connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
        }
    }

    private val onDisconnectListener = DisconnectListener {
        connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
        outputViewModel.clear()
        outputViewModel.appendOutput("DataBroker disconnected")
    }

    private lateinit var dataBrokerEngine: DataBrokerEngine
    private val kotlinDataBrokerEngine by lazy {
        KotlinDataBrokerEngine(lifecycleScope, assets)
    }

    private val javaDataBrokerEngine by lazy {
        JavaDataBrokerEngine(assets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate() called with: savedInstanceState = $savedInstanceState")

        setContent {
            KuksaAppAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DataBrokerView(topAppBarViewModel, connectionViewModel, vssPropertiesViewModel, outputViewModel)
                }
            }
        }

        dataBrokerEngine = kotlinDataBrokerEngine
        topAppBarViewModel.onCompatibilityModeChanged = { isCompatibilityModeEnabled ->
            dataBrokerEngine = if (isCompatibilityModeEnabled) {
                javaDataBrokerEngine
            } else {
                kotlinDataBrokerEngine
            }
            val enabledState = if (isCompatibilityModeEnabled) "enabled" else "disabled"
            outputViewModel.appendOutput("Java Compatibility Mode $enabledState")
        }

        connectionViewModel.onConnect = { connectionInfo ->
            connect(connectionInfo)
        }

        connectionViewModel.onDisconnect = {
            disconnect()
        }

        vssPropertiesViewModel.onGetProperty = { property: Property ->
            fetchProperty(property)
        }

        vssPropertiesViewModel.onSetProperty = { property: Property, datapoint: Datapoint ->
            updateProperty(property, datapoint)
        }

        vssPropertiesViewModel.onSubscribeProperty = { property: Property ->
            subscribeProperty(property)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        dataBrokerEngine.registerDisconnectListener(onDisconnectListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        dataBrokerEngine.unregisterDisconnectListener(onDisconnectListener)
    }

    private fun connect(connectionInfo: ConnectionInfo) {
        Log.d(TAG, "Connecting to DataBroker: $connectionInfo")

        outputViewModel.appendOutput("Connecting to data broker - Please wait")
        connectionViewModel.updateConnectionState(ConnectionViewState.CONNECTING)

        dataBrokerEngine.connect(connectionInfo, dataBrokerConnectionCallback)
    }

    private fun disconnect() {
        Log.d(TAG, "Disconnecting from DataBroker")
        dataBrokerEngine.disconnect()
        outputViewModel.clear()
        connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
    }

    private fun fetchProperty(property: Property) {
        Log.d(TAG, "Fetch property: $property")

        dataBrokerEngine.fetchProperty(
            property,
            object : CoroutineCallback<GetResponse>() {
                override fun onSuccess(result: GetResponse?) {
                    val automaticValueType = result?.metadata?.valueType ?: ValueCase.VALUE_NOT_SET
                    Log.d(TAG, "Fetched automatic value type from meta data: $automaticValueType")

                    val errorsList = result?.errorsList
                    errorsList?.forEach {
                        outputViewModel.appendOutput(it.toString())
                        return
                    }

                    val vssProperties = vssPropertiesViewModel.vssProperties
                        .copy(valueType = automaticValueType)
                    vssPropertiesViewModel.updateVssProperties(vssProperties)

                    outputViewModel.appendOutput(result.toString())
                }

                override fun onError(error: Throwable) {
                    outputViewModel.appendOutput("Connection to data broker failed: ${error.message}")
                }
            },
        )
    }

    private fun updateProperty(property: Property, datapoint: Datapoint) {
        Log.d(TAG, "Update property: $property dataPoint: $datapoint, type: ${datapoint.valueCase}")
        dataBrokerEngine.updateProperty(
            property,
            datapoint,
            object : CoroutineCallback<SetResponse>() {
                override fun onSuccess(result: SetResponse?) {
                    val errorsList = result?.errorsList
                    errorsList?.forEach {
                        outputViewModel.appendOutput(it.toString())
                        return
                    }

                    outputViewModel.appendOutput(result.toString())
                }

                override fun onError(error: Throwable) {
                    outputViewModel.appendOutput("Connection to data broker failed: ${error.message}")
                }
            },
        )
    }

    private fun subscribeProperty(property: Property) {
        dataBrokerEngine.subscribe(
            property,
            object : PropertyObserver {
                override fun onPropertyChanged(vssPath: String, updatedValue: Types.DataEntry) {
                    Log.d(TAG, "onPropertyChanged path: vssPath = $vssPath, changedValue = $updatedValue")
                    outputViewModel.appendOutput(updatedValue.toString())
                }
            },
        )
    }
}
