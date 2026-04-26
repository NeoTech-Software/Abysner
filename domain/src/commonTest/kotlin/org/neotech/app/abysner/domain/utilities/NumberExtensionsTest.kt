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

package org.neotech.app.abysner.domain.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberExtensionsTest {

    @Test
    fun equalsTolerant_trueForExactlyEqualValues() {
        assertTrue(5.0.equalsTolerant(5.0))
    }

    @Test
    fun equalsTolerant_trueForDifferenceWithinTolerance() {
        assertTrue(5.0.equalsTolerant(5.0 + 1e-7))
    }

    @Test
    fun equalsTolerant_falseForDifferenceOutsideTolerance() {
        assertFalse(5.0.equalsTolerant(5.0 + 1e-5))
    }

    @Test
    fun greaterThanTolerant_falseWhenWithinTolerance() {
        assertFalse((5.0 + 1e-7).greaterThanTolerant(5.0))
    }

    @Test
    fun greaterThanTolerant_trueWhenOutsideTolerance() {
        assertTrue((5.0 + 1e-5).greaterThanTolerant(5.0))
    }

    @Test
    fun greaterThanOrEqualTolerant_trueWhenWithinTolerance() {
        assertTrue((5.0 - 1e-7).greaterThanOrEqualTolerant(5.0))
    }

    @Test
    fun greaterThanOrEqualTolerant_falseWhenOutsideTolerance() {
        assertFalse((5.0 - 1e-5).greaterThanOrEqualTolerant(5.0))
    }

    @Test
    fun lessThanTolerant_falseWhenWithinTolerance() {
        assertFalse((5.0 - 1e-7).lessThanTolerant(5.0))
    }

    @Test
    fun lessThanTolerant_trueWhenOutsideTolerance() {
        assertTrue((5.0 - 1e-5).lessThanTolerant(5.0))
    }

    @Test
    fun lessThanOrEqualTolerant_trueWhenWithinTolerance() {
        assertTrue((5.0 + 1e-7).lessThanOrEqualTolerant(5.0))
    }

    @Test
    fun lessThanOrEqualTolerant_falseWhenOutsideTolerance() {
        assertFalse((5.0 + 1e-5).lessThanOrEqualTolerant(5.0))
    }

    @Test
    fun ceilTolerant_floorsWhenWithinTolerance() {
        assertEquals(9.0, ceilTolerant(9.0000000002))
    }

    @Test
    fun ceilTolerant_ceilsWhenOutsideTolerance() {
        assertEquals(10.0, ceilTolerant(9.5))
    }

    @Test
    fun ceilTolerant_returnsExactInteger() {
        assertEquals(9.0, ceilTolerant(9.0))
    }

    @Test
    fun floorTolerant_ceilsWhenWithinTolerance() {
        assertEquals(3.0, floorTolerant(2.9999999998))
    }

    @Test
    fun floorTolerant_floorsWhenOutsideTolerance() {
        assertEquals(2.0, floorTolerant(2.5))
    }

    @Test
    fun floorTolerant_returnsExactInteger() {
        assertEquals(3.0, floorTolerant(3.0))
    }

    @Test
    fun removeFloatingPointNoise_removesNoiseWithinTolerance() {
        assertEquals(6.0, removeFloatingPointNoise(6.000000000000001))
    }

    @Test
    fun removeFloatingPointNoise_preservesDecimalsOutsideTolerance() {
        assertEquals(6.123456, removeFloatingPointNoise(6.123456))
    }
}
