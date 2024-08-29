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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import kotlin.time.measureTimedValue

@OptIn(FlowPreview::class)
@Inject
class PlanScreenViewModel(
    planningRepository: PlanningRepository,
) : ViewModel() {

    private val inputState = MutableStateFlow(defaultDivePlanInputModel)
    private val isCalculatingDivePlan = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)

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
        isCalculatingDivePlan.value = true
        val result = measureTimedValue {
            calculateDivePlan(inputState, configuration)
        }.also {
            isCalculatingDivePlan.value = false
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
        combine(inputState, divePlanSet, isCalculatingDivePlan, isLoading) { input, divePlan, isCalculatingDivePlan, isLoading ->
            ViewState(
                segments = input.plannedProfile,
                availableGas = input.cylinders,
                isCalculatingDivePlan = isCalculatingDivePlan,
                divePlanSet = divePlan,
                isLoading = isLoading,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ViewState()
        )

    init {
        viewModelScope.launch(context = Dispatchers.IO) {

            inputState.value = planningRepository.getDivePlanInput() ?: defaultDivePlanInputModel
            //delay(1000)
            isLoading.value = false
            inputState.debounce(1000).collectLatest {
                try {
                    planningRepository.setDivePlanInput(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateCylinderUsage(segments: List<DiveProfileSection>, cylinders: List<PlannedCylinderModel>): List<PlannedCylinderModel> {
        val inUse = segments.map { it.cylinder }
        return cylinders.map {
            it.copy(isInUse = inUse.contains(it.cylinder))
        }
    }

    fun updateSegment(index: Int, diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.plannedProfile.toMutableList().apply {
                set(index, diveProfileSection)
            }
            it.copy(
                plannedProfile = newSegments,
                cylinders = updateCylinderUsage(newSegments, it.cylinders)
            )
        }
    }

    fun addSegment(diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.plannedProfile.plus(diveProfileSection)
            it.copy(
                plannedProfile = newSegments,
                cylinders = updateCylinderUsage(newSegments, it.cylinders)
            )
        }
    }

    fun removeSegment(index: Int) {
        inputState.update {
            val newSegments = it.plannedProfile.toMutableList().apply {
                removeAt(index)
            }
            it.copy(
                plannedProfile = newSegments,
                cylinders = updateCylinderUsage(newSegments, it.cylinders)
            )
        }
    }

    fun addCylinder(cylinder: Cylinder) {
        inputState.update {
            it.copy(
                cylinders = it.cylinders.plus(
                    PlannedCylinderModel(
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
            val index = inputState.cylinders.indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
            val item = inputState.cylinders[index]
            inputState.copy(
                cylinders = inputState.cylinders.toMutableList().apply {
                    set(index, item.copy(cylinder = cylinder))
                },
                plannedProfile = inputState.plannedProfile.map {
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
            if (inputState.plannedProfile.any { it.cylinder == cylinder }) {
                // Cylinder is in used, do not allow toggle.
                inputState
            } else {
                inputState.copy(
                    cylinders = inputState.cylinders.map { pair ->
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
            if (inputState.plannedProfile.any { it.cylinder == gas }) {
                // Cylinder is in used, do not allow removal.
                inputState
            } else {
                inputState.copy(
                    cylinders = inputState.cylinders.toMutableList().apply {
                        removeAll { pair -> pair.cylinder == gas }
                    }
                )
            }
        }
    }

    private suspend fun calculateDivePlan(inputState: DivePlanInputModel, configuration: Configuration, ): Result<DivePlanSet> {
        return try {
            val timedResult = measureTimedValue {
                val planner = DivePlanner()
                planner.configuration = configuration

                val segmentsAdjusted = inputState.plannedProfile.toMutableList()
                val index = segmentsAdjusted.indices.maxByOrNull { segmentsAdjusted[it].depth }
                val deepestSegment = index?.let { inputState.plannedProfile[it] }

                val deeper = configuration.contingencyDeeper.takeIf { inputState.deeper }
                val longer = configuration.contingencyLonger.takeIf { inputState.longer }

                if (deepestSegment != null) {
                    segmentsAdjusted[index] = deepestSegment.copy(
                        depth = deepestSegment.depth + (deeper ?: 0),
                        duration = deepestSegment.duration + (longer ?: 0)
                    )
                }

                val decoGasses = inputState.cylinders.filter { it.isChecked }.map { it.cylinder }

                val adjustedPlan = planner.getDecoPlan(
                    plan = segmentsAdjusted,
                    decoGases = decoGasses,
                )

                val gasPlan = GasPlanner().calculateGasPlan(adjustedPlan)

                Result.success(
                    DivePlanSet(
                        base = adjustedPlan,
                        deeper = deeper,
                        longer = longer,
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

    fun setContingency(deeper: Boolean, longer: Boolean) {
        inputState.update { inputState ->
            inputState.copy(
                deeper = deeper,
                longer = longer,
            )
        }
    }

    data class ViewState(
        val segments: List<DiveProfileSection> = defaultProfile,
        val availableGas: List<PlannedCylinderModel> = defaultCylinders,
        val divePlanSet: Result<DivePlanSet?> = Result.success(null),
        val isCalculatingDivePlan: Boolean = false,
        val isLoading: Boolean = true,
    )
}

// TODO consider removing defaults from the production version of the app.
private val defaultCylinderAir = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)

// TODO consider removing defaults from the production version of the app.
private val defaultCylinders: List<PlannedCylinderModel> = listOf(
    PlannedCylinderModel(
        cylinder = defaultCylinderAir,
        isInUse = true,
        isChecked = true
    ),
    PlannedCylinderModel(
        cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0),
        isInUse = false,
        isChecked = true
    ),
    PlannedCylinderModel(
        cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80, pressure = 207.0),
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

private val defaultDivePlanInputModel = DivePlanInputModel(
    deeper = false,
    longer = false,
    plannedProfile = defaultProfile,
    cylinders = defaultCylinders,
)


private const val SUBSCRIPTION_TIME_OUT: Long = 5 * 60 * 1000
