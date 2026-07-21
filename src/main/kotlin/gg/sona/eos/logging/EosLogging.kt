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
package gg.sona.eos.logging

import gg.sona.eos.EosResult
import gg.sona.eos.internal.CallbackStubs
import gg.sona.eos.internal.EosCallback
import gg.sona.eos.internal.Native
import gg.sona.eos.internal.getInt32
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** Verbosity of log messages. Higher values mean more chatty logs. */
public enum class EosLogLevel(val value: Int) {
    Off(0),
    Fatal(100),
    Error(200),
    Warning(300),
    Info(400),
    Verbose(500),
    VeryVerbose(600);

    public companion object {
        public fun fromValue(v: Int): EosLogLevel = entries.firstOrNull { it.value == v } ?: Off
    }
}

/** Category of an EOS log message. */
public enum class EosLogCategory(val value: Int) {
    AllCategories(0x7fffffff),
    Core(0),
    Auth(1),
    Friends(2),
    Presence(3),
    UserInfo(4),
    HttpSerialization(5),
    Ecom(6),
    P2P(7),
    Sessions(8),
    RateLimiter(9),
    PlayerDataStorage(10),
    Analytics(11),
    Messaging(12),
    Connect(13),
    Overlay(14),
    Achievements(15),
    Stats(16),
    UI(17),
    Lobby(18),
    Leaderboards(19),
    Keychain(20),
    IntegratedPlatform(21),
    TitleStorage(22),
    Mods(23),
    AntiCheat(24),
    Reports(25),
    Sanctions(26),
    ProgressionSnapshots(27),
    KWS(28),
    RTC(29),
    RTCAdmin(30),
    CustomInvites(31),
    HTTP(41);

    public companion object {
        public fun fromValue(v: Int): EosLogCategory = entries.firstOrNull { it.value == v } ?: AllCategories
    }
}

/** A log message emitted by the SDK. */
public class EosLogMessage(
    public val category: String,
    public val message: String,
    public val level: EosLogLevel,
)

/** Callback that receives log messages from the SDK. */
public fun interface EosLogMessageFunc {
    public fun onLogMessage(message: EosLogMessage)
}

/**
 * The Logging interface allows registering a callback to receive log messages
 * from the SDK, and to control the verbosity per category.
 */
public object EosLogging {

    private var currentCallback: EosLogMessageFunc? = null
    private var currentCallbackId: Long = -1L
    private val callbackLock = Any()

    /**
     * Set the callback function to use for SDK log messages. Any previously
     * set callback will no longer be called. The supplied callback must
     * remain valid until replaced.
     */
    public fun setCallback(callback: EosLogMessageFunc?): EosResult {
        synchronized(callbackLock) {
            if (currentCallbackId != -1L) {
                CallbackStubs.release(currentCallbackId)
                currentCallbackId = -1L
            }
            currentCallback = callback
            if (callback == null) {
                val fn = Native.downcall(
                    "EOS_Logging_SetCallback",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
                )
                return EosResult.fromValue(fn.invokeExact(MemorySegment.NULL) as Int)
            }
            val invoker = EosCallback { rawData ->
                val data = rawData.reinterpret(24)
                val categoryPtr = data.get(ValueLayout.ADDRESS, 0)
                val category = if (categoryPtr.address() == 0L) "" else
                    categoryPtr.reinterpret(Long.MAX_VALUE).getString(0)
                val messagePtr = data.get(ValueLayout.ADDRESS, 8)
                val message = if (messagePtr.address() == 0L) "" else
                    messagePtr.reinterpret(Long.MAX_VALUE).getString(0)
                val level = EosLogLevel.fromValue(data.getInt32(16))
                callback.onLogMessage(EosLogMessage(category, message, level))
            }
            val stub = CallbackStubs.register(invoker)
            currentCallbackId = stub.id
            val stubSeg = CallbackStubs.getStubSegment(currentCallbackId)
                ?: error("Failed to register logging callback")
            val setter = Native.downcall(
                "EOS_Logging_SetCallback",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            )
            return EosResult.fromValue(setter.invokeExact(stubSeg) as Int)
        }
    }

    /**
     * Set the log level for a category. The default is `Warning` for all
     * categories. Use [EosLogCategory.AllCategories] to apply to all at once.
     */
    public fun setLogLevel(category: EosLogCategory, level: EosLogLevel): EosResult {
        val fn = Native.downcall(
            "EOS_Logging_SetLogLevel",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
        )
        return EosResult.fromValue(fn.invokeExact(category.value, level.value) as Int)
    }
}
