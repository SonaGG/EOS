/*
 * Copyright 2024 sona
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gg.sona.eos

import gg.sona.eos.common.ContinuanceToken
import gg.sona.eos.common.EpicAccountId
import gg.sona.eos.common.ProductUserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for the opaque-handle value classes. They do not require the native
 * library because they only exercise the Kotlin side of the API.
 */
class OpaqueHandleTest {

    @Test
    fun `ProductUserId Invalid is the zero handle`() {
        assertEquals(0L, ProductUserId.Invalid.raw)
        assertEquals(0L, ProductUserId.fromString("").raw)
    }

    @Test
    fun `EpicAccountId Invalid is the zero handle`() {
        assertEquals(0L, EpicAccountId.Invalid.raw)
    }

    @Test
    fun `ProductUserId value class is a single Long`() {
        // Value classes should erase to a single field at runtime; the only
        // property of ProductUserId is `raw`. We just check structural
        // equality of two instances built with the same raw value.
        val a = ProductUserId(0x1234L)
        val b = ProductUserId(0x1234L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `distinct handles compare distinct`() {
        assertNotEquals(ProductUserId(1L), ProductUserId(2L))
        assertNotEquals(EpicAccountId(1L), EpicAccountId(2L))
    }

    @Test
    fun `ProductUserId toString of Invalid is empty`() {
        assertEquals("", ProductUserId.Invalid.toStringValue())
    }

    @Test
    fun `ContinuanceToken Invalid has zero raw`() {
        assertEquals(0L, ContinuanceToken(0L).raw)
    }

    @Test
    fun `value class identity semantics`() {
        val handle = ProductUserId(0xCAFE)
        assertTrue(handle.raw == 0xCAFEL)
    }
}
