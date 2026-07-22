package gg.sona.eos.sessions

/** Opaque handle to an active session, used for join operations. */
@JvmInline
value class SessionHandle(val raw: Long) {
    fun isValid(): Boolean = raw != 0L

    companion object {
        val Invalid: SessionHandle = SessionHandle(0L)
    }
}