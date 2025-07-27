/*
 * Abysner - Dive planner
 * Copyright (C) 2024 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.terms_and_conditions

import abysner.composeapp.generated.resources.Res
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.screens.terms_and_conditions.TermsAndConditionsViewModel.ViewState
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.closeApp
import org.neotech.app.abysner.presentation.component.core.onlyBottom
import org.neotech.app.abysner.presentation.component.core.withoutBottom
import org.neotech.app.abysner.presentation.utilities.EventEffect
import org.neotech.app.abysner.presentation.utilities.consumed

typealias TermsAndConditionsScreen = @Composable (navController: NavHostController) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun TermsAndConditionsScreen(
    @Assisted navController: NavHostController = rememberNavController(),
    viewModelCreator: () -> TermsAndConditionsViewModel,
) {
    val viewModel = viewModel { viewModelCreator() }
    val viewState: ViewState = viewModel.viewState.collectAsState().value
    TermsAndConditionsScreen(
        navController = navController,
        viewState = viewState,
        onAcceptTermsAndConditions = {viewModel.acceptTermsAndConditions(true)},
        onDeclineTermsAndConditions = {viewModel.acceptTermsAndConditions(false)},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Inject
@Composable
fun TermsAndConditionsScreen(
    navController: NavHostController = rememberNavController(),
    viewState: ViewState,
    onAcceptTermsAndConditions: () -> Unit,
    onDeclineTermsAndConditions: () -> Unit,
) {
    AbysnerTheme {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                    TopAppBar(
                        title = { Text("Terms & Conditions") },
                        navigationIcon = {

                            val previousBackStackEntry = navController.previousBackStackEntry
                            if (previousBackStackEntry != null || LocalInspectionMode.current) {
                                IconButton(onClick = {
                                    navController.navigateUp()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { insets ->

            if(viewState is ViewState.Content) {
                EventEffect(viewState.acceptAndNavigate) { accepted ->
                    if(accepted) {
                        navController.navigate(Destinations.PLANNER.destinationName) {
                            popUpTo(Destinations.TERMS_AND_CONDITIONS.destinationName) {
                                inclusive = true
                            }
                        }
                    } else {
                        closeApp()
                    }
                }
            }

            Column(modifier = Modifier.padding(insets.withoutBottom())) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    if(viewState is ViewState.Content) {

                        Markdown(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                            content = viewState.termsAndConditionsText,
                            colors = markdownColor(),
                            padding = markdownPadding(
                                block = 8.dp
                            ),
                            typography = markdownTypography(
                                h1 = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                                h2 = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                h3 = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                h4 = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                h5 = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                h6 = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                text = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                quote = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                paragraph = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                ordered = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                bullet = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                list = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            )
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                Surface(
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(insets.onlyBottom()).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDeclineTermsAndConditions()
                            }) {
                            Text("Decline")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onAcceptTermsAndConditions()
                            }) {
                            Text("Accept")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
private fun TermsAndConditionsScreenPreview() {

    val terms = runBlocking {
        Res.readBytes("files/terms-and-conditions.md").decodeToString()
    }

    TermsAndConditionsScreen(
        navController = rememberNavController(),
        viewState = ViewState.Content(termsAndConditionsText = terms, consumed<Boolean>()),
        onAcceptTermsAndConditions = {},
        onDeclineTermsAndConditions = {},
    )
}
