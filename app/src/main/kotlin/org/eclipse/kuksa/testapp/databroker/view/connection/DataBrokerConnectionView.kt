/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.kuksa.testapp.databroker.view.connection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.eclipse.kuksa.testapp.databroker.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.DefaultElementPadding
import org.eclipse.kuksa.testapp.databroker.view.MinimumButtonWidth
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel
import org.eclipse.kuksa.testapp.databroker.viewmodel.ConnectionViewModel.ConnectionViewState
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.extension.compose.RememberCountdown
import org.eclipse.kuksa.testapp.preferences.ConnectionInfoRepository

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DataBrokerConnectionView(viewModel: ConnectionViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val repositoryConnectionInfoState =
        viewModel.connectionInfoFlow.collectAsStateWithLifecycle(initialValue = ConnectionInfo())

    val connectionInfoState = remember(repositoryConnectionInfoState.value) {
        mutableStateOf(repositoryConnectionInfoState.value)
    }
    val connectionInfo = connectionInfoState.value

    Column {
        Headline("Connection")

        AnimatedVisibility(visible = viewModel.isDisconnected) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextField(
                        value = connectionInfo.host,
                        onValueChange = {
                            val newConnectionInfo = repositoryConnectionInfoState.value.copy(host = it)
                            connectionInfoState.value = newConnectionInfo
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.updateConnectionInfo(connectionInfo)
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier
                            .weight(2f),
                        singleLine = true,
                        label = {
                            Text(text = "Host")
                        },
                    )
                    Text(
                        text = ":",
                        modifier = Modifier
                            .padding(start = 5.dp, end = 5.dp)
                            .align(Alignment.CenterVertically),
                    )
                    TextField(
                        value = connectionInfo.port.toString(),
                        onValueChange = { value ->
                            try {
                                val port = value.toInt()
                                val newConnectionInfo = connectionInfo.copy(port = port)
                                connectionInfoState.value = newConnectionInfo
                            } catch (e: NumberFormatException) {
                                // ignore gracefully
                            }
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.updateConnectionInfo(connectionInfo)
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier
                            .weight(1f),
                        singleLine = true,
                        label = {
                            Text(text = "Port")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
                TlsOptionsView(
                    connectionInfo = connectionInfo,
                    onTlsStateChanged = { isTlsEnabled ->
                        val newConnectionInfo = connectionInfo.copy(isTlsEnabled = isTlsEnabled)
                        viewModel.updateConnectionInfo(newConnectionInfo)
                    },
                    onCertificateSelected = { certificateUri ->
                        val newCertificate = connectionInfo.certificate.copy(uriPath = certificateUri.toString())
                        val newConnectionInfo = connectionInfo.copy(certificate = newCertificate)
                        viewModel.updateConnectionInfo(newConnectionInfo)
                    },
                    onAuthorityOverrideChanged = { overrideAuthorityValue ->
                        val certificate = connectionInfo.certificate.copy(overrideAuthority = overrideAuthorityValue)
                        val newConnectionInfo = connectionInfo.copy(certificate = certificate)
                        connectionInfoState.value = newConnectionInfo
                    },
                    onKeyboardDone = {
                        viewModel.updateConnectionInfo(connectionInfo)
                        keyboardController?.hide()
                    },
                )

                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))

                AuthenticationOptionsView(
                    connectionInfo = connectionInfo,
                    onAuthenticationStateChanged = { isAuthenticationEnabled ->
                        val newConnectionInfo =
                            connectionInfo.copy(isAuthenticationEnabled = isAuthenticationEnabled)
                        viewModel.updateConnectionInfo(newConnectionInfo)
                    },
                    onJwtSelected = { jwtUri ->
                        val newConnectionInfo = connectionInfo.copy(jwtUriPath = jwtUri.toString())
                        viewModel.updateConnectionInfo(newConnectionInfo)
                    },
                )

                Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AnimatedContent(
                targetState = viewModel.connectionViewState,
                label = "ConnectAnimation",
            ) { connectionViewState ->
                when (connectionViewState) {
                    ConnectionViewState.DISCONNECTED ->
                        Button(
                            onClick = {
                                viewModel.onConnect(connectionInfo)
                                viewModel.updateConnectionInfo(connectionInfo)

                                keyboardController?.hide()
                            },
                            modifier = Modifier.width(MinimumButtonWidth),
                        ) {
                            Text(text = "Connect", textAlign = TextAlign.Center)
                        }

                    ConnectionViewState.CONNECTING ->
                        Button(
                            onClick = { },
                            modifier = Modifier.requiredWidth(MinimumButtonWidth),
                        ) {
                            val timeout by RememberCountdown(initialMillis = viewModel.connectionTimeoutMillis)

                            @Suppress("MagicNumber") // To seconds
                            val timeoutSeconds = timeout / 1000
                            Text(text = "Connecting... ($timeoutSeconds)", textAlign = TextAlign.Center)
                        }

                    ConnectionViewState.CONNECTED -> {
                        // intentionally left empty
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConnectedPreview() {
    val connectionInfoRepository = ConnectionInfoRepository(LocalContext.current)
    val viewModel = ConnectionViewModel(connectionInfoRepository)
    Surface {
        DataBrokerConnectionView(viewModel = viewModel)
    }
}

@Preview
@Composable
private fun DisconnectedPreview() {
    val connectionInfoRepository = ConnectionInfoRepository(LocalContext.current)
    val viewModel = ConnectionViewModel(connectionInfoRepository)
    viewModel.updateConnectionState(ConnectionViewState.CONNECTING)
    Surface {
        DataBrokerConnectionView(viewModel = viewModel)
    }
}
