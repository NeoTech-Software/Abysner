package org.neotech.app.abysner.data.diveplanning.resources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class ConfigurationResourceV1(
    val sacRate: Double,
    // Renamed from sacRateOutOfAir to sacRateStress. The SerialName keeps existing saves readable.
    @SerialName("sacRateOutOfAir")
    val sacRateStress: Double,
    val maxPPO2Deco: Double,
    val maxPPO2: Double,
    val maxEND: Double,
    val maxAscentRate: Double,
    val maxDescentRate: Double,
    val gfLow: Double,
    val gfHigh: Double,
    val forceMinimalDecoStopTime: Boolean,
    val useDecoGasBetweenSections: Boolean,
    val decoStepSize: Double,
    val lastDecoStopDepth: Double,
    val contingencyDeeper: Double,
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
    val ccrToHighSetpointSwitchDepth: Double? = null,
    val ccrToLowSetpointSwitchDepth: Double? = null,
    // Default allows deserializing saves that predate this field.
    val sacRateDeco: Double = 20.0,
    // Default allows deserializing saves that predate this field.
    val stressDurationMinutes: Int = 10,
): SerializableResource
