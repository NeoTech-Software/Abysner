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

package org.neotech.app.abysner.presentation.utilities

import androidx.compose.runtime.Composable

/**
 * Same as [Event] but allows an event to have state when [Unconsumed].
 */
sealed class StateEvent<T> {

    data class Unconsumed<T>(val content: T, val onConsumed: () -> Unit): StateEvent<T>()

    class Consumed<T>: StateEvent<T>()
}

/**
 * In modern Android development it is common practice to have events part of the view state.
 *
 * As an example:
 * To navigate somewhere once you would add a boolean to the view state, that when true
 * triggers the navigation action. After the navigation action is handled you would call a method on
 * the ViewModel to tell the ViewModel that the navigation action has been completed, after which
 * the ViewModel updates the view state, setting the boolean to false.
 *
 * The above causes quite a lot of boiler plate, this Event class tries to solve that by still leaving
 * the event in the view state while also leaving the responsibility for updating the state in the
 * ViewModel.
 *
 * [EventEffect] is used in the view layer to consume the event, it calls the [Unconsumed.consume]
 * method on the event after handling it. [Unconsumed.consume] is set by the ViewModel and causes
 * logic in the ViewModel to run. Essentially [Unconsumed.consume] is the method in the above example
 * that needs to be called to set the state back to false.
 */
sealed class Event {

    data class Unconsumed(val consume: () -> Unit): Event()

    data object Consumed: Event()
}

fun event(onConsumed: () -> Unit): Event = Event.Unconsumed(onConsumed)

fun <T> event(content: T, onConsumed: () -> Unit): StateEvent<T> =
    StateEvent.Unconsumed(content, onConsumed)

fun consumed(): Event = Event.Consumed

fun <T> consumed(): StateEvent<T> = StateEvent.Consumed()


@Composable
fun EventEffect(event: Event, onEvent: () -> Unit) {
    if(event is Event.Unconsumed) {
        onEvent()
        event.consume()
    }
}

@Composable
fun <T> EventEffect(event: StateEvent<T>, onEvent: (T) -> Unit) {
    if(event is StateEvent.Unconsumed) {
        onEvent(event.content)
        event.onConsumed()
    }
}

