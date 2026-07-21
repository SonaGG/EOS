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
package gg.sona.eos.common

/**
 * Local user login status reported by EOS.
 */
public enum class EosLoginStatus {
    /** No user has logged in or chosen a local profile. */
    NotLoggedIn,

    /** The user is using a local profile but is not logged in. */
    UsingLocalProfile,

    /** The user has been validated by the platform-specific authentication service. */
    LoggedIn;

    public companion object {
        internal fun fromValue(v: Int): EosLoginStatus = when (v) {
            0 -> NotLoggedIn
            1 -> UsingLocalProfile
            2 -> LoggedIn
            else -> NotLoggedIn
        }
    }
}

/**
 * Supported types of data that can be stored as an attribute on sessions and
 * lobbies.
 */
public enum class EosAttributeType {
    Boolean,
    Int64,
    Double,
    String;

    public companion object {
        internal fun fromValue(v: Int): EosAttributeType = when (v) {
            0 -> Boolean
            1 -> Int64
            2 -> Double
            3 -> String
            else -> Boolean
        }
    }
}

/**
 * Comparison operator used for session and lobby attribute searches.
 */
public enum class EosComparisonOp(val value: Int) {
    Equal(0),
    NotEqual(1),
    GreaterThan(2),
    GreaterThanOrEqual(3),
    LessThan(4),
    LessThanOrEqual(5),
    Nearest(6),
    AnyOf(7),
    NotAnyOf(8),
    OneOf(9),
    NotOneOf(10),
    Contains(11),
    MatchesRegex(12),
    Size(13);

    public companion object {
        internal fun fromValue(v: Int): EosComparisonOp = entries.getOrElse(v) { Equal }
    }
}

/**
 * External account provider identifiers supported by EOS.
 */
public enum class EosExternalAccountType {
    Epic,
    Steam,
    Psn,
    XboxLive,
    Discord,
    Gog,
    Nintendo,
    Uplay,
    OpenId,
    Apple,
    Google,
    Oculus,
    Itchio,
    Amazon,
    Viveport;

    public companion object {
        internal fun fromValue(v: Int): EosExternalAccountType = entries.getOrElse(v) { Epic }
    }
}

/**
 * Identity provider used to authenticate a user.
 */
public enum class EosExternalCredentialType(val value: Int) {
    Epic(0),
    SteamAppTicket(1),
    PsnIdToken(2),
    XboxXstsToken(3),
    DiscordAccessToken(4),
    GogSessionTicket(5),
    NintendoIdToken(6),
    NintendoNsaIdToken(7),
    UplayAccessToken(8),
    OpenIdAccessToken(9),
    DeviceIdAccessToken(10),
    AppleIdToken(11),
    GoogleIdToken(12),
    OculusUserIdNonce(13),
    ItchioJwt(14),
    ItchioKey(15),
    EpicIdToken(16),
    AmazonAccessToken(17),
    SteamSessionTicket(18),
    ViveportUserToken(19);

    public companion object {
        internal fun fromValue(v: Int): EosExternalCredentialType = entries.firstOrNull { it.value == v } ?: Epic
    }
}

/**
 * Online platform identifier returned by EOS for the current process.
 */
public enum class EosOnlinePlatform {
    Unknown,
    Epic,
    Psn,
    Nintendo,
    XboxLive,
    Steam;

    public companion object {
        internal fun fromValue(v: Int): EosOnlinePlatform = when (v) {
            100 -> Epic
            1000 -> Psn
            2000 -> Nintendo
            3000 -> XboxLive
            4000 -> Steam
            else -> Unknown
        }
    }
}

/**
 * The application state that the platform should treat the process as being in.
 */
public enum class EosApplicationStatus(val value: Int) {
    /** Xbox only: the application has entered constrained mode. */
    BackgroundConstrained(0),

    /** Xbox only: the application has returned from constrained mode. */
    BackgroundUnconstrained(1),

    /** The application has been suspended. */
    BackgroundSuspended(2),

    /** The application is in the foreground. */
    Foreground(3);

    public companion object {
        public fun fromValue(v: Int): EosApplicationStatus = entries.firstOrNull { it.value == v } ?: Foreground
    }
}

/**
 * Network state as told to the SDK by the application.
 */
public enum class EosNetworkStatus(val value: Int) {
    /** Networking is disabled. */
    Disabled(0),

    /** Offline: not connected to the internet. */
    Offline(1),

    /** Online. */
    Online(2);

    public companion object {
        public fun fromValue(v: Int): EosNetworkStatus = entries.firstOrNull { it.value == v } ?: Offline
    }
}

/**
 * Status of desktop crossplay functionality on Windows.
 */
public enum class EosDesktopCrossplayStatus(val value: Int) {
    /** Desktop crossplay is ready to use. */
    Ok(0),

    /** The application was not launched through the Bootstrapper. */
    ApplicationNotBootstrapped(1),

    /** The redistributable service is not installed. */
    ServiceNotInstalled(2),

    /** The service failed to start. */
    ServiceStartFailed(3),

    /** The service is no longer running. */
    ServiceNotRunning(4),

    /** The overlay is disabled. */
    OverlayDisabled(5),

    /** The overlay is not installed. */
    OverlayNotInstalled(6),

    /** The overlay failed trust check. */
    OverlayTrustCheckFailed(7),

    /** The overlay failed to load. */
    OverlayLoadFailed(8);

    public companion object {
        public fun fromValue(v: Int): EosDesktopCrossplayStatus =
            entries.firstOrNull { it.value == v } ?: Ok
    }
}
