package gg.sona.eos
/** Client credentials issued by the Epic dev portal. */
public class EosClientCredentials {
    public var clientId: String? = null
    public var clientSecret: String? = null

    public companion object {
        public fun of(clientId: String, clientSecret: String): EosClientCredentials =
            EosClientCredentials().apply {
                this.clientId = clientId
                this.clientSecret = clientSecret
            }
    }
}