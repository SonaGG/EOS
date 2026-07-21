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
