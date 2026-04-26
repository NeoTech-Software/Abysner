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

package org.neotech.app.abysner.domain.gasplanning

import org.neotech.app.abysner.domain.core.model.BreathingMode
import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.physics.metersToAmbientPressure
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import org.neotech.app.abysner.domain.decompression.algorithm.buhlmann.ccrSchreinerInputs
import kotlin.math.exp
import kotlin.math.pow

/**
 * This class contains routines for calculating CNS and OTU loading, it is mostly based on 2 blog
 * posts written by Robert Helling (which again is based on Erik Baker his work) and an open-source
 * implementation of Helling's blog posts that is MIT licensed called GasPlanner (by Jiri Pokorny):
 *
 * - http://web.archive.org/web/20170708072206/https://www.shearwater.com/wp-content/uploads/2012/08/Oxygen_Toxicity_Calculations.pdf
 * - https://thetheoreticaldiver.org/wordpress/index.php/2019/08/15/calculating-oxygen-cns-toxicity/
 * - https://thetheoreticaldiver.org/wordpress/index.php/2018/12/05/a-few-thoughts-on-oxygen-toxicity/
 * - https://github.com/jirkapok/GasPlanner/blob/master/projects/scuba-physics/src/lib/cnsCalculator.ts
 */
object OxygenToxicityCalculator {

    fun calculateCns(segments: List<DiveSegment>, environment: Environment): Double {
        var cns = 0.0
        segments.forEach {
            val averagePressure = metersToAmbientPressure((it.startDepth + it.endDepth) / 2.0, environment).value
            val ppO2 = effectivePartialOxygenPressure(it.cylinder.gas.oxygenFraction, averagePressure, it.breathingMode)
            cns += calculateCns(ppO2, it.duration)
        }
        return cns
    }

    private fun calculateCns(ppO2: Double, duration: Int): Double {
        if (ppO2 <= MINIMUM_CNS_PPO2) {
            return 0.0
        }
        val exponent = getCnsPpo2Slope(ppO2)
        return (duration * 60.0) * exp(exponent) * 100.0
    }

    /**
     * Essentially 2 linear lines fitted on the curve of the raw values in the NOAA table (with a
     * logarithmic timescale).
     *
     * Based on: https://thetheoreticaldiver.org/wordpress/index.php/2019/08/15/calculating-oxygen-cns-toxicity/
     *
     * Note: In 2025 research suggested relaxing the ppO2 = 1.3 bar single-exposure limit from
     * 180 to 240 min. If the NOAA table is updated accordingly, the ppO2 ≤ 1.5 curve fit below
     * should be re-fitted against the new values. See: "Revised guideline for CNS oxygen toxicity
     * exposure limits when using an inspired PO2 of 1.3 atmospheres."
     * https://doi.org/10.28920/dhm55.3.262-270
     */
    private fun getCnsPpo2Slope(ppO2: Double): Double {
        if(ppO2 <= 1.5) {
            return -11.7853 + (1.93873 * ppO2)
        }
        return -23.6349 + (9.80829 * ppO2)
    }

    fun calculateOtu(segments: List<DiveSegment>, environment: Environment): Double {
        // For this calculation it should not matter if segments are multiple minutes long, but for
        // CNS this seems to be more important (or even necessary).
        var otu = 0.0
        segments.forEach {
            val startAmbientPressure = metersToAmbientPressure(it.startDepth, environment).value
            val endAmbientPressure = metersToAmbientPressure(it.endDepth, environment).value
            val ppo2Start = effectivePartialOxygenPressure(it.cylinder.gas.oxygenFraction, startAmbientPressure, it.breathingMode)
            val ppo2End = effectivePartialOxygenPressure(it.cylinder.gas.oxygenFraction, endAmbientPressure, it.breathingMode)
            otu += this.calculateOtu(it.duration, ppo2Start, ppo2End)
        }
        return otu
    }

