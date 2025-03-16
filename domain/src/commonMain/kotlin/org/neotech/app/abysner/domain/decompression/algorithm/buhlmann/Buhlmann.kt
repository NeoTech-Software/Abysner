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

package org.neotech.app.abysner.domain.decompression.algorithm.buhlmann

import org.neotech.app.abysner.domain.core.model.Environment
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.core.physics.Pressure
import org.neotech.app.abysner.domain.core.physics.partialPressure
import org.neotech.app.abysner.domain.decompression.algorithm.DecompressionModel
import org.neotech.app.abysner.domain.diveplanning.DivePlanner.PlanningException
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * Pure Buhlmann model with gradient factors, without too much planning logic, just tissue loading.
 */
class Buhlmann(
    /**
     * Buhlmann version to use.
     */
    val version: Version,
    /**
     * The environment that is being dived in.
     */
    val environment: Environment,
    val gfLow: Double,
    val gfHigh: Double,
) : DecompressionModel {

    /**
     * Keeps track of the lowest ceiling in bars encountered during the dive so far, based on gfLow.
     */
    private var lowestCeiling: Double = 0.0

    /**
     * The tissues of the diver, these will be loaded during the dive with inert gasses (see:
     * [addFlat] and [addPressureChange]). Based on pressure levels in these tissues the ceiling will
     * be calculated.
     */
    private val tissues = mutableListOf<TissueCompartment>().apply {
        val compartments = when (version) {
            Version.ZH16A -> ZH16A_COMPARTMENTS
            Version.ZH16B -> ZH16B_COMPARTMENTS
            Version.ZH16C -> ZH16C_COMPARTMENTS
        }
        compartments.forEach {
            add(TissueCompartment(it, environment))
        }
    }

    /**
     * Add a descending, ascending or flat section of the dive to tissues.
     *
     * @param startPressure start depth in meters from the surface.
     * @param endPressure end depth in meters from the surface.
     * @param gas the gas being breathe by the diver during this section.
     * @param timeInMinutes the timeInMinutes this section takes.
     */
    override fun addPressureChange(startPressure: Pressure, endPressure: Pressure, gas: Gas, timeInMinutes: Int) {
        val fO2 = gas.oxygenFraction
        val fHe = gas.heliumFraction

        var loadChange = 0.0
        tissues.forEach {
            val tissueChange = it.addPressureChange(startPressure.value, endPressure.value, fO2, fHe, timeInMinutes)
            loadChange += tissueChange
        }
    }

    override fun addSurfaceInterval(timeInMinutes: Int) {
        addFlat(Pressure(environment.atmosphericPressure), Gas.Air, timeInMinutes)
    }

    /**
     * Get the current ceiling based on maximum tissue tolerances in meters from the surface.
     */
    override fun getCeiling(): Pressure {
        val ceiling = this.getMinimumToleratedAmbientPressure(environment.atmosphericPressure, gfLow, gfHigh)

        // Cap ceiling to surface, which is fine, because tissues will only be affected underwater.
        // This is obviously not entirely true, in theory driving up a mountain directly after a
        // dive may still break the ceiling, but for the sake of this application we don't need
        // this information.
        return if (ceiling < environment.atmosphericPressure) {
            Pressure(environment.atmosphericPressure)
        } else {
            Pressure(ceiling)
        }
    }

    /**
     * Finds the lowest tolerated ambient pressure of all tissue compartments, taking into account
     * the given gradient factors, where gfLow applies to the lowest known ceiling.
     */
    private fun getMinimumToleratedAmbientPressure(surfacePressure: Double, gfLow: Double, gfHigh: Double): Double {
        val currentLowestCeiling = tissues.calculateCeiling(gfLow)

        if (currentLowestCeiling > this.lowestCeiling) {
            this.lowestCeiling = currentLowestCeiling
        }

        val currentCeiling = tissues.maxOf {
            it.toleratedInertGasPressure(surfacePressure, lowestCeiling, gfHigh, gfLow)
        }
        return currentCeiling
    }

    override fun getNoDecompressionLimit(depth: Pressure, gas: Gas): Int {
        // Take a snapshot of the tissues to restore at a later moment
        val snapshot = snapshot()
        var minutesAdded = 0
        // Load tissues at given depth minutes by minute until the ceiling is below the surface.
        while(getCeiling().value <= environment.atmosphericPressure) {
            minutesAdded++
            addFlat(depth, gas, 1)
        }
        reset(snapshot)
        return minutesAdded
    }

    override fun snapshot(): BuhlmannSnapshot {
        return BuhlmannSnapshot(
            lowestCeiling = lowestCeiling,
            tissues = tissues.map { it.copy() },
            version = version
        )
    }

    override fun reset(snapshot: DecompressionModel.Snapshot) {
        if(snapshot !is BuhlmannSnapshot) {
            throw PlanningException("Unable to restore snapshot, snapshot must be of type BuhlmannSnapshot!")
        } else if (version != snapshot.version) {
            throw PlanningException("Unable to restore BuhlmannSnapshot: snapshot Buhlmann version (${snapshot.version}) does not equal the current Buhlmann version ($version)!")
        }
        this.lowestCeiling = snapshot.lowestCeiling
        this.tissues.clear()
        this.tissues.addAll(snapshot.tissues)
    }

    override fun reset() {
        this.lowestCeiling = 0.0
        this.tissues.clear()
        val compartments = when (version) {
            Version.ZH16A -> ZH16A_COMPARTMENTS
            Version.ZH16B -> ZH16B_COMPARTMENTS
            Version.ZH16C -> ZH16C_COMPARTMENTS
        }
        compartments.forEach {
            tissues.add(TissueCompartment(it, environment))
        }
    }

    data class BuhlmannSnapshot(
        val lowestCeiling: Double,
        val tissues: List<TissueCompartment>,
        val version: Version
    ) : DecompressionModel.Snapshot {

        override val model: KClass<out DecompressionModel> = Buhlmann::class
    }

    enum class Version {
        ZH16A,
        ZH16B,
        ZH16C
    }
}

