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
package gg.sona.eos

import gg.sona.eos.achievements.EosAchievements
import gg.sona.eos.anticheat.client.EosAntiCheatClient
import gg.sona.eos.anticheat.server.EosAntiCheatServer
import gg.sona.eos.auth.EosAuth
import gg.sona.eos.common.EosApplicationStatus
import gg.sona.eos.common.EosNetworkStatus
import gg.sona.eos.connect.EosConnect
import gg.sona.eos.custominvites.EosCustomInvites
import gg.sona.eos.ecom.EosEcom
import gg.sona.eos.friends.EosFriends
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.withCallArena
import gg.sona.eos.integratedplatform.EosIntegratedPlatform
import gg.sona.eos.kws.EosKws
import gg.sona.eos.leaderboards.EosLeaderboards
import gg.sona.eos.lobby.EosLobby
import gg.sona.eos.metrics.EosMetrics
import gg.sona.eos.mods.EosMods
import gg.sona.eos.p2p.EosP2P
import gg.sona.eos.playerdatastorage.EosPlayerDataStorage
import gg.sona.eos.presence.EosPresence
import gg.sona.eos.progressionsnapshot.EosProgressionSnapshot
import gg.sona.eos.reports.EosReports
import gg.sona.eos.rtc.EosRtc
import gg.sona.eos.sanctions.EosSanctions
import gg.sona.eos.sessions.EosSessions
import gg.sona.eos.stats.EosStats
import gg.sona.eos.titlestorage.EosTitleStorage
import gg.sona.eos.ui.EosUi
import gg.sona.eos.userinfo.EosUserInfo
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.ValueLayout
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a single Epic Online Services platform instance.
 *
 * Construct one with [Eos.createPlatform], drive it with [tick], and dispose
 * of it with [release]. Each subsystem interface ([auth], [connect], [p2p],
 * etc.) is obtained as a property; these are lightweight accessors.
 */
