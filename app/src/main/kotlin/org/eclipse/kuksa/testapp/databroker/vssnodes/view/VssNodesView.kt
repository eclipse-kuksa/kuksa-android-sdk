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
import androidx.compose.ui.platform.LocalFocusManager
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

private class VssNodeSuggestionAdapter(
    override val items: Collection<VssNode>,
    override val startingItem: VssNode,
) : SuggestionAdapter<VssNode> {
    override fun toString(item: VssNode): String {
        return item.vssPath
    }
}

@Composable
fun VssNodesView(viewModel: VssNodesViewModel) {
    val focusManager = LocalFocusManager.current

    Column {
        Headline(name = "Generated VSS Nodes")

        var currentNode = viewModel.node

        val isUpdatePossible = currentNode is VssSignal<*>
        val adapter = VssNodeSuggestionAdapter(viewModel.nodes, currentNode)

        SuggestionTextView(
            adapter = adapter,
            onItemSelected = {
                val vssNode = it ?: VssVehicle()
                viewModel.updateNode(vssNode)

                // Do an initial fetch of a VssSignal so the value reflects the actual one
                if (vssNode is VssSignal<*>) {
                    viewModel.onGetNode(vssNode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        VssNodeInformation(
            viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = DefaultEdgePadding, end = DefaultEdgePadding),
            onSignalChanged = { updatedSignal ->
                currentNode = updatedSignal
            },
        )
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onGetNode(currentNode)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(
                onClick = {
                    focusManager.clearFocus()
                    (currentNode as VssSignal<*>?)?.let { vssSignal ->
                        viewModel.onUpdateSignal(vssSignal)
                    }
                },
                enabled = isUpdatePossible,
                modifier = Modifier.requiredWidth(100.dp),
            ) {
                Text(text = "Update")
            }
            if (viewModel.isSubscribed) {
                Button(onClick = {
                    focusManager.clearFocus()
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
    viewModel: VssNodesViewModel,
    modifier: Modifier = Modifier,
    isShowingInformation: Boolean = false,
    onSignalChanged: (VssSignal<*>) -> Unit = {},
) {
    val vssNode = viewModel.node
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
                            VssSignalInformation(viewModel, onSignalChanged = onSignalChanged)
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
    viewModel: VssNodesViewModel,
    modifier: Modifier = Modifier,
    onSignalChanged: (VssSignal<*>) -> Unit = {},
) {
    val boldSpanStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val vssSignal = viewModel.node as VssSignal<*>

    var inputValue: String by remember(vssSignal, viewModel.updateCounter) {
        val value = if (vssSignal.value is Array<*>) {
            val valueArray = vssSignal.value as Array<*>
            valueArray.joinToString()
        } else {
            vssSignal.value.toString()
        }

        mutableStateOf(value)
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

            // Try and forget approach: Try to brute force the random input string into the VssSignal.
            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            try {
                val trimmedValue = newValue.trim()
                if (trimmedValue.isEmpty()) return@TextField

                val datapoint = vssSignal.valueCase.createDatapoint(trimmedValue)
                val updatedVssSignal = vssSignal.copy(datapoint)
                onSignalChanged(updatedVssSignal)
            } catch (e: Exception) {
                // Do nothing, wrong input
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
        VssNodesView(VssNodesViewModel())
    }
}

@Preview
@Composable
private fun NodeInformationPreview() {
    KuksaAppAndroidTheme {
        VssNodeInformation(VssNodesViewModel(), isShowingInformation = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun SignalInformationPreview() {
    KuksaAppAndroidTheme {
        val vssNodesViewModel = VssNodesViewModel()
        vssNodesViewModel.updateNode(VssHeartRate())
        VssNodeInformation(vssNodesViewModel, isShowingInformation = true)
    }
}
