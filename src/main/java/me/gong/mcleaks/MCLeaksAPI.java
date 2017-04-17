package me.gong.mcleaks;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Interface for requesting whether accounts are MCLeaks or not
 */
public interface MCLeaksAPI {

    /**
     * Checks whether the specified name is an MCLeaks account
     *
     * @param username The name to check
     * @param callback Handle the processed result
     * @param errorHandler Handle any errors that have occured
     */
    void checkAccount(String username, Consumer<Boolean> callback, Consumer<Throwable> errorHandler);

    /**
     * Shuts down the API and prevents further requests
     */
    void shutdown();

    /**
     * Begins building a new MCLeaks API
     *
     * @return A new MCLeaks API
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating a MCLeaks checker
     */
    class Builder {
        private int threadCount = 3;
        private long expireAfter = 5;
        private TimeUnit unit = TimeUnit.MINUTES;

        /**
         * The amount of threads to use for concurrent requests
         *
         * @param threadCount The number of threads
         * @return This builder
         */
        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        /**
         * How long to keep data before requiring re-fetching
         *
         * @param expireAfter The amount of time to cache
         * @param unit The unit of time
         * @return This builder
         */
        public Builder expireAfter(long expireAfter, TimeUnit unit) {
            this.expireAfter = expireAfter;
            this.unit = unit;
            return this;
        }

        /**
         * Builds a new MCLeaks API
         *
         * @return The built API
         */
        public MCLeaksAPI build() {
            return new MCLeaksAPIImpl(threadCount, expireAfter, unit);
        }
    }
}
