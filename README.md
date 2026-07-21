# EOS Kotlin Bindings

Kotlin bindings for the Epic Online Services (EOS) SDK, version 1.19.1.2.

The bindings cover every subsystem of the C SDK with an idiomatic Kotlin
API. Async EOS calls return a `CompletableFuture`. One-shot results return
an `EosResult`. Callbacks are exposed as Kotlin function types.

## Highlights

- Uses the JDK 25 Foreign Function & Memory API for native interop. No
  JNI is generated and no third-party library is required.
- All public types use `value class` where possible, so the JIT can erase
  them at runtime.
- Subsystems that are commonly grouped together (P2P, RTC, Anti-Cheat) are
  fully implemented including notification registration and message
  transport.
- The Python helper `scripts/codegen.py` is provided to partially
  automate the generation of new struct types from the C headers.

## Quick start

```kotlin
import gg.sona.eos.*
import gg.sona.eos.common.*

// 1. Initialize the SDK.
Eos.initialize(
    EosInitializeOptions.create("MyProduct", "1.0.0")
)

// 2. Create a platform instance.
val platform = Eos.createPlatform(
    EosPlatformOptions.create(
        productId = "00000000000000000000000000000000",
        sandboxId = "00000000000000000000000000000000",
        deploymentId = "00000000000000000000000000000000",
        clientCredentials = EosClientCredentials.of(
            "xyz...clientid",
            "xyz...clientsecret"
        ),
    )
)
try {
    // 3. Drive the SDK.
    while (true) platform.tick()

    // 4. Log in to a user account.
    val loginResult = platform.connect.login(
        localUserId = ProductUserId.Invalid,
        credentialType = EosExternalCredentialType.DeviceIdAccessToken,
        token = "...",
    ).get()

    val userId = loginResult.localUserId

    // 5. Send and receive P2P packets.
    platform.p2p.sendPacket(
        localUserId = userId,
        remoteUserId = someOtherUser,
        socketId = EosP2PSocketId("game-channel"),
        channel = 0,
        data = byteArrayOf(1, 2, 3),
        reliability = EosPacketReliability.ReliableOrdered,
    )
} finally {
    platform.close()
    Eos.shutdown()
}
```

## Subsystem coverage

| Subsystem               | Status    | Notes                                       |
|-------------------------|-----------|---------------------------------------------|
| `Eos` (init/shutdown)   | Full      | Version, byte array helpers                 |
| `EosPlatform`           | Full      | Tick, application/network status            |
| `EosAuth`               | Full      | Login, logout, login status notifications   |
| `EosConnect`            | Full      | External account login, device id, mapping |
| `EosFriends`            | Full      | Friend list, invites, block list           |
| `EosPresence`           | Full      | Query, presence notifications               |
| `EosUserInfo`           | Full      | User info query, local platform type        |
| `EosP2P`                | **Full**  | Send/receive, NAT, relay control            |
| `EosRtc`                | **Full**  | Room join/leave, all notifications          |
| `EosRtcAudio`           | **Full**  | Send audio, mute, volume, devices, buffers  |
| `EosRtcData`            | **Full**  | In-room data channel                        |
| `EosRtcAdmin`           | **Full**  | Server-side kick, hard mute, token query   |
| `EosAntiCheatClient`    | **Full**  | Begin/end session, peer registration, NetProtect encryption, integrity callbacks |
| `EosAntiCheatServer`    | **Full**  | Begin/end session, register/unregister clients, NetProtect encryption, Cerberus event logging |
| `EosLogging`            | Full      | Categories, levels, message callback        |
| `EosLobby`              | Partial   | Handle accessor; full session API WIP       |
| `EosSessions`           | Partial   | Handle accessor; full session API WIP       |
| `EosAchievements`       | Partial   | Handle accessor                             |
| `EosStats`              | Partial   | Handle accessor                             |
| `EosLeaderboards`       | Partial   | Handle accessor                             |
| `EosEcom`               | Partial   | Handle accessor                             |
| `EosUi`                 | Partial   | Handle accessor                             |
| `EosMetrics`            | Partial   | Handle accessor                             |
| `EosMods`               | Partial   | Handle accessor                             |
| `EosReports`            | Partial   | Handle accessor                             |
| `EosSanctions`          | Partial   | Handle accessor                             |
| `EosKws`                | Partial   | Handle accessor                             |
| `EosCustomInvites`      | Partial   | Handle accessor                             |
| `EosProgressionSnapshot`| Partial   | Handle accessor                             |
| `EosIntegratedPlatform` | Partial   | Handle accessor                             |
| `EosPlayerDataStorage`  | Partial   | Handle accessor                             |
| `EosTitleStorage`       | Partial   | Handle accessor                             |