public class EosPlatform internal constructor(
    @JvmField internal val handle: Long,
) : AutoCloseable {

    private val released = AtomicBoolean(false)

    /** True after [release] has been called. */
    public val isReleased: Boolean get() = released.get()

    /**
     * Tick the SDK. Call frequently, typically once per game tick, to allow
     * EOS to process callbacks, network traffic, and so on.
     */
    public fun tick() {
        check(!released.get()) { "Platform has been released" }
        val fn = Native.downcall("EOS_Platform_Tick", FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG))
        fn.invokeExact(handle)
    }

    /**
     * Release the platform instance. Must be called exactly once for each
     * platform instance, and before [Eos.shutdown].
     */
    override fun close() {
        if (released.compareAndSet(false, true)) {
            val fn = Native.downcall(
                "EOS_Platform_Release",
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)
            )
            fn.invokeExact(handle)
            CallbackStubs.releaseAll()
        }
    }

    // region Subsystem accessors

    /** Auth interface (Epic Account login, persistent auth, etc.). */
    public val auth: EosAuth get() = EosAuth(this)

    /** Connect interface (external account login, device id, account mapping). */
    public val connect: EosConnect get() = EosConnect(this)

    /** Ecom interface (offers, entitlements, checkout). */
    public val ecom: EosEcom get() = EosEcom(this)

    /** UI interface (overlay, social overlay, friends list, etc.). */
    public val ui: EosUi get() = EosUi(this)

    /** Friends interface (friend list, invites, blocking). */
    public val friends: EosFriends get() = EosFriends(this)

    /** Presence interface (status, rich presence). */
    public val presence: EosPresence get() = EosPresence(this)

    /** Sessions interface (match-making, game sessions). */
    public val sessions: EosSessions get() = EosSessions(this)

    /** Lobby interface (persistent lobbies with built-in voice support). */
    public val lobby: EosLobby get() = EosLobby(this)

    /** UserInfo interface (display name, locale). */
    public val userInfo: EosUserInfo get() = EosUserInfo(this)

    /** P2P interface (NAT punching, reliable/unreliable packet sending). */
    public val p2p: EosP2P get() = EosP2P(this)

    /** RTC interface (voice chat). */
    public val rtc: EosRtc get() = EosRtc(this)

    /** Player data storage interface (per-user cloud saves). */
    public val playerDataStorage: EosPlayerDataStorage get() = EosPlayerDataStorage(this)

    /** Title storage interface (game-wide shared cloud files). */
    public val titleStorage: EosTitleStorage get() = EosTitleStorage(this)

    /** Achievements interface. */
    public val achievements: EosAchievements get() = EosAchievements(this)

    /** Stats interface. */
    public val stats: EosStats get() = EosStats(this)

    /** Leaderboards interface. */
    public val leaderboards: EosLeaderboards get() = EosLeaderboards(this)

    /** Mods interface. */
    public val mods: EosMods get() = EosMods(this)

    /** Anti-Cheat Client interface. */
    public val antiCheatClient: EosAntiCheatClient get() = EosAntiCheatClient(this)

    /** Anti-Cheat Server interface. */
    public val antiCheatServer: EosAntiCheatServer get() = EosAntiCheatServer(this)

    /** Reports interface. */
    public val reports: EosReports get() = EosReports(this)

    /** Sanctions interface. */
    public val sanctions: EosSanctions get() = EosSanctions(this)

    /** Kids Web Services interface. */
    public val kws: EosKws get() = EosKws(this)

    /** Custom Invites interface. */
    public val customInvites: EosCustomInvites get() = EosCustomInvites(this)

    /** Progression Snapshot interface. */
    public val progressionSnapshot: EosProgressionSnapshot get() = EosProgressionSnapshot(this)

    /** Metrics interface. */
    public val metrics: EosMetrics get() = EosMetrics(this)

    /** Integrated Platform interface. */
    public val integratedPlatform: EosIntegratedPlatform get() = EosIntegratedPlatform(this)

    // endregion

    // region State management

    /**
     * Notify the SDK of an application status change (background/foreground).
     * Call this before [tick] when foregrounding.
     */
    public fun setApplicationStatus(status: EosApplicationStatus): EosResult {
        val fn = Native.downcall(
            "EOS_Platform_SetApplicationStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )
        return EosResult.fromValue(fn.invokeExact(handle, status.value) as Int)
    }

    /** Get the current application status as told to the SDK. */
    public fun getApplicationStatus(): EosApplicationStatus {
        val fn = Native.downcall(
            "EOS_Platform_GetApplicationStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return EosApplicationStatus.fromValue(fn.invokeExact(handle) as Int)
    }

    /** Notify the SDK of a network status change. */
    public fun setNetworkStatus(status: EosNetworkStatus): EosResult {
        val fn = Native.downcall(
            "EOS_Platform_SetNetworkStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )
        return EosResult.fromValue(fn.invokeExact(handle, status.value) as Int)
    }

    /** Get the current network status as told to the SDK. */
    public fun getNetworkStatus(): EosNetworkStatus {
        val fn = Native.downcall(
            "EOS_Platform_GetNetworkStatus",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return EosNetworkStatus.fromValue(fn.invokeExact(handle) as Int)
    }

    /**
     * If the app was not launched through the Epic Games Launcher, ask the
     * SDK to relaunch it. Returns [EosResult.Success] if a relaunch was
     * triggered (in which case the calling process should exit), or
     * [EosResult.NoChange] if already launched through the launcher.
     */
    public fun checkForLauncherAndRestart(): EosResult {
        val fn = Native.downcall(
            "EOS_Platform_CheckForLauncherAndRestart",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
        )
        return EosResult.fromValue(fn.invokeExact(handle) as Int)
    }

    /** Get the active country code for a user. */
    public fun getActiveCountryCode(userId: gg.sona.eos.common.EpicAccountId): String? = withCallArena { arena ->
        val sizePtr = arena.allocate(ValueLayout.JAVA_INT)
        val fn = Native.downcall(
            "EOS_Platform_GetActiveCountryCode",
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            )
        )
        val result = fn.invokeExact(handle, userId.raw, java.lang.foreign.MemorySegment.NULL, sizePtr) as Int
        if (result != 0) return@withCallArena null
        val size = sizePtr.get(ValueLayout.JAVA_INT, 0)
        val buf = arena.allocate(size.toLong())
        sizePtr.set(ValueLayout.JAVA_INT, 0, size)
        fn.invokeExact(handle, userId.raw, buf, sizePtr)
        buf.getString(0)
    }

    // endregion
}
