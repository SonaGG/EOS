package gg.sona.eos

import gg.sona.eos.common.ProductUserId
import gg.sona.eos.logging.EosLogCategory
import gg.sona.eos.logging.EosLogLevel
import gg.sona.eos.logging.EosLogMessage
import gg.sona.eos.logging.EosLogging
import gg.sona.eos.util.printResult

fun logCallback(msg: EosLogMessage) {
    println("[${msg.category}] ${msg.level}: ${msg.message}")
}

// https://dev.epicgames.com/docs/epic-online-services/eos-get-started/get-started-guide/integrate-eos-sdk#step-a-create-a-data-structure-to-hold-your-games-credentials
data object EOSSdkConfig {
    /** The product ID for the running game, found on the Developer Portal. */
    val ProductId = "ce21b21dbc44475a9260588f24cd4951"

    /** Client ID of the service permissions entry, found on the Developer Portal. */
    val ClientCredentialsId = "xyza7891ZWIlCZuZHVMmPZGMzCVSbduX"

    /** Client secret for accessing the set of permissions, found on the Developer Portal. */
    val ClientCredentialsSecret = "[redacted]"

    /** The sandbox ID for the running game, found on the Developer Portal. */
    val SandboxId = "8ae7719c77df46f2b91e48ca91b3e1f6"

    /** The deployment ID for the running game, found on the Developer Portal. */
    val DeploymentId = "000442b86aa24cc69296fe87f2b690b2"

    /** A display name of your choice. EOS services use the display name to identify your game in logs and reporting. You can use any combination of alphanumeric or special characters. */
    val GameName = "EOS_TEST"

    /** A version name of your choice. EOS services use the version to identify your game in logs and reporting. You can use any combination of alphanumeric or special characters. */
    val GameVersion = "1.0.0"

    /** Encryption key. Required only if your game uses Player Data Storage or Title Data Storage. */
    val EncryptionKey = "YOUR_ENCRYPTION_KEY"
}

fun main() {
    Eos.initialize(
        EosInitializeOptions.create(
            productName = EOSSdkConfig.GameName,
            productVersion = EOSSdkConfig.GameVersion
        )
    ).printResult()

    EosLogging.setCallback(::logCallback)
    EosLogging.setLogLevel(EosLogCategory.AllCategories, EosLogLevel.Verbose)

    val platform = Eos.createPlatform(
        EosPlatformOptions.create(
            productId = EOSSdkConfig.ProductId,
            sandboxId = EOSSdkConfig.SandboxId,
            deploymentId = EOSSdkConfig.DeploymentId,
            clientCredentials = EosClientCredentials.of(
                clientId = EOSSdkConfig.ClientCredentialsId,
                clientSecret = EOSSdkConfig.ClientCredentialsSecret
            )
        )
    )

    println(platform.getApplicationStatus())
}