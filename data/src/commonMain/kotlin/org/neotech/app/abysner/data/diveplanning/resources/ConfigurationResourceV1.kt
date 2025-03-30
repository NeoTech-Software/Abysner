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
    val salinity: String,
    val altitude: Double,
    val algorithm: String,
): SerializableResource
