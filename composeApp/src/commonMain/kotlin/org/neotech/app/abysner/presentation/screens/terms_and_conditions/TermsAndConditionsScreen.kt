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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.component.core.ifTrue
import org.neotech.app.abysner.presentation.component.core.onlyBottom
import org.neotech.app.abysner.presentation.component.core.withoutBottom
import org.neotech.app.abysner.presentation.screens.terms_and_conditions.TermsAndConditionsViewModel.ViewState
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.utilities.EventEffect
import org.neotech.app.abysner.presentation.utilities.closeApp
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
        onAcceptTermsAndConditions = { viewModel.acceptTermsAndConditions(true) },
        onDeclineTermsAndConditions = { viewModel.acceptTermsAndConditions(false) },
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

            if (viewState is ViewState.Content) {
                EventEffect(viewState.acceptAndNavigate) { accepted ->
                    if (accepted) {
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

            Column(
                modifier = Modifier.fillMaxSize().padding(insets.withoutBottom()),
                verticalArrangement = Arrangement.Center
            ) {

                if (viewState is ViewState.Content) {
                    Markdown(
                        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).ifTrue(viewState.accepted) {
                            padding(insets.onlyBottom())
                        },
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

                    if (!viewState.accepted) {

                        Surface(
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(insets.onlyBottom())
                                    .padding(16.dp),
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

                } else {
                    CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
private fun TermsAndConditionsScreenPreview() {

    val terms = """
        # Lorem Ipsum Dolor Sit Amet

        **Lorem Ipsum:** Consectetur Adipiscing Elit

        ## Lorem Ipsum
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. 

        ## 1. Lorem Ipsum

        - **Lorem:** Lorem ipsum dolor sit amet.
        - **Ipsum:** Consectetur adipiscing elit.
        - **Dolor:** Sed do eiusmod tempor.
        - **Sit:** Incididunt ut labore et.
        - **Amet:** Dolore magna aliqua.
        - **Consectetur:** Ut enim ad minim veniam.

        ## 2. Lorem Ipsum
        Quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 

        ## 3. Lorem Ipsum

        ### 3.1. Lorem Ipsum
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

        ### 3.2. Lorem Ipsum Dolor
        Quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.

        ## 4. Lorem Ipsum

        ### 4.1. Lorem Ipsum
        Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

        ### 4.2. Lorem Ipsum Dolor
        Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

        ## 5. Lorem Ipsum
        Ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse.

        ## 6. Lorem Ipsum
        Sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet.

        ## 7. Lorem Ipsum
        Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, [lorem@ipsum.dolor](mailto:lorem@ipsum.dolor).

    """.trimIndent()

    TermsAndConditionsScreen(
        navController = rememberNavController(),
        viewState = ViewState.Content(
            accepted = false,
            termsAndConditionsText = terms,
            consumed<Boolean>()
        ),
        onAcceptTermsAndConditions = {},
        onDeclineTermsAndConditions = {},
    )
}
