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
 */

package org.eclipse.kuksa.testapp.databroker.vssnodes.view

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.eclipse.kuksa.extension.createDatapoint
import org.eclipse.kuksa.extension.valueCase
import org.eclipse.kuksa.extension.vss.copy
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.DefaultElementPadding
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionAdapter
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionTextView
import org.eclipse.kuksa.testapp.databroker.vssnodes.viewmodel.VssNodesViewModel
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vss.VssHeartRate
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal

private const val Tag = "DataBrokerVssNodesView"

@Composable
fun DataBrokerVssNodesView(viewModel: VssNodesViewModel) {
    Column {
        Headline(name = "Generated VSS Nodes")

        var currentVssSignal: VssSignal<*>? = null

        val currentNode = viewModel.node
        val isUpdatePossible = currentNode is VssSignal<*>
        val adapter = object : SuggestionAdapter<VssNode> {
            override fun toString(item: VssNode): String {
                return item.vssPath
            }
        }

        SuggestionTextView(
            value = "Vehicle",
            suggestions = viewModel.nodes,
            adapter = adapter,
            onItemSelected = {
                val vssNode = it ?: VssVehicle()
                viewModel.updateNode(vssNode)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        VssNodeInformation(
            currentNode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
            onSignalChanged = { updatedSignal ->
                currentVssSignal = updatedSignal
            },
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    viewModel.onGetNode(currentNode)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(
                onClick = {
                    currentVssSignal?.let {
                        viewModel.onUpdateSignal(it)
                    }
                },
                enabled = isUpdatePossible,
                modifier = Modifier.requiredWidth(100.dp),
            ) {
                Text(text = "Update")
            }
            if (viewModel.isSubscribed) {
                Button(onClick = {
                    viewModel.subscribedNodes.remove(currentNode)
                    viewModel.onUnsubscribeNode(currentNode)
                }) {
                    Text(text = "Unsubscribe")
                }
            } else {
                Button(onClick = {
                    viewModel.subscribedNodes.add(currentNode)
                    viewModel.onSubscribeNode(currentNode)
                }) {
                    Text(text = "Subscribe")
                }
            }
        }
    }
}

@Composable
private fun VssNodeInformation(
    vssNode: VssNode,
    modifier: Modifier = Modifier,
    isShowingInformation: Boolean = false,
    onSignalChanged: (VssSignal<*>) -> Unit = {},
) {
    val isVssSignal = vssNode is VssSignal<*>
    var isShowingNodeInformation by remember { mutableStateOf(isShowingInformation) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        OutlinedButton(onClick = { isShowingNodeInformation = !isShowingNodeInformation }) {
            Text(text = "Node Information")
        }
    }

    AnimatedContent(
        targetState = isShowingNodeInformation,
        label = "ShowingNodeInformationAnimation",
        modifier = modifier,
    ) {
        when (it) {
            true -> {
                Surface(color = MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(8.dp)) {
                    val boldSpanStyle = SpanStyle(fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.padding(top = DefaultElementPadding, bottom = DefaultElementPadding))
                    Column(modifier = Modifier.padding(all = DefaultElementPadding)) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = boldSpanStyle) {
                                    append("UUID: ")
                                }
                                append(vssNode.uuid)
                            },
                            maxLines = 2,
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = boldSpanStyle) {
                                    append("VSS Path: ")
                                }
                                append(vssNode.vssPath)
                            },
                            maxLines = 1,
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = boldSpanStyle) {
                                    append("Type: ")
                                }
                                append(vssNode.type)
                            },
                            maxLines = 1,
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = boldSpanStyle) {
                                    append("Description: ")
                                }
                                append(vssNode.description)
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isVssSignal) {
                            val vssSignal = vssNode as VssSignal<*>
                            VssSignalInformation(vssSignal, onSignalChanged = onSignalChanged)
                        }
                    }
                }
            }

            false -> {
                Surface {
                }
            }
        }
    }
}

@Composable
private fun VssSignalInformation(
    vssSignal: VssSignal<*>,
    modifier: Modifier = Modifier,
    onSignalChanged: (VssSignal<*>) -> Unit = {},
) {
    val boldSpanStyle = SpanStyle(fontWeight = FontWeight.Bold)

    var inputValue: String by remember(vssSignal) {
        mutableStateOf(vssSignal.value.toString())
    }

    Text(
        text = buildAnnotatedString {
            withStyle(style = boldSpanStyle) {
                append("Data Type: ")
            }
            append(vssSignal.dataType.simpleName)
        },
        maxLines = 1,
    )
    TextField(
        value = inputValue,
        onValueChange = { newValue ->
            inputValue = newValue

            @Suppress("TooGenericExceptionCaught")
            try {
                val datapoint = vssSignal.valueCase.createDatapoint(newValue.trim())
                val updatedVssSignal = vssSignal.copy(datapoint)
                onSignalChanged(updatedVssSignal)
            } catch (e: Exception) {
                // Do nothing, wrong input
                Log.v(Tag, "Wrong input for VssSignal:value: ", e)
            }
        },
        modifier = modifier
            .fillMaxWidth(),
        singleLine = true,
        label = {
            Text(text = "Value")
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    KuksaAppAndroidTheme {
        DataBrokerVssNodesView(VssNodesViewModel())
    }
}

@Preview
@Composable
private fun NodeInformationPreview() {
    KuksaAppAndroidTheme {
        VssNodeInformation(VssVehicle(), isShowingInformation = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun SignalInformationPreview() {
    KuksaAppAndroidTheme {
        VssNodeInformation(VssHeartRate(), isShowingInformation = true)
    }
}
