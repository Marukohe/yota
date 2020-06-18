package io.github.clixyz.yota.droid

interface DroidDelegate {

    // fetch and save, only once
    abstract class SingletonFetcher<T: Any> {
        private lateinit var delegate: T

        @Throws(UnableToFetchException::class)
        fun fetch(): T {
            if (!this::delegate.isInitialized) {
                delegate = doFetch()
            }
            return delegate
        }

        @Throws(UnableToFetchException::class)
        protected abstract fun doFetch(): T
    }

    class UnableToFetchException(name: String) : Exception("Failed to fetch $name")
}