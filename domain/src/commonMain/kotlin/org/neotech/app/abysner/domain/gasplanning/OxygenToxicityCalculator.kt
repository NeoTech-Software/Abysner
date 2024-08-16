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

package org.neotech.app.abysner.domain.gasplanning

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.physics.depthInMetersToBars
import org.neotech.app.abysner.domain.decompression.model.DiveSegment
import kotlin.math.exp
import kotlin.math.pow

/**
 * This class contains routines for calculating CNS and OTU loading, it is mostly based on 2 blog
 * posts written by by Robert C. Helling (which again is based on Erik Baker his work) and an
 * open-source implementation of Robert C. Hellings blog posts that is MIT licensed called
 * GasPlanner (by Jiri Pokorny):
 *
 * - http://web.archive.org/web/20170708072206/https://www.shearwater.com/wp-content/uploads/2012/08/Oxygen_Toxicity_Calculations.pdf
 * - https://thetheoreticaldiver.org/wordpress/index.php/2019/08/15/calculating-oxygen-cns-toxicity/
 * - https://thetheoreticaldiver.org/wordpress/index.php/2018/12/05/a-few-thoughts-on-oxygen-toxicity/
 * - https://github.com/jirkapok/GasPlanner/blob/master/projects/scuba-physics/src/lib/cnsCalculator.ts
 */
class OxygenToxicityCalculator {

    fun calculateCns(segments: List<DiveSegment>, environment: Environment): Double {
        var cns = 0.0
        segments.forEach {
            cns += calculateCns(it.cylinder.gas.oxygenFraction, it.startDepth, it.endDepth, environment, it.duration)
        }
        return cns
    }

    private fun calculateCns(fO2: Double, startDepth: Double, endDepth: Double, environment: Environment, duration: Int): Double {
        val avgDepth = depthInMetersToBars((startDepth + endDepth) / 2.0, environment)
        val ppO2 = fO2 * avgDepth

        if(ppO2 <= MINIMUM_CNS_PPO2) {
            return 0.0
        }

        val exponent = this.getCnsPpo2Slope(ppO2)
        return (duration * 60.0) * exp(exponent) * 100.0
    }

    /**
     * Essentially 2 linear lines fitted on the curve of the raw values in the NOAA table (with a
     * logarithmic timescale).
     *
     * Based on: https://thetheoreticaldiver.org/wordpress/index.php/2019/08/15/calculating-oxygen-cns-toxicity/
     */
    private fun getCnsPpo2Slope(ppO2: Double): Double {
        if(ppO2 <= 1.5) {
            return -11.7853 + (1.93873 * ppO2)
        }
        return -23.6349 + (9.80829 * ppO2)
    }

    fun calculateOtu(segments: List<DiveSegment>, environment: Environment): Double {
        // For this calculation it should not matter if segments are multiple minutes long, but for CNS this
        // seems to be more important (or even necessary).
        var otu = 0.0
        segments.forEach {
            val o2 = it.cylinder.gas.oxygenFraction
            otu += this.calculateOtu(it.duration, o2, it.startDepth, it.endDepth, environment)
        }
        return otu
    }

    private fun calculateOtu(duration: Int, pO2: Double, startDepth: Double, endDepth: Double, environment: Environment): Double {
        var durationInMinutes = duration.toDouble()
        val startAAP = depthInMetersToBars(startDepth, environment)
        val endAAP = depthInMetersToBars(endDepth, environment)
        var ppo2Start = startAAP * pO2
        var ppo2End = endAAP * pO2

        if ((ppo2Start <= MINIMAL_OTU_PPO2) && (ppo2End <= MINIMAL_OTU_PPO2)) {
            // If both start and end partial pressure is below a PPO2 of 0.5, then calculating OTU is
            // pointless since the effect only really starts at 0.5.
            return 0.0
        }

        // only part of the segment bellow limit
        if (ppo2Start <= MINIMAL_OTU_PPO2) {
            // PPO2 at start is lower then the minimum, only take into account the part of the segment
            // that is higher then the minimum value, then also change ppo2start ot the minimum,
            // to make sure calculations start form there.
            durationInMinutes = durationInMinutes * (ppo2End - MINIMAL_OTU_PPO2) / (ppo2End - ppo2Start)
            ppo2Start = MINIMAL_OTU_PPO2
        } else if (ppo2End <= MINIMAL_OTU_PPO2) {
            durationInMinutes = durationInMinutes * (ppo2Start - MINIMAL_OTU_PPO2) / (ppo2Start - ppo2End)
            ppo2End = MINIMAL_OTU_PPO2
        }

        // Robert in his blog post "A few thoughts on oxygen toxicity" suggests a new formula for OTU
        // based on Erik Baker his paper "Oxygen Toxicity Calculations", this formula allows calculating
        // both ascents an descents as well as equal depth sections all at once (without divide by zero issues):
        // https://thetheoreticaldiver.org/wordpress/index.php/2018/12/05/a-few-thoughts-on-oxygen-toxicity/
        // The new formula uses a new variable called Pm which is calculated like this:
        // Pm = (Pa + Pb) / 2
        // This variable is then used in the main formula
        //
        // ...(Pm - 0.5) / 0.5...
        //
        // Or expanded:
        //
        // ...((Pa + Pb) / 2 - 0.5 / 0.5)...
        //
        // Which is basically saying divide by 2, subtract 0.5 then multiply by 2 again, so one could
        // have subtracted 1.0 directly instead, for the same result:
        val pm = (ppo2Start + ppo2End) - 1.0
        val rate = pm.pow(5.0 / 6.0) * (1.0 - 5.0 * (ppo2End - ppo2Start).pow(2) / 216 / (pm * pm))
        return rate * durationInMinutes
    }
}

private const val MINIMAL_OTU_PPO2 = 0.5
private const val MINIMUM_CNS_PPO2 = 0.5
