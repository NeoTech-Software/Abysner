package org.neotech.app.abysner.data.diveplanning.resources

import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class ConfigurationResourceV1(
    val sacRate: Double,
    val sacRateOutOfAir: Double,
    val maxPPO2Deco: Double,
    val maxPPO2: Double,
    val maxEND: Double,
    val maxAscentRate: Double,
    val maxDescentRate: Double,
    val gfLow: Double,
    val gfHigh: Double,
    val forceMinimalDecoStopTime: Boolean,
    val useDecoGasBetweenSections: Boolean,
    val decoStepSize: Int,
    val lastDecoStopDepth: Int,
    val contingencyDeeper: Int,
    val contingencyLonger: Int,
    // Default allows deserializing saves that predate this field.
    val gasSwitchTime: Int = 1,
    val salinity: String,
    val altitude: Double,
    val algorithm: String,
    // CCR fields: defaults allow deserializing saves that predate these fields.
    val ccrLowSetpoint: Double = 0.7,
    val ccrHighSetpoint: Double = 1.2,
    val ccrLoopVolumeLiters: Double = 7.0,
    val ccrMetabolicO2LitersPerMinute: Double = 0.8,
): SerializableResource