data class CompartmentParameters(
    val n2HalfTime: Double,
    val n2ValueA: Double,
    val n2ValueB: Double,
    val heHalfTime: Double,
    val heValueA: Double,
    val heValueB: Double,
)

/**
 * In the Buhlmann algorithm a compartment is an representation of a certain tissue type. The count
 * of tissue types differs between version of the Buhlmann algorithm. The compartment keeps track of
 * the gas loading in this tissue type.
 */
data class TissueCompartment(
    val parameters: CompartmentParameters,
    /**
     * The salinity (density) and atmospheric pressure of the dive.
     *
     * Note: a new compartment always starts fully saturated at atmospheric pressure (in equilibrium).
     */
    // TODO This could be moved away from the constructor and into the gas loading methods as
    //   parameter to allow for calculating dives in which the diver encounters both salt and fresh
    //   water?
    val environment: Environment,
    /**
     * Initial nitrogen load assumes fully saturated (equilibrium), regardless of atmospheric pressure.
     */
    private var pNitrogen: Double = partialPressure(environment.atmosphericPressure, 0.79) - waterVapourPressure,
    private var pHelium: Double = 0.0,
    private var pTotal: Double = pNitrogen + pHelium
) {

    fun addPressureChange(startPressure: Double, endPressure: Double, fO2: Double, fHe: Double, timeInMinutes: Int): Double {
        if (timeInMinutes <= 0) {
            throw IllegalArgumentException("Invalid duration `$timeInMinutes` for on/off-gassing tissues. The minimum duration must be higher then 0.")
        }
        // Calculate nitrogen fraction (by just subtracting oxygen and helium)
        val fN2 = (1.0 - fO2) - fHe
        if (fN2 < 0.0) {
            throw IllegalArgumentException("Invalid gas mix `$fO2/$fHe` for on/off-gassing tissues, oxygen and helium should together never exceed 1.0 (100% of the gas mix).")
        }

        // The code below assumes a non-constant partial pressure for oxygen and helium.
        // However with CCR the oxygen fraction remains constant, causing the nitrogen fraction to
        // change constantly.
        // Instead of Schreiners equation for CCR it may make more sens to instead simulate the
        // constant oxygen fraction by using small time increments at constant pressure.
        // TODO: Once CCR support is added consider making the time increments smaller (seconds)?
        // https://thetheoreticaldiver.org/wordpress/index.php/2017/11/30/ccr-schreiner-equation/

        val depthChangeInBarsPerMinute = pressureChangeInBarsPerMinute(startPressure, endPressure, timeInMinutes)

        // Calculate nitrogen loading
        var gasRate = partialPressure(depthChangeInBarsPerMinute, fN2)
        var pGas = partialPressure(startPressure, fN2)
        this.pNitrogen = schreinerEquation(pNitrogen, pGas, timeInMinutes, parameters.n2HalfTime, gasRate)

        // Calculate helium loading
        gasRate = partialPressure(depthChangeInBarsPerMinute, fHe)
        pGas = partialPressure(startPressure, fHe)
        this.pHelium = schreinerEquation(pHelium, pGas, timeInMinutes, parameters.heHalfTime, gasRate)

        val prevTotal = this.pTotal
        // Calculate total loading
        this.pTotal = this.pNitrogen + this.pHelium

        // Return the difference of load added.
        return this.pTotal - prevTotal
    }

    /**
     * Returns this compartments ceiling in bars, with the given [gf] conservatism factor applied.
     * A ceiling above the surface is clamped to [Environment.atmosphericPressure].
     */
    fun calculateCeiling(gf: Double): Double {

        // Calculate combined a and b coefficients (since both helium and nitrogen are tracked)
        //
        // 𝑃ₜ = 𝑃𝑛₂ + 𝑃ℎ𝑒
        // 𝐴 = (𝐴𝑛₂ ∗ 𝑃𝑛₂ + 𝐴ℎ𝑒 ∗ 𝑃ℎ𝑒) / 𝑃ₜ
        // 𝐵 = (𝐵𝑛₂ ∗ 𝑃𝑛₂ + 𝐵ℎ𝑒 ∗ 𝑃ℎ𝑒) / 𝑃ₜ

        val a = ((parameters.n2ValueA * this.pNitrogen) + (parameters.heValueA * this.pHelium)) / (this.pTotal)
        val b = ((parameters.n2ValueB * this.pNitrogen) + (parameters.heValueB * this.pHelium)) / (this.pTotal)

        // Calculate the pressure limit (which is the ceiling)
        // 𝑃𝑙 = (𝑃ₜ − 𝐴 ∗ 𝑔𝑓) / (𝑔𝑓 / 𝐵 + 1.0 − 𝑔𝑓)

        val ceiling = (this.pTotal - (a * gf)) / ((gf / b) + 1.0 - gf)

        // Ceiling may be above the surface pressure, for these cases just clamp the ceiling to the surface pressure.
        return max(ceiling, environment.atmosphericPressure)
    }

    fun toleratedInertGasPressure(surface: Double, lowestCeiling: Double, gfHigh: Double, gfLow: Double): Double {

        // The equations/formulas below are the result of looking at the combined work of:
        // - https://github.com/subsurface/subsurface/commit/67d59ff0181f4dccdd46923d53cda7902f279a57
        // - https://www.heinrichsweikamp.net/downloads/OSTC_GF_web_en.pdf
        // - http://www.dive-tech.co.uk/resources/mvalues.pdf
        // - https://wrobell.dcmod.org/decotengu/model.html
        // And probably a whole bunch of other sources on the internet.

        // TODO cache A and B values? Calculate them right after tissue loading occurred?
        // a = M-value at 0 ATM (intercept)
        // b = Slope
        // Note: b is not like the b in the slope-intercept standard form it purely refers to 'b' as
        // found in the Bühlmann model (reciprocal of the slope)
        val a = ((parameters.n2ValueA * this.pNitrogen) + (parameters.heValueA * this.pHelium)) / (this.pTotal)
        val b = ((parameters.n2ValueB * this.pNitrogen) + (parameters.heValueB * this.pHelium)) / (this.pTotal)

        // This calculates the current M-value (𝑃𝑡.𝑡𝑜𝑙 - tolerated tissue inert gas pressure) at the surface adjusted for GF-high
        val pToleratedSurface = (surface / b + a - surface) * gfHigh + surface
        // This calculates the current M-value (𝑃𝑡.𝑡𝑜𝑙 - tolerated tissue inert gas pressure) at the lowest ceiling adjusted for GF-Low
        val pToleratedLowestCeiling = (lowestCeiling / b + a - lowestCeiling) * gfLow + lowestCeiling

        // Now we know for 2 given ambient pressures (surface @ gfHigh and lowestCeiling @ gfLow)
        // the corresponding M-values (tolerated tissue inert gas pressure).
        //
        // These are essentially the 2 known points of the new "shifted" M-value line, the gradient line.
        //
        // What we do not know is what the slope and intercept of this new line is, so we have 2
        // unknown values:
        //
        // gfSlope (α)
        // gfIntercept (β)
        //
        // We need to find these parameters so that:
        //
        // gfSlope * surface + gfIntercept = (surface / b + a - surface) * gfHigh + surface
        // and
        // gfSlope * lowestCeiling + gfIntercept = (lowestCeiling / b + a - lowestCeiling) * gfLow + lowestCeiling
        //
        // We can solve for gfSlope and GfIntercept and use these to obtain the maximum ambient pressure if we invert the equation as well.
        //
        // For gfSlope this can be done using the following equation:
        //     gfSlopeNumerator = (lowestCeiling - surface + gfLow * (lowestCeiling * (1.0 - b) / b + a) - gfHigh * (surface * (1.0 - b) / b + a))
        //     gfSlopeDenominator = (lowestCeiling - surface)
        //     gfSlope = alphaNumerator / alphaDenominator
        //
        // For gfIntercept this can be done using the following equation:
        //
        //     gfIntercept = surface + gfHigh * (surface * (1.0 - b) / b + a) - gfSlope * surface
        //
        // Then we can calculate the tolerated tissue inert gas pressure given the current total insert gas pressure
        //     currentTolerated = (pTotal - gfIntercept) / gfSlope
        //

        if (pToleratedSurface < pToleratedLowestCeiling) {
            // Maximum allowed inert gas pressure is higher at the surface then at the lowest ceiling
            // this essentially means this compartment has a ceiling below the surface. And thus
            // calculating the ceiling using gradient factors makes sense.

            // Now calculate the true ceiling by linearly interpolating between the to M-values the difference between gfLow and gfHigh

            // Calculate gfSlope (α)
            val gfSlopeNumerator = (lowestCeiling - surface + gfLow * (lowestCeiling * (1.0 - b) / b + a) - gfHigh * (surface * (1.0 - b) / b + a))
            val gfSlopeDenominator = (lowestCeiling - surface)
            val gfSlope = gfSlopeNumerator / gfSlopeDenominator

            // Calculate gfIntercept (β)
            val gfIntercept = surface + gfHigh * (surface * (1.0 - b) / b + a) - gfSlope * surface

            // Returns 𝑃𝑎𝑚𝑏.𝑡𝑜𝑙 for 𝑃𝑡 at at a certain gradient factor
            return (pTotal - gfIntercept) / gfSlope
        } else {
            return 0.0
        }
    }

    companion object {
        // TODO some implementations online seem to use a value of 35.2 degrees, why is that?
        //      Do those implementation assume temperature of water vapour in the lung alveoli is never
        //      exactly 37.0 degrees as the air from a OC system comes in cold?
        private val waterVapourPressure: Double = waterVapourPressureInBars(37.0)
    }
}

