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
import kotlinx.coroutines.launch
import org.eclipse.kuksa.CoroutineCallback
import org.eclipse.kuksa.DataBrokerConnection
import org.eclipse.kuksa.DataBrokerException
import org.eclipse.kuksa.extension.metadata
import org.eclipse.kuksa.extension.valueType
import org.eclipse.kuksa.model.Property
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
import org.eclipse.kuksa.testapp.databroker.viewmodel.VssSpecificationsViewModel
import org.eclipse.kuksa.testapp.extension.TAG
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vsscore.model.annotation.VssDefinition
import org.eclipse.kuksa.vsscore.model.model.VssSpecification

@VssDefinition("vss_rel_4.0.yaml")
class KuksaDataBrokerActivity : ComponentActivity() {
    private val topAppBarViewModel: TopAppBarViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val vssPropertiesViewModel: VSSPropertiesViewModel by viewModels()
    private val vssSpecificationsViewModel: VssSpecificationsViewModel by viewModels()
    private val outputViewModel: OutputViewModel by viewModels()

    private val dataBrokerConnectionCallback = object : CoroutineCallback<DataBrokerConnection>() {
        override fun onSuccess(result: DataBrokerConnection?) {
            outputViewModel.appendOutput("Connection to data broker was successful")
            connectionViewModel.updateConnectionState(ConnectionViewState.CONNECTED)
        }

        override fun onError(error: Throwable) {
            outputViewModel.appendOutput("Connection to data broker failed: ${error.message}")
            connectionViewModel.updateConnectionState(ConnectionViewState.DISCONNECTED)
        }
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
                    DataBrokerView(
                        topAppBarViewModel,
                        connectionViewModel,
                        vssPropertiesViewModel,
                        vssSpecificationsViewModel,
                        outputViewModel,
                    )
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

        vssSpecificationsViewModel.onSubscribeSpecification = { specification ->
            subscribeSpecification(specification)
        }

        vssSpecificationsViewModel.onGetSpecification = { specification ->
            fetchSpecification(specification)
        }
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

        lifecycleScope.launch {
            try {
                val result = dataBrokerEngine.fetchProperty(property)

                val automaticValueType = result?.metadata?.valueType ?: ValueCase.VALUE_NOT_SET
                Log.d(TAG, "Fetched automatic value type from meta data: $automaticValueType")

                val errorsList = result?.errorsList
                errorsList?.forEach {
                    outputViewModel.appendOutput(it.toString())
                    return@launch
                }

                val vssProperties = vssPropertiesViewModel.vssProperties
                    .copy(valueType = automaticValueType)
                vssPropertiesViewModel.updateVssProperties(vssProperties)

                outputViewModel.appendOutput(result.toString())
            } catch (error: DataBrokerException) {
                outputViewModel.appendOutput("Connection to data broker failed: ${error.message}")
            }
        }
    }

    private fun updateProperty(property: Property, datapoint: Datapoint) {
        Log.d(TAG, "Update property: $property dataPoint: $datapoint, type: ${datapoint.valueCase}")

        lifecycleScope.launch {
            try {
                val result = dataBrokerEngine.updateProperty(property, datapoint)

                val errorsList = result?.errorsList
                errorsList?.forEach {
                    outputViewModel.appendOutput(it.toString())
                    return@launch
                }

                outputViewModel.appendOutput(result.toString())
            } catch (error: DataBrokerException) {
                outputViewModel.appendOutput("Connection to data broker failed: ${error.message}")
            }
        }
    }

    private fun subscribeProperty(property: Property) {
        Log.d(TAG, "Subscribing to property: $property")
        dataBrokerEngine.subscribe(property) { vssPath, updatedValue ->
            Log.d(TAG, "onPropertyChanged path: vssPath = $vssPath, changedValue = $updatedValue")
            outputViewModel.appendOutput("Updated value: $updatedValue")
        }
    }

    private fun fetchSpecification(specification: VssSpecification) {
        lifecycleScope.launch {
            try {
                val updatedSpecification = dataBrokerEngine.fetchSpecification(specification)
                Log.d(TAG, "Fetched specification: $updatedSpecification")
                outputViewModel.appendOutput("Fetched specification: $updatedSpecification")
            } catch (error: DataBrokerException) {
                outputViewModel.appendOutput("Could not fetch specification: ${error.message}")
            }
        }
    }

    private fun subscribeSpecification(specification: VssSpecification) {
        dataBrokerEngine.subscribe(specification) { updatedSpecification ->
            outputViewModel.appendOutput("Updated specification: $updatedSpecification")
        }
    }
}
