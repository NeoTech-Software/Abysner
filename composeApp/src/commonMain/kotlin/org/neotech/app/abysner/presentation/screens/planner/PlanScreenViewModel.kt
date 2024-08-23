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

package org.neotech.app.abysner.presentation.screens.planner

import androidx.compose.material3.Text
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import kotlin.time.measureTimedValue

@Inject
class PlanScreenViewModel(
    planningRepository: PlanningRepository,
) : ViewModel() {

    private val inputState = MutableStateFlow(InputState())
    private val isLoading = MutableStateFlow(false)

    /**
     * The downside of using combine on a StateFlow is that it returns a cold flow, meaning that
     * everytime it gains a subscriber the `transform` block will run again. If combine would return
     * a StateFlow as well... then this `transform` block would not need execution again, because the
     * result is cached.
     */
    private val divePlanSet: StateFlow<Result<DivePlanSet?>> = combine(
        flow = inputState,
        flow2 = planningRepository.configuration
    ) { inputState, configuration ->
        // For now it seems the recalculation is so fast, that showing a loading state is kinda pointless.
        // Maybe instead of a full loading status, a small indicator somewhere can show whether or
        // not recalculation is happening?
        isLoading.value = true
        val result = measureTimedValue {
            calculateDivePlan(inputState, configuration)
        }.also {
            isLoading.value = false
        }
        println("Duration: Calculating dive plan took ${result.duration}")
        result.value
    }.flowOn(Dispatchers.IO)
        // This calculation is quite heavy, we don't want to re-run it when in a short time period the
        // last subscription ends after which a new subscription appears (in a short time period).
        // However we also do not want to keep subscribed, since that could trigger calculations in
        // the background (while the user is not on this screen), which is also wasteful.
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_TIME_OUT),
            Result.success(null)
        )

    val uiState: StateFlow<ViewState> =
        combine(inputState, divePlanSet, isLoading) { input, divePlan, isLoading ->
            ViewState(
                segments = input.segments,
                availableGas = input.availableCylinders,
                isLoading = isLoading,
                divePlanSet = divePlan
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ViewState()
        )

    private fun updateCylinderUsage(segments: List<DiveProfileSection>, cylinders: List<CylinderStatusUiModel>): List<CylinderStatusUiModel> {
        val inUse = segments.map { it.cylinder }
        return cylinders.map {
            it.copy(isInUse = inUse.contains(it.cylinder))
        }
    }

    fun updateSegment(index: Int, diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.segments.toMutableList().apply {
                set(index, diveProfileSection)
            }
            it.copy(
                segments = newSegments,
                availableCylinders = updateCylinderUsage(newSegments, it.availableCylinders)
            )
        }
    }

    fun addSegment(diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.segments.plus(diveProfileSection)
            it.copy(
                segments = newSegments,
                availableCylinders = updateCylinderUsage(newSegments, it.availableCylinders)
            )
        }
    }

    fun removeSegment(index: Int) {
        inputState.update {
            val newSegments = it.segments.toMutableList().apply {
                removeAt(index)
            }
            it.copy(
                segments = newSegments,
                availableCylinders = updateCylinderUsage(newSegments, it.availableCylinders)
            )
        }
    }

    fun addCylinder(cylinder: Cylinder) {
        inputState.update {
            it.copy(
                availableCylinders = it.availableCylinders.plus(
                    CylinderStatusUiModel(
                        cylinder = cylinder,
                        isChecked = false,
                        isInUse = false
                    )
                ).sortedBy { gas -> gas.cylinder.gas.oxygenFraction }
            )
        }
    }

    fun updateCylinder(cylinder: Cylinder) {
        inputState.update { inputState ->
            val index = inputState.availableCylinders.indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
            val item = inputState.availableCylinders[index]
            inputState.copy(
                availableCylinders = inputState.availableCylinders.toMutableList().apply {
                    set(index, item.copy(cylinder = cylinder))
                },
                segments = inputState.segments.map {
                    if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) {
                        it.copy(cylinder = cylinder)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun toggleCylinder(cylinder: Cylinder, enabled: Boolean) {
        inputState.update { inputState ->
            if (inputState.segments.any { it.cylinder == cylinder }) {
                // Cylinder is in used, do not allow toggle.
                inputState
            } else {
                inputState.copy(
                    availableCylinders = inputState.availableCylinders.map { pair ->
                        if (pair.cylinder == cylinder) {
                            pair.copy(isChecked = enabled)
                        } else {
                            pair
                        }
                    }
                )
            }
        }
    }

    fun removeCylinder(gas: Cylinder) {
        inputState.update { inputState ->
            if (inputState.segments.any { it.cylinder == gas }) {
                // Cylinder is in used, do not allow removal.
                inputState
            } else {
                inputState.copy(
                    availableCylinders = inputState.availableCylinders.toMutableList().apply {
                        removeAll { pair -> pair.cylinder == gas }
                    }
                )
            }
        }
    }

    private suspend fun calculateDivePlan(inputState: InputState, configuration: Configuration, ): Result<DivePlanSet> {
        return try {
            val timedResult = measureTimedValue {
                val planner = DivePlanner()
                planner.configuration = configuration

                val segmentsDeeperAndLonger = inputState.segments.toMutableList()
                val index =
                    segmentsDeeperAndLonger.indices.maxByOrNull { segmentsDeeperAndLonger[it].depth }
                val deepestSegment = index?.let { inputState.segments[it] }

                if (deepestSegment != null) {
                    segmentsDeeperAndLonger[index] = deepestSegment.copy(
                        depth = deepestSegment.depth + configuration.contingencyDeeper,
                        duration = deepestSegment.duration + configuration.contingencyLonger
                    )
                }

                val decoGasses = inputState.availableCylinders.filter { it.isChecked }.map { it.cylinder }

                val basePlan = planner.getDecoPlan(
                    plan = inputState.segments,
                    decoGases = decoGasses,
                )

                val deeperAndLongerPlan = planner.getDecoPlan(
                    plan = segmentsDeeperAndLonger,
                    decoGases = decoGasses,
                )

                val gasPlan = GasPlanner().calculateGasPlan(deeperAndLongerPlan)

                Result.success(
                    DivePlanSet(
                        base = basePlan,
                        deeperAndLonger = deeperAndLongerPlan,
                        gasPlan = gasPlan,
                    )
                )
            }
            println("Dive plan calculation took: ${timedResult.duration.inWholeMilliseconds}ms")
            timedResult.value
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    data class InputState(
        val segments: List<DiveProfileSection> = defaultProfile,
        val availableCylinders: List<CylinderStatusUiModel> = defaultCylinders,
    )

    data class ViewState(
        val segments: List<DiveProfileSection> = defaultProfile,
        val availableGas: List<CylinderStatusUiModel> = defaultCylinders,
        val divePlanSet: Result<DivePlanSet?> = Result.success(null),
        val isLoading: Boolean = false
    )
}

data class CylinderStatusUiModel(
    val cylinder: Cylinder,
    val isInUse: Boolean,
    val isChecked: Boolean,
)

// TODO consider removing defaults from the production version of the app.
private val defaultCylinderAir = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)

// TODO consider removing defaults from the production version of the app.
private val defaultCylinders: List<CylinderStatusUiModel> = listOf(
    CylinderStatusUiModel(
        cylinder = defaultCylinderAir,
        isInUse = true,
        isChecked = true
    ),
    CylinderStatusUiModel(
        cylinder = Cylinder.aluminium80Cuft(gas = Gas.Oxygen50, pressure = 207.0),
        isInUse = false,
        isChecked = true
    ),
    CylinderStatusUiModel(
        cylinder = Cylinder.aluminium63Cuft(gas = Gas.Oxygen80, pressure = 207.0),
        isInUse = false,
        isChecked = false
    )
)

// TODO consider removing defaults from the production version of the app.
private val defaultProfile = listOf(
    DiveProfileSection(
        30,
        25,
        defaultCylinderAir
    )
)

private const val SUBSCRIPTION_TIME_OUT: Long = 5 * 60 * 1000