/**
 * Returns the current lowest ceiling of the give compartments in bars, with the given [gf]
 * conservatism factor applied. A ceiling above the surface is clamped to
 * [Environment.atmosphericPressure].
 */
private fun List<TissueCompartment>.calculateCeiling(gf: Double): Double {
    require(gf in 0.0..1.0) { "Gradient factor should not be outside the 0.0-1.0 range, but is $gf!" }
    return this.maxOf { it.calculateCeiling(gf) }
}

// Note on the compartment values:
//
// It's hard to find the exact numbers of these compartments for some reason, I validated these
// numbers by comparing these to some existing open-source software:
//
// https://github.com/subsurface/subsurface/blob/35556b9f438c086ec62503397be877aa1120b0a7/core/deco.cpp#L83
// https://github.com/gully/decotengu/blob/9d6a7f538bcc9497a40e8e5bf9017d07141bc3bb/decotengu/model.py#L616C40-L616C48
// https://github.com/ThomasChiroux/dipplanner/commit/b3c0b4a9f859e466c439ba1e17ddfa1b559a8e07#diff-85de4a66ed8ea8b282276c341afee8ad019a417a337cbe3f5c2fcb745c76cc70
//
// Please note:
// - N2 half times are equal in version A, B and C
// - b values are equal in version A, B and C
//
// TLDR: Only 'a' values differ.


private val ZH16A_COMPARTMENTS = listOf(
    CompartmentParameters(5.0, 1.1696, 0.5578, 1.88, 1.6189, 0.4770),
    CompartmentParameters(8.0, 1.0000, 0.6514, 3.02, 1.3830, 0.5747),
    CompartmentParameters(12.5, 0.8618, 0.7222, 4.72, 1.1919, 0.6527),
    CompartmentParameters(18.5, 0.7562, 0.7825, 6.99, 1.0458, 0.7223),
    CompartmentParameters(27.0, 0.6667, 0.8126, 10.21, 0.9220, 0.7582),
    CompartmentParameters(38.3, 0.5933, 0.8434, 14.48, 0.8205, 0.7957),
    CompartmentParameters(54.3, 0.5282, 0.8693, 20.53, 0.7305, 0.8279),
    CompartmentParameters(77.0, 0.4701, 0.8910, 29.11, 0.6502, 0.8553),
    CompartmentParameters(109.0, 0.4187, 0.9092, 41.20, 0.5950, 0.8757),
    CompartmentParameters(146.0, 0.3798, 0.9222, 55.19, 0.5545, 0.8903),
    CompartmentParameters(187.0, 0.3497, 0.9319, 70.69, 0.5333, 0.8997),
    CompartmentParameters(239.0, 0.3223, 0.9403, 90.34, 0.5189, 0.9073),
    CompartmentParameters(305.0, 0.2971, 0.9477, 115.29, 0.5181, 0.9122),
    CompartmentParameters(390.0, 0.2737, 0.9544, 147.42, 0.5176, 0.9171),
    CompartmentParameters(498.0, 0.2523, 0.9602, 188.24, 0.5172, 0.9217),
    CompartmentParameters(635.0, 0.2327, 0.9653, 240.03, 0.5119, 0.9267)
)

private val ZH16B_COMPARTMENTS = listOf(
    CompartmentParameters(5.0, 1.1696, 0.5578, 1.88, 1.6189, 0.4770),
    CompartmentParameters(8.0, 1.0000, 0.6514, 3.02, 1.3830, 0.5747),
    CompartmentParameters(12.5, 0.8618, 0.7222, 4.72, 1.1919, 0.6527),
    CompartmentParameters(18.5, 0.7562, 0.7825, 6.99, 1.0458, 0.7223),
    CompartmentParameters(27.0, 0.6667, 0.8126, 10.21, 0.9220, 0.7582),
    CompartmentParameters(38.3, 0.5600, 0.8434, 14.48, 0.8205, 0.7957),
    CompartmentParameters(54.3, 0.4947, 0.8693, 20.53, 0.7305, 0.8279),
    CompartmentParameters(77.0, 0.4500, 0.8910, 29.11, 0.6502, 0.8553),
    CompartmentParameters(109.0, 0.4187, 0.9092, 41.20, 0.5950, 0.8757),
    CompartmentParameters(146.0, 0.3798, 0.9222, 55.19, 0.5545, 0.8903),
    CompartmentParameters(187.0, 0.3497, 0.9319, 70.69, 0.5333, 0.8997),
    CompartmentParameters(239.0, 0.3223, 0.9403, 90.34, 0.5189, 0.9073),
    CompartmentParameters(305.0, 0.2850, 0.9477, 115.29, 0.5181, 0.9122),
    CompartmentParameters(390.0, 0.2737, 0.9544, 147.42, 0.5176, 0.9171),
    CompartmentParameters(498.0, 0.2523, 0.9602, 188.24, 0.5172, 0.9217),
    CompartmentParameters(635.0, 0.2327, 0.9653, 240.03, 0.5119, 0.9267)
)

private val ZH16C_COMPARTMENTS = listOf(
    CompartmentParameters(5.0, 1.1696, 0.5578, 1.88, 1.6189, 0.4770),
    CompartmentParameters(8.0, 1.0000, 0.6514, 3.02, 1.3830, 0.5747),
    CompartmentParameters(12.5, 0.8618, 0.7222, 4.72, 1.1919, 0.6527),
    CompartmentParameters(18.5, 0.7562, 0.7825, 6.99, 1.0458, 0.7223),
    CompartmentParameters(27.0, 0.6200, 0.8126, 10.21, 0.9220, 0.7582),
    CompartmentParameters(38.3, 0.5043, 0.8434, 14.48, 0.8205, 0.7957),
    CompartmentParameters(54.3, 0.4410, 0.8693, 20.53, 0.7305, 0.8279),
    CompartmentParameters(77.0, 0.4000, 0.8910, 29.11, 0.6502, 0.8553),
    CompartmentParameters(109.0, 0.3750, 0.9092, 41.20, 0.5950, 0.8757),
    CompartmentParameters(146.0, 0.3500, 0.9222, 55.19, 0.5545, 0.8903),
    CompartmentParameters(187.0, 0.3295, 0.9319, 70.69, 0.5333, 0.8997),
    CompartmentParameters(239.0, 0.3065, 0.9403, 90.34, 0.5189, 0.9073),
    CompartmentParameters(305.0, 0.2835, 0.9477, 115.29, 0.5181, 0.9122),
    CompartmentParameters(390.0, 0.2610, 0.9544, 147.42, 0.5176, 0.9171),
    CompartmentParameters(498.0, 0.2480, 0.9602, 188.24, 0.5172, 0.9217),
    CompartmentParameters(635.0, 0.2327, 0.9653, 240.03, 0.5119, 0.9267)
)
