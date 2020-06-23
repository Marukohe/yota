package io.github.clixyz.yota.droid

import io.github.clixyz.yota.droid.delegates.*

object Droid {

    private var amInternal: AmsDelegate? = null
    private var imInternal: ImsDelegate? = null
    private var pmInternal: PmsDelegate? = null
    private var wmInternal: WmsDelegate? = null
    private var uaInternal: UiAutoDelegate? = null

    val am: AmsDelegate
    @Throws(HasNotInitializeException::class)
    get() {
        mustInitialize()
        return amInternal!!
    }

    val im: ImsDelegate
    @Throws(HasNotInitializeException::class)
    get() {
        mustInitialize()
        return imInternal!!
    }

    val pm: PmsDelegate
    @Throws(HasNotInitializeException::class)
    get() {
        mustInitialize()
        return pmInternal!!
    }

    val wm: WmsDelegate
    @Throws(HasNotInitializeException::class)
    get() {
        mustInitialize()
        return wmInternal!!
    }

    val ua: UiAutoDelegate
    @Throws(HasNotInitializeException::class)
    get() {
        mustInitialize()
        return uaInternal!!
    }

    val inited
    get() = amInternal != null &&
            imInternal != null &&
            pmInternal != null &&
            uaInternal != null &&
            uaInternal!!.connected

    fun init() {
        if (inited) {
            return
        }

        amInternal = try {
            AmsDelegate.FETCHER.fetch()
        } catch (e: DroidDelegate.UnableToFetchException) {
            throw UnableToInitException("Unable to connect to activity manager; "
                    + "is the system running?")
        }

        imInternal = try {
            ImsDelegate.FETCHER.fetch()
        } catch (e: DroidDelegate.UnableToFetchException) {
            throw UnableToInitException("Unable to connect to input manager; "
                    + "is the system running?")
        }

        pmInternal = try {
            PmsDelegate.FETCHER.fetch()
        } catch (e: DroidDelegate.UnableToFetchException) {
            throw UnableToInitException("Unable to connect to package manager; "
                    + "is the system running?")
        }

        wmInternal = try {
            WmsDelegate.FETCHER.fetch()
        } catch (e: DroidDelegate.UnableToFetchException) {
            throw UnableToInitException("Unable to connect to window manager; "
                    + "is the system running?")
        }

        uaInternal = UiAutoDelegate.apply {
            connect()
        }
    }

    fun deinit() {
        if (inited) {
            uaInternal!!.disconnect()
        }
    }

    /**
     * Run action within droid
     * - if initialized, do it directly, and never deinit afterwards
     * - otherwise, init, do, and deinit
     */
    inline fun <R> exec(action: (Droid) -> R): R {
        return if (inited) {
            action.invoke(this)
        } else {
            try {
                init()
                action.invoke(this)
            } finally {
                deinit()
            }
        }
    }

    @Throws(HasNotInitializeException::class)
    private fun mustInitialize() {
        if (!inited) {
            throw HasNotInitializeException()
        }
    }

    class UnableToInitException(msg: String) : Exception(msg)
    class HasNotInitializeException : Exception("Droid has not been initialized yet")
}