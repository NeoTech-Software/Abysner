/*
 * Abysner - Dive planner
 * Copyright (C) 2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.domain.diveplanning

import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.UnitSystem
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.toAssignedCylinder
import org.neotech.app.abysner.domain.diveplanning.model.truncateAtRuntime
import org.neotech.app.abysner.domain.gasplanning.GasPlanner

/**
 * Plans a sequence of dives, carrying tissue loading and surface intervals from one dive into the
 * next.
 */
class MultiDivePlanner(
    private val configuration: Configuration,
    private val unitSystem: UnitSystem,
) {

    fun plan(model: MultiDivePlanInputModel): MultiDivePlanSet {
        val planner = DivePlanner(configuration, unitSystem)
        val gasPlanner = GasPlanner()

        val sets = model.dives.mapIndexed { index, diveInput ->
            // Apply surface interval before this dive.
            // index == 0 is skipped, surfaceIntervalBefore of the first dive is ignored in planning.
            if (index > 0) {
                diveInput.surfaceIntervalBefore?.let { planner.addSurfaceInterval(it) }
            }

            val segments = diveInput.plannedProfile.toMutableList()
            val deepestIdx = segments.indices.maxByOrNull { segments[it].depthInMeters }
            val deeper = configuration.contingencyDeeper.takeIf { diveInput.deeper }
            val longer = configuration.contingencyLonger.takeIf { diveInput.longer }

            if (deepestIdx != null) {
                segments[deepestIdx] = segments[deepestIdx].let {
                    it.copy(
                        depthInMeters = it.depthInMeters + (deeper ?: 0.0),
                        duration = it.duration + (longer ?: 0),
                    )
                }
            }

            val cylinders = diveInput.cylinders.filter { it.isChecked }.map { it.toAssignedCylinder() }

            // For a CCR dive with bailout enabled, we need two separate plans:
            // - A normal CCR plan (no OC ascent) for accurate gas planning
            // - A bailout plan (OC ascent) for the graph and tissue loading
            // We snapshot the tissues before the normal plan, restore them, then run
            // the bailout plan so the post-bailout tissues carry over to the next dive.
            val (divePlan, gasPlan) = if (diveInput.diveMode.isCcr && diveInput.bailout) {
                val preDiveSnapshot = planner.snapshotTissues()
                val normalPlan = planner.addDive(
                    plan = segments,
                    cylinders = cylinders,
                    diveMode = diveInput.diveMode,
                    bailout = false,
                )

                // Find the worst-case bailout point from the normal CCR plan (longest bailout TTS)
                // and truncate the input profile at that point so the bailout graph shows the
                // ascent from the point with the longest TTS.
                val worstBailoutCandidate = normalPlan.segments.maxByOrNull { it.ttsBailoutAfter ?: 0 }
                val bailoutSegments = if (worstBailoutCandidate != null) {
                    segments.truncateAtRuntime(worstBailoutCandidate.end)
                } else {
                    segments
                }

                planner.restoreTissues(preDiveSnapshot)
                val bailoutPlan = planner.addDive(
                    plan = bailoutSegments,
                    cylinders = cylinders,
                    diveMode = diveInput.diveMode,
                    bailout = true,
                )
                bailoutPlan to gasPlanner.calculateGasPlan(normalPlan)
            } else {
                val plan = planner.addDive(
                    plan = segments,
                    cylinders = cylinders,
                    diveMode = diveInput.diveMode,
                    bailout = diveInput.bailout,
                )
                plan to gasPlanner.calculateGasPlan(plan)
            }

            val deeperDisplay = deeper?.let { unitSystem.metersToDisplayDepth(it).toInt() }

            DivePlanSet(
                base = divePlan,
                deeper = deeperDisplay,
                longer = longer,
                bailout = diveInput.bailout,
                diveMode = diveInput.diveMode,
                gasPlan = gasPlan,
            )
        }

        return MultiDivePlanSet(divePlanSets = sets)
    }
}
