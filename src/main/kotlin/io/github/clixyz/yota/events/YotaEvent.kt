package io.github.clixyz.yota.events

interface YotaEvent {

    companion object {
        const val INJECT_SUCCEEDED = 0
        const val INJECT_FAILED = -1
        const val INJECT_FAILED_REMOTE_EXCEPTION = -2
        const val INJECT_FAILED_SECURITY_EXCEPTION = -3
        const val INJECT_FAILED_NULL_ROOT = -4
        const val INJECT_FAILED_NO_SUCH_VIEW = -5
    }

    fun inject(): Int
}