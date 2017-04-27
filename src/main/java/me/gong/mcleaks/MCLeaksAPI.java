package me.gong.mcleaks;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Interface for requesting whether accounts are MCLeaks or not
 */
public interface MCLeaksAPI {

    /**
     * Checks whether the specified name is an MCLeaks account
     * <br>
     * Recommended to use {@link MCLeaksAPI#checkAccount(UUID)}
     *
     * @param username     The name to check
     * @param callback     Handle the processed result
     * @param errorHandler Handle any errors that have occured
     */
    void checkAccount(String username, Consumer<Boolean> callback, Consumer<Throwable> errorHandler);

    /**
     * Checks an account blocking the thread that invoked the method
     * <br>
     * Recommended to use {@link MCLeaksAPI#checkAccount(UUID)}
     *
     * @param username The name to check
     * @return The results containing either whether the account was MCLeaks or if there was an error
     */
    Result checkAccount(String username);

    /**
     * Checks whether the specified name is an MCLeaks account
     *
     * @param uuid         The uuid to check
     * @param callback     Handle the processed result
     * @param errorHandler Handle any errors that have occured
     */
    void checkAccount(UUID uuid, Consumer<Boolean> callback, Consumer<Throwable> errorHandler);

    /**
     * Checks an account blocking the thread that invoked the method
     *
     * @param uuid The name to check
     * @return The results containing either whether the account was MCLeaks or if there was an error
     */
    Result checkAccount(UUID uuid);

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
     * Data storage for the response of the MCLeaks API
     */
    class Result {
        private boolean isMCLeaks;
        private Throwable error;

        Result(boolean isMCLeaks) {
            this.isMCLeaks = isMCLeaks;
        }

        Result(Throwable error) {
            this.error = error;
        }

        // getter no doc needed
        public boolean isMCLeaks() {
            return this.isMCLeaks;
        }

        public Throwable getError() {
            return this.error;
        }

        /**
         * Checks whether any error was thrown during checking
         *
         * @return True if checking an account failed
         */
        public boolean hasError() {
            return this.error != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Result result = (Result) o;

            if (isMCLeaks != result.isMCLeaks) return false;
            return error != null ? error.equals(result.error) : result.error == null;
        }

        @Override
        public int hashCode() {
            int result = (isMCLeaks ? 1 : 0);
            result = 31 * result + (error != null ? error.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Result{" + "isMCLeaks=" + isMCLeaks +
                    ", error=" + error +
                    '}';
        }
    }

    /**
     * Builder for creating a MCLeaks checker
     */
    class Builder {
        private int threadCount = 3;
        private long expireAfter = 5;
        private TimeUnit unit = TimeUnit.MINUTES;
        private boolean testing, noCache;

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
         * How long to keep data before requiring re-fetching.
         * Not used if {@link Builder#nocache()} is used
         *
         * @param expireAfter The amount of time to cache
         * @param unit        The unit of time
         * @return This builder
         */
        public Builder expireAfter(long expireAfter, TimeUnit unit) {
            this.expireAfter = expireAfter;
            this.unit = unit;
            return this;
        }

        /**
         * Marks requests coming from the service for testing
         *
         * @return This builder
         */
        public Builder testing() {
            this.testing = true;
            return this;
        }

        /**
         * Makes it so a cache is not used.
         * Useful if you're going to implement your own cache
         *
         * @return This builder
         */
        public Builder nocache() {
            this.noCache = true;
            return this;
        }

        /**
         * Builds a new MCLeaks API
         *
         * @return The built API
         */
        public MCLeaksAPI build() {
            if(this.noCache) return new MCLeaksAPIImpl(threadCount, testing);
            return new MCLeaksAPIImpl(threadCount, expireAfter, unit, testing);
        }
    }
}
