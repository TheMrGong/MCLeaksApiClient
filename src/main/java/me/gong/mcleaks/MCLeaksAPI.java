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
     * @param name The name to check
     * @param callback Handle the processed result
     * @param errorHandler Handle any errors that have occured
     */
    void checkMCLeak(String name, Consumer<Boolean> callback, Consumer<Throwable> errorHandler);

    /**
     * Shuts down the API and prevents further requests
     */
    void shutdown();

    /**
     * Creates a new MCLeaks API with specified parameters
     *
     * @param key The API key for requests
     * @param threadCount The amount of threads (allows multiple requests to be running)
     * @param expireAfter The amount of time to cache responses
     * @param unit The time unit type
     * @return A new MCLeaks API
     */
    static MCLeaksAPI newAPI(String key, int threadCount, long expireAfter, TimeUnit unit) {
        return new MCLeaksAPIImpl(key, threadCount, expireAfter, unit);
    }
}
