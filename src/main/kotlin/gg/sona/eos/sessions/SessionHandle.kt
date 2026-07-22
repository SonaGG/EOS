package gg.sona.eos.sessions

/** Opaque handle to an active session, used for join operations. */
@JvmInline
public value class SessionHandle(public val raw: Long) {
    public fun isValid(): Boolean = raw != 0L

    public companion object {
        public val Invalid: SessionHandle = SessionHandle(0L)
    }
}