/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.about

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.abysner_logo
import abysner.composeapp.generated.resources.ic_github
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.version.VersionInfo


typealias AboutScreen = @Composable (navController: NavHostController) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun AboutScreen(
    @Assisted navController: NavHostController = rememberNavController()
) {
    val uriHandler = LocalUriHandler.current

    AbysnerTheme {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    TopAppBar(
                        title = { Text("About") },
                        navigationIcon = {

                            val currentBackStackEntry by navController.currentBackStackEntryAsState()
                            if (currentBackStackEntry != null || LocalInspectionMode.current) {
                                IconButton(onClick = {
                                    navController.navigateUp()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                uriHandler.openUri("https://github.com/NeoTech-Software/Abysner")
                            }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_github),
                                    contentDescription = "GitHub"
                                )
                            }
                        }
                    )
                }
            }
        ) { insets ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                val isCompactHeight = maxHeight < 500.dp

                Box(
                    Modifier
                        .padding(top = insets.calculateTopPadding())
                        .fillMaxSize()
                        .waveBackground(),
                    contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.6f),
                ) {
                    AboutScreenContent(
                        insets = insets,
                        isLandscape = isLandscape,
                        isCompactHeight = isCompactHeight,
                    )
                }

                val layoutDirection = LocalLayoutDirection.current
                CopyrightFooter(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = insets.calculateStartPadding(layoutDirection),
                            end = insets.calculateEndPadding(layoutDirection),
                            bottom = insets.calculateBottomPadding() + 8.dp,
                        ),
                    navController = navController,
                    singleLine = isLandscape,
                )
            }
        }
    }
}

@Composable
private fun AboutScreenContent(
    insets: PaddingValues,
    isLandscape: Boolean,
    isCompactHeight: Boolean
) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = insets.calculateStartPadding(layoutDirection) + 16.dp,
                end = insets.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = insets.calculateBottomPadding(),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Image(
                    modifier = Modifier.size(80.dp),
                    painter = painterResource(Res.drawable.abysner_logo),
                    contentDescription = null
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Abysner", style = MaterialTheme.typography.displayMedium)
                    VersionText()
                }
            }
        } else {
            Image(
                modifier = Modifier.size(100.dp),
                painter = painterResource(Res.drawable.abysner_logo),
                contentDescription = null
            )
            Text(text = "Abysner", style = MaterialTheme.typography.displayLarge)
            VersionText()
        }
        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (isLandscape) 16.dp else 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            text = "Made with \uD83D\uDC99\uFE0F by Rolf Smit"
        )
        if (!isCompactHeight) {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (isLandscape) 16.dp else 32.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                text = "All time spent on this was not\nspent diving! Can you even imagine! \uD83D\uDE31"
            )
        }
    }
}

@Composable
private fun CopyrightFooter(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    singleLine: Boolean = false,
) {
    val footerTextColor = Color.White.copy(alpha = 0.85f)

    val linkStyle = SpanStyle(
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
        color = footerTextColor,
    )

    val annotatedString = buildAnnotatedString {

        pushLink(LinkAnnotation.Url("https://github.com/NeoTech-Software/Abysner"))
        withStyle(style = linkStyle) {
            append("Open-source")
        }
        pop()

        append(" & licensed under ")

        pushLink(LinkAnnotation.Url("https://www.gnu.org/licenses/agpl-3.0.txt"))
        withStyle(style = linkStyle) {
            append("GNU AGPLv3")
        }
        pop()

        if (singleLine) {
            append(" \u00B7 ")
        } else {
            appendLine()
        }

        pushLink(LinkAnnotation.Url("https://github.com/NeoTech-Software/Abysner/graphs/contributors"))
        withStyle(style = linkStyle) {
            append("Contributors")
        }
        pop()

        append(" \u00B7 ")

        pushLink(LinkAnnotation.Clickable(tag = "terms_and_conditions") {
            navController.navigate(Destinations.TERMS_AND_CONDITIONS.destinationName)
        })
        withStyle(style = linkStyle) {
            append("Terms & Conditions")
        }
        pop()
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(alpha = 0.2f),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                textAlign = TextAlign.Center,
                color = footerTextColor,
                lineHeight = 20.sp,
            ),
            text = annotatedString,
        )
    }
}

@Composable
private fun VersionText() {
    val versionString = when {
        LocalInspectionMode.current -> "0.0.0-test (preview)"
        VersionInfo.DIRTY -> "${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH}-dirty)"
        else -> "${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH})"
    }
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = versionString,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
    )
}

@Preview
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}
