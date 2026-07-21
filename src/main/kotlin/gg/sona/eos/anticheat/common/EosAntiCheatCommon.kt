/*
 * Copyright 2026 Sona Softworks LLC
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
package gg.sona.eos.anticheat.common

import gg.sona.eos.internal.Native
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout

/** Common Anti-Cheat type definitions shared between client and server. */
public object EosAntiCheatCommon {

    /**
     * Local identifier for a remote client/peer. The exact value is opaque
     * to EOS, only used to refer back to the registered client.
     */
    @JvmInline
    public value class ClientHandle(public val raw: Long) {
        public fun isValid(): Boolean = raw != 0L && raw != SELF.raw

        public companion object {
            /** The sentinel handle referring to the local client itself. */
            public val SELF: ClientHandle = ClientHandle(-1L)

            public val Invalid: ClientHandle = ClientHandle(0L)
        }
    }

    /** Type of a remote client. */
    public enum class ClientType(val value: Int) {
        ProtectedClient(0),
        UnprotectedClient(1),
        AiBot(2);

        public companion object {
            public fun fromValue(v: Int): ClientType = entries.firstOrNull { it.value == v } ?: ProtectedClient
        }
    }

    /** Platform the remote client is on, if known. */
    public enum class ClientPlatform(val value: Int) {
        Unknown(0),
        Windows(1),
        Mac(2),
        Linux(3),
        Xbox(4),
        PlayStation(5),
        Nintendo(6),
        Ios(7),
        Android(8);

        public companion object {
            public fun fromValue(v: Int): ClientPlatform = entries.firstOrNull { it.value == v } ?: Unknown
        }
    }

    /** Actions that may be required for a client/peer. */
    public enum class ClientAction(val value: Int) {
        Invalid(0),
        RemovePlayer(1);

        public companion object {
            public fun fromValue(v: Int): ClientAction = entries.firstOrNull { it.value == v } ?: Invalid
        }
    }

    /** Reasons for a client/peer action. */
    public enum class ClientActionReason(val value: Int) {
        Invalid(0),
        InternalError(1),
        InvalidMessage(2),
        AuthenticationFailed(3),
        NullClient(4),
        HeartbeatTimeout(5),
        ClientViolation(6),
        BackendViolation(7),
        TemporaryCooldown(8),
        TemporaryBanned(9),
        PermanentBanned(10);

        public companion object {
            public fun fromValue(v: Int): ClientActionReason =
                entries.firstOrNull { it.value == v } ?: Invalid
        }
    }

    /** Authentication status of a client/peer. */
    public enum class ClientAuthStatus(val value: Int) {
        Invalid(0),
        LocalAuthComplete(1),
        RemoteAuthComplete(2);

        public companion object {
            public fun fromValue(v: Int): ClientAuthStatus =
                entries.firstOrNull { it.value == v } ?: Invalid
        }
    }

    /** Flags describing a remote client. */
    public enum class ClientFlags(val value: Int) {
        None(0),
        Admin(1);

        public companion object {
            public fun fromValue(v: Int): Set<ClientFlags> = entries.filter { v and it.value != 0 }.toSet()
        }
    }

    /** Input device used by a remote client. */
    public enum class ClientInput(val value: Int) {
        Unknown(0),
        MouseKeyboard(1),
        Gamepad(2),
        TouchInput(3);

        public companion object {
            public fun fromValue(v: Int): ClientInput = entries.firstOrNull { it.value == v } ?: Unknown
        }
    }

    /** Type of a custom gameplay event. */
    public enum class EventType(val value: Int) {
        Invalid(0),
        GameEvent(1),
        PlayerEvent(2);

        public companion object {
            public fun fromValue(v: Int): EventType = entries.firstOrNull { it.value == v } ?: Invalid
        }
    }

    /** Type of a custom event parameter. */
    public enum class EventParamType(val value: Int) {
        Invalid(0),
        ClientHandle(1),
        String(2),
        UInt32(3),
        Int32(4),
        UInt64(5),
        Int64(6),
        Vector3f(7),
        Quat(8),
        Float(9);

        public companion object {
            public fun fromValue(v: Int): EventParamType = entries.firstOrNull { it.value == v } ?: Invalid
        }
    }

    /** Competition type for a game round. */
    public enum class GameRoundCompetitionType(val value: Int) {
        None(0),
        Casual(1),
        Ranked(2),
        Competitive(3);

        public companion object {
            public fun fromValue(v: Int): GameRoundCompetitionType =
                entries.firstOrNull { it.value == v } ?: None
        }
    }

    /** Player movement state. */
    public enum class PlayerMovementState(val value: Int) {
        None(0),
        Crouching(1),
        Prone(2),
        Mounted(3),
        Swimming(4),
        Falling(5),
        Flying(6),
        OnLadder(7);

        public companion object {
            public fun fromValue(v: Int): PlayerMovementState =
                entries.firstOrNull { it.value == v } ?: None
        }
    }

    /** Source of a damage event. */
    public enum class PlayerTakeDamageSource(val value: Int) {
        None(0),
        Player(1),
        NonPlayerCharacter(2),
        World(3);

        public companion object {
            public fun fromValue(v: Int): PlayerTakeDamageSource =
                entries.firstOrNull { it.value == v } ?: None
        }
    }

    /** Type of damage applied. */
    public enum class PlayerTakeDamageType(val value: Int) {
        None(0),
        PointDamage(1),
        RadialDamage(2),
        DamageOverTime(3);

        public companion object {
            public fun fromValue(v: Int): PlayerTakeDamageType =
                entries.firstOrNull { it.value == v } ?: None
        }
    }

    /** Result of a damage event. */
    public enum class PlayerTakeDamageResult(val value: Int) {
        None(0),
        NormalToDowned(3),
        NormalToEliminated(4),
        DownedToEliminated(5);

        public companion object {
            public fun fromValue(v: Int): PlayerTakeDamageResult =
                entries.firstOrNull { it.value == v } ?: None
        }
    }

    /** Left-handed 3D vector (matches Unreal Engine convention). */
    public class Vec3f(public val x: Float, public val y: Float, public val z: Float)

    /** Left-handed quaternion. */
    public class Quat(public val w: Float, public val x: Float, public val y: Float, public val z: Float)
}
