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

package org.eclipse.kuksa.testapp.databroker.connection.view.connection

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import org.eclipse.kuksa.testapp.databroker.connection.model.ConnectionInfo
import org.eclipse.kuksa.testapp.databroker.view.DefaultEdgePadding
import org.eclipse.kuksa.testapp.databroker.view.FileSelectorSettingView
import org.eclipse.kuksa.testapp.extension.fetchFileName

@Composable
fun AuthenticationOptionsView(
    connectionInfo: ConnectionInfo,
    modifier: Modifier = Modifier,
    onAuthenticationStateChanged: (Boolean) -> Unit = {},
    onJwtSelected: (Uri) -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(start = DefaultEdgePadding, end = DefaultEdgePadding)) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onAuthenticationStateChanged(!connectionInfo.isAuthenticationEnabled)
                },
        ) {
            val (text, checkbox) = createRefs()
            Text(
                text = "Authentication",
                modifier = Modifier.constrainAs(text) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            )
            Checkbox(
                checked = connectionInfo.isAuthenticationEnabled,
                onCheckedChange = onAuthenticationStateChanged,
                modifier = Modifier
                    .constrainAs(checkbox) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)

                        end.linkTo(parent.end)
                    },
            )
        }

        if (connectionInfo.isAuthenticationEnabled) {
            val uri = Uri.parse(connectionInfo.jwtUriPath ?: "")
            val fileName = uri.fetchFileName(context) ?: "Select JWT..."

            FileSelectorSettingView(
                label = "JWT",
                value = fileName,
                onResult = onJwtSelected,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
    }
}

@Preview
@Composable
private fun DisabledTlsOptionsViewPreview() {
    val connectionInfo = ConnectionInfo(isAuthenticationEnabled = false)

    Surface {
        AuthenticationOptionsView(connectionInfo)
    }
}

@Preview
@Composable
private fun EnabledTlsOptionsViewPreview() {
    val connectionInfo = ConnectionInfo(isAuthenticationEnabled = true)

    Surface {
        AuthenticationOptionsView(connectionInfo)
    }
}
