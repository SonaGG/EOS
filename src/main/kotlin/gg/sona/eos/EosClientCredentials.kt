package gg.sona.eos

/** Client credentials issued by the Epic dev portal. */
class EosClientCredentials {
    var clientId: String? = null
    var clientSecret: String? = null

    companion object {
        fun of(clientId: String, clientSecret: String): EosClientCredentials =
            EosClientCredentials().apply {
                this.clientId = clientId
                this.clientSecret = clientSecret
            }
    }
}