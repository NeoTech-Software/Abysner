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

package org.neotech.app.abysner.presentation.screens

import abysner.composeapp.generated.resources.Res
import abysner.composeapp.generated.resources.abysner_logo
import abysner.composeapp.generated.resources.ic_github
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.painterResource
import org.neotech.app.abysner.presentation.Destinations
import org.neotech.app.abysner.presentation.theme.AbysnerTheme
import org.neotech.app.abysner.presentation.component.core.onlyBottom
import org.neotech.app.abysner.presentation.component.core.preMixedWith
import org.neotech.app.abysner.presentation.component.core.withoutBottom
import org.neotech.app.abysner.version.VersionInfo
import kotlin.math.ceil
import kotlin.math.max


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

            val infiniteTransition = rememberInfiniteTransition()

            val wave1HorizontalMovementAnimation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(10000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            val wave2HorizontalMovementAnimation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            val wave2VerticalMovementAnimation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val animationStarted = rememberSaveable { mutableStateOf(false) }
            val cameraVerticalPanAnimation = remember { Animatable(if(animationStarted.value) { 1f} else { 0f }) }

            LaunchedEffect(cameraVerticalPanAnimation) {
                animationStarted.value = true
                cameraVerticalPanAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(5000, easing = FastOutSlowInEasing)
                )
            }

            // Color from top of logo
            val wave1ColorStart = Color(148, 195, 217)
            val wave1ColorEnd = wave1ColorStart.preMixedWith(Color.Black)

            // Color from bottom of logo
            val wave2ColorStart = Color(6, 38, 56)
            val wave2ColorEnd = wave2ColorStart.preMixedWith(Color.Black)

            val wave1Brush = Brush.verticalGradient(listOf(wave1ColorStart, wave1ColorEnd))
            val wave2Brush = Brush.verticalGradient(listOf(wave2ColorStart.copy(alpha = 0.7f), wave2ColorEnd.copy(alpha = 0.7f)))

            // Since the content and background of second box is animated, we need another Box as
            // wrapper to put the non animated content in.
            Box {
                Box(
                    Modifier
                        .padding(insets.withoutBottom())
                        // TODO left and right padding?
                        .fillMaxSize()
                        .drawWithCache {

                            val wave1Amplitude = 60.dp.toPx()
                            val wave1Length = 200.dp.toPx()

                            val wave2Amplitude = 30.dp.toPx()
                            val wave2VerticalMovement = wave2Amplitude / 2f
                            val wave2Length = 120.dp.toPx()

                            val boundsExtra = size.toRect().copy(
                                right = size.width + max(wave1Length, wave2Length)
                            )

                            // Animate waves down until 75% of the screen is above the surface
                            val wavesVerticalCameraPanDistance = (boundsExtra.bottom * 0.70f)
                            // Animate content up starting 20% below the vertical center of the screen.
                            val contentVerticalCameraPanDistance = boundsExtra.height * 0.2f

                            val wave1Path = createWavePath(
                                bounds = boundsExtra,
                                wavelength = wave1Length,
                                amplitude = wave1Amplitude
                            )
                            // wave2 has extra vertical offset, to make sure both are draw on the same
                            // imaginary horizontal line, but this offset is multiplied to have the
                            // foreground way slightly below the background wave.
                            val wave2Path = createWavePath(
                                bounds = boundsExtra,
                                wavelength = wave2Length,
                                amplitude = wave2Amplitude,
                                extraVerticalOffset = (wave1Amplitude - wave2Amplitude) * 1.5f
                            )

                            onDrawWithContent {

                                val wavesVerticalOffset =
                                    wavesVerticalCameraPanDistance * cameraVerticalPanAnimation.value

                                // Draw wave behind the content
                                translate(
                                    left = -(wave1Length * wave1HorizontalMovementAnimation),
                                    top = wavesVerticalOffset
                                ) {
                                    drawPath(style = Fill, brush = wave1Brush, path = wave1Path)
                                }

                                translate(
                                    // Pan content a little bit, inverse of the waves
                                    top = contentVerticalCameraPanDistance * (1f - cameraVerticalPanAnimation.value)
                                ) {
                                    this@onDrawWithContent.drawContent()
                                }

                                // Draw wave in front of the content
                                translate(
                                    left = -(wave2Length * wave2HorizontalMovementAnimation),
                                    // Animate the second wave also up and down (with half its amplitude)
                                    top = wavesVerticalOffset - (wave2VerticalMovement / 2f) + (wave2VerticalMovement * wave2VerticalMovementAnimation)
                                ) {
                                    drawPath(style = Fill, brush = wave2Brush, path = wave2Path)
                                }
                            }
                        }
                        .padding(insets.withoutBottom()),
                    contentAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.6f),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){

                        val grayTextColor = MaterialTheme.typography.bodySmall.color.preMixedWith(Color.White.copy(alpha = 0.8f))

                        Image(modifier = Modifier.size(100.dp), painter = painterResource(Res.drawable.abysner_logo), contentDescription = null)
                        Text(text = "Abysner", style = MaterialTheme.typography.displayLarge)
                        Text(modifier = Modifier.padding(top = 4.dp), text = getVersionString(), style = MaterialTheme.typography.bodySmall.copy(color = grayTextColor))
                        Text(
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            text = "Made with \uD83D\uDC99️ by Rolf Smit"
                        )
                        Text(
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 32.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = grayTextColor),
                            text = "All time spent on this was not\nspent diving! Can you even imagine! \uD83D\uDE31"
                        )
                    }
                }

                val annotatedString = buildAnnotatedString {

                    pushLink(LinkAnnotation.Url("https://github.com/NeoTech-Software/Abysner"))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                        append("Open-source")
                    }
                    pop()

                    append(" & licensed under ")

                    pushLink(LinkAnnotation.Url("https://www.gnu.org/licenses/agpl-3.0.txt"))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                        append("GNU AGPLv3")
                    }
                    pop()

                    appendLine()
                    pushLink(LinkAnnotation.Clickable(tag = "terms_and_conditions") { link ->
                        navController.navigate(Destinations.TERMS_AND_CONDITIONS_VIEW.destinationName)
                    })
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                        append("Terms & Conditions")
                    }
                    pop()
                }

                Text(
                    modifier = Modifier.padding(insets.onlyBottom()).padding(bottom = 16.dp).align(Alignment.BottomCenter),
                    style = MaterialTheme.typography.labelMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    text = annotatedString,
                )
            }
        }
    }
}

private fun getVersionString(): String {
    return if(!VersionInfo.DIRTY) {
        "${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH})"
    } else {
        "${VersionInfo.VERSION_NAME} (${VersionInfo.COMMIT_HASH}-dirty)"
    }
}

/**
 * Creates waves as a [Path] by utilizing a quadratic bezier. The wave size can be influenced using
 * [wavelength] and [amplitude]. The path is created in such a way that the wave starts from the
 * top-left and that it's highest point touches [Rect.top] of the given [bounds]. If [closePath] is
 * true the wave will be closed in such a way that it covers a rectangle that fills the remaining
 * space below the wave completely.
 */
private fun createWavePath(
    bounds: Rect,
    wavelength: Float,
    amplitude: Float,
    extraVerticalOffset: Float = 0f,
    closePath: Boolean = true,
): Path {

    // A single wave is made up of 2 bezier curves (up and down), hence the instance between points
    // is half that of a full wave length.
    val distanceBetweenPoints = wavelength / 2f
    val pointCount = ceil(bounds.width / distanceBetweenPoints).toInt() + 1

    // I'm creating a new Path object for brevity, but you'll
    // want to cache it somewhere to reuse across draw frames.
    return Path().apply {
        var pointX = bounds.left
        for (point in 0..pointCount) {
            val offsetY = bounds.top + amplitude + extraVerticalOffset
            when (point) {
                0 -> if (closePath) {
                    moveTo(pointX, bounds.bottom)
                    lineTo(pointX, offsetY)
                } else {
                    moveTo(pointX, offsetY)
                }

                else -> {
                    val controlY = if (point % 2 == 0) { -amplitude } else { amplitude }
                    quadraticTo(
                        x1 = pointX - (distanceBetweenPoints / 2f),
                        y1 = offsetY + controlY,
                        x2 = pointX,
                        y2 = offsetY
                    )
                }
            }
            pointX = (distanceBetweenPoints * point)
        }
        if (closePath) {
            lineTo(pointX, bounds.bottom)
        }
    }
}

@Preview
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}