    private fun calculateOtu(duration: Int, ppo2AtStart: Double, ppo2AtEnd: Double): Double {
        var durationInMinutes = duration.toDouble()
        var ppo2Start = ppo2AtStart
        var ppo2End = ppo2AtEnd

        if ((ppo2Start <= MINIMAL_OTU_PPO2) && (ppo2End <= MINIMAL_OTU_PPO2)) {
            // If both start and end partial pressure is below a PPO2 of 0.5, then calculating OTU
            // is pointless since the effect only really starts at 0.5.
            return 0.0
        }

        // only part of the segment bellow limit
        if (ppo2Start <= MINIMAL_OTU_PPO2) {
            // PPO2 at start is lower than the minimum: only take into account the part of the
            // segment that is higher than the minimum value, also change ppo2start to the
            // minimum, to make sure calculations take into account only the valid section.
            durationInMinutes = durationInMinutes * (ppo2End - MINIMAL_OTU_PPO2) / (ppo2End - ppo2Start)
            ppo2Start = MINIMAL_OTU_PPO2
        } else if (ppo2End <= MINIMAL_OTU_PPO2) {
            durationInMinutes = durationInMinutes * (ppo2Start - MINIMAL_OTU_PPO2) / (ppo2Start - ppo2End)
            ppo2End = MINIMAL_OTU_PPO2
        }

        // Robert in his blog post "A few thoughts on oxygen toxicity" suggests a new formula for
        // OTU based on Erik Baker his paper "Oxygen Toxicity Calculations", this formula allows
        // calculating ascents, descents and flat sections all at once (without divide by zero
        // issues):
        // https://thetheoreticaldiver.org/wordpress/index.php/2018/12/05/a-few-thoughts-on-oxygen-toxicity/

        // The new formula uses a new variable called Pm which is calculated like this:
        // Pm = (Pa + Pb) / 2
        // This variable is then used in the main formula
        //
        // (Pm - 0.5) / 0.5
        //
        // Or expanded:
        //
        // ((Pa + Pb) / 2 - 0.5 / 0.5)
        //
        // Which is basically saying divide by 2, subtract 0.5 then multiply by 2 again, so one could
        // have subtracted 1.0 directly instead, for the same result:
        val pm = (ppo2Start + ppo2End) - 1.0
        val rate = pm.pow(5.0 / 6.0) * (1.0 - 5.0 * (ppo2End - ppo2Start).pow(2) / 216 / (pm * pm))
        return rate * durationInMinutes
    }

    /**
     * Returns the effective ppO2 for a given ambient pressure and breathing mode.
     *
     * For open circuit this is simply fO2 * ambientPressure. For closed circuit, the ppO2 cannot
     * exceed ambient pressure at shallow depths, and the assumption is made that if the ppO2 from
     * just the diluent is higher than the setpoint, the setpoint will never be reached (same
     * assumption as [ccrSchreinerInputs]). In reality the ppO2 will eventually drop due to
     * metabolic consumption. This transient is not modeled, matching the approach used by other CCR
     * planners.
     */
    internal fun effectivePartialOxygenPressure(
        fO2Diluent: Double,
        ambientPressure: Double,
        breathingMode: BreathingMode,
    ): Double = when (breathingMode) {
        // Note: it seems kinda weird that we don't correct for water vapor pressure here, but I
        // guess it makes sense since the tables the calculations here are based on empirical
        // testing where they did not look specifically at the true inspired O2? But rather ambient
        // PPO2? Anyhow, not correcting is the conservative choice anyway.
        is BreathingMode.OpenCircuit ->
            fO2Diluent * ambientPressure
        is BreathingMode.ClosedCircuit -> {
            val diluentPpO2 = fO2Diluent * ambientPressure
            minOf(maxOf(breathingMode.setpoint, diluentPpO2), ambientPressure)
        }
    }

    private const val MINIMAL_OTU_PPO2 = 0.5
    private const val MINIMUM_CNS_PPO2 = 0.5
}