The partial subsystems expose the C handle accessor and are ready to be
extended with the rest of their methods. Each has a working `handle()`
method that obtains the subsystem handle from the platform.

## Anti-Cheat example

The anti-cheat client/server pair is fully implemented and easy to use:

```kotlin
// Client (game process)
platform.antiCheatClient.beginSession(localUserId, EosAntiCheatClientMode.ClientServer)
val handle = platform.antiCheatClient.addNotifyMessageToServer { info ->
    // Forward `info.data` to the game server over your own transport.
    sendToServer(info.data)
}
try {
    // ...
} finally {
    platform.antiCheatClient.removeNotifyMessageToServer(handle)
    platform.antiCheatClient.endSession()
}

// Server (dedicated server process)
platform.antiCheatServer.beginSession(
    registerTimeoutSeconds = 60,
    serverName = "my-dedicated-server",
)
val serverHandle = platform.antiCheatServer.addNotifyMessageToClient { info ->
    // Send `info.data` to the client identified by `info.clientHandle`.
    sendToClient(info.clientHandle, info.data)
}
platform.antiCheatServer.registerClient(
    clientHandle = EosAntiCheatCommon.ClientHandle(playerPointer),
    clientType = ClientType.ProtectedClient,
    userId = localUserId,
)
// ...
```

NetProtect message encryption is also available:

```kotlin
val encrypted = platform.antiCheatClient.protectMessage(plaintext)
val decrypted = platform.antiCheatClient.unprotectMessage(encrypted.data)
```

## Voice chat (RTC) example

```kotlin
// Join a room. Tokens are issued by the server via RTC admin.
val join = platform.rtc.joinRoom(
    localUserId = userId,
    roomName = "party-123",
    clientBaseUrl = "wss://...",          // issued by EOS backend
    participantToken = "...",             // issued by EOS backend
).get()

// Mute / unmute yourself.
platform.rtc.audio.updateSending(
    localUserId = userId,
    roomName = "party-123",
    status = EosRtcAudioStatus.Disabled,
).get()

// Block a participant.
platform.rtc.blockParticipant(
    localUserId = userId,
    roomName = "party-123",
    participantId = otherUser,
    blocked = true,
).get()

// Listen for participants joining or leaving.
val notif = platform.rtc.addNotifyParticipantStatusChanged(userId, "party-123") { info ->
    when (info.status) {
        EosRtcParticipantStatus.Joined -> println("${info.participantId} joined")
        EosRtcParticipantStatus.Left   -> println("${info.participantId} left")
    }
}
```

## Native library

The shared library is loaded automatically from one of:

1. `System.loadLibrary("EOSSDK")` - if installed system-wide.
2. `resources/natives/` inside the project's JAR - extracted to a temp
   file at startup. Place the appropriate binary here for your target
   platform (`EOSSDK-Win64-Shipping.dll`, `libEOSSDK-Linux-Shipping.so`,
   or `libEOSSDK-Mac-Shipping.dylib`).

The Kotlin build automatically copies the binary from `EOS_SDK/SDK/Bin/`
into the JAR at build time.

## Building

```
./gradlew build
```

The build uses JDK 25 and Kotlin 2.4.0. The FFM API is used for all
native interop; no JNI is generated.

## Code generation

`scripts/codegen.py` is a small helper that parses the EOS C headers
and emits Kotlin skeletons for enums and structs. It is not a full
transpiler; the output is meant as a starting point. The bindings in
this repository were written and curated by hand.
