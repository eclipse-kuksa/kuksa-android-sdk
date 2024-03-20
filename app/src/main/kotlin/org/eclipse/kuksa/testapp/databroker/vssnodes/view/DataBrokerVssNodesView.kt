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
import androidx.compose.material3.Button
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.DefaultElementPadding
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionAdapter
import org.eclipse.kuksa.testapp.databroker.view.suggestions.SuggestionTextView
import org.eclipse.kuksa.testapp.databroker.vssnodes.viewmodel.VssNodesViewModel
import org.eclipse.kuksa.testapp.extension.compose.Headline
import org.eclipse.kuksa.testapp.ui.theme.KuksaAppAndroidTheme
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.model.VssNode
import org.eclipse.kuksa.vsscore.model.VssSignal

@Composable
fun DataBrokerVssNodesView(viewModel: VssNodesViewModel) {
    Column {
        Headline(name = "Generated VSS Nodes")

        val isUpdatePossible = viewModel.node is VssSignal<*>
        val adapter = object : SuggestionAdapter<VssNode> {
            override fun toString(item: VssNode): String {
                return item.vssPath
            }
        }

        SuggestionTextView(
            value = "Vehicle",
            suggestions = viewModel.vssNodes,
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
        Spacer(modifier = Modifier.padding(top = DefaultElementPadding))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    viewModel.onGetNode(viewModel.node)
                },
                modifier = Modifier.requiredWidth(80.dp),
            ) {
                Text(text = "Get")
            }
            Button(
                onClick = {
                    viewModel.onUpdateNode(viewModel.node)
                },
                enabled = isUpdatePossible,
                modifier = Modifier.requiredWidth(100.dp),
            ) {
                Text(text = "Update")
            }
            if (viewModel.isSubscribed) {
                Button(onClick = {
                    viewModel.subscribedNodes.remove(viewModel.node)
                    viewModel.onUnsubscribeNode(viewModel.node)
                }) {
                    Text(text = "Unsubscribe")
                }
            } else {
                Button(onClick = {
                    viewModel.subscribedNodes.add(viewModel.node)
                    viewModel.onSubscribeNode(viewModel.node)
                }) {
                    Text(text = "Subscribe")
                }
            }
        }
    }
}

