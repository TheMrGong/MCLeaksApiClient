package me.gong.mcleaks;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

class MCLeaksAPIImpl implements MCLeaksAPI {

    private static final String API_PRE = "https://mcleaks.themrgong.xyz/api/v3/",
            NAME_CHECK = "isnamemcleaks",
            UUID_CHECK = "isuuidmcleaks";

    private final ExecutorService service;
    private final LoadingCache<String, Boolean> nameCache;
    private final LoadingCache<UUID, Boolean> uuidCache;
    private final MCLeaksChecker<String> nameChecker;
    private final MCLeaksChecker<UUID> uuidChecker;
    private final Gson gson = new Gson();
    private final String userAgent;

    private final boolean testing;

    MCLeaksAPIImpl(int threadCount, long expireAfter, TimeUnit unit, boolean testing, String userAgent) {
        this.service = Executors.newFixedThreadPool(threadCount);

        this.nameChecker = new MCLeaksNameChecker();
        this.uuidChecker = new MCLeaksUUIDChecker();

        this.nameCache = createCache(expireAfter, unit, this.nameChecker);
        this.uuidCache = createCache(expireAfter, unit, this.uuidChecker);

        this.testing = testing;
        this.userAgent = userAgent;
    }

    MCLeaksAPIImpl(int threadCount, boolean testing, String userAgent) {
        this.service = Executors.newFixedThreadPool(threadCount);

        this.nameChecker = new MCLeaksNameChecker();
        this.uuidChecker = new MCLeaksUUIDChecker();

        this.nameCache = null;
        this.uuidCache = null;

        this.testing = testing;
        this.userAgent = userAgent;
    }

    @Override
    public void checkAccount(String username, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        doResultHandling(() -> checkAccount(username), callback, errorHandler);
    }

    @Override
    public Result checkAccount(String username) {
        try {
            return new Result(checkNameExists(username));
        } catch (Exception ex) {
            return new Result(ex.getCause());
        }
    }

    @Override
    public Optional<Boolean> getCachedCheck(String username) {
        return Optional.ofNullable(this.nameCache == null ? null : this.nameCache.getIfPresent(username));
    }

    @Override
    public void checkAccount(UUID uuid, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        doResultHandling(() -> checkAccount(uuid), callback, errorHandler);
    }

    @Override
    public Result checkAccount(UUID uuid) {
        try {
            return new Result(checkUUIDExists(uuid));
        } catch (Exception ex) {
            return new Result(ex.getCause());
        }
    }

    @Override
    public Optional<Boolean> getCachedCheck(UUID uuid) {
        return Optional.ofNullable(this.uuidCache == null ? null : this.uuidCache.getIfPresent(uuid));
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
        this.nameCache.cleanUp();
    }

    private boolean checkNameExists(String name) throws Exception {
        if (this.nameCache == null) return this.nameChecker.load(name);
        return this.nameCache.get(name);
    }

    private boolean checkUUIDExists(UUID uuid) throws Exception {
        if (this.uuidCache == null) return this.uuidChecker.load(uuid);
        return this.uuidCache.get(uuid);
    }

    private <T> LoadingCache<T, Boolean> createCache(long expireAfter, TimeUnit unit, MCLeaksChecker<T> checker) {
        return CacheBuilder.newBuilder().expireAfterWrite(expireAfter, unit).build(checker);
    }

    private void doResultHandling(Supplier<Result> resultSupplier, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        this.service.submit(() -> {
            final Result result = resultSupplier.get();
            if (result.hasError()) errorHandler.accept(result.getError());
            else callback.accept(result.isMCLeaks());
        });
    }

    private static class MCLeaksResponse {
        private boolean isMcleaks;
    }

    private static class MCLeaksError {
        private String error;
    }

    private class MCLeaksNameChecker extends MCLeaksChecker<String> {

        @Override
        protected String getApiType() {
            return NAME_CHECK;
        }

        @Override
        protected String getInputText(String input) {
            return input;
        }
    }

    private class MCLeaksUUIDChecker extends MCLeaksChecker<UUID> {

        @Override
        protected String getApiType() {
            return UUID_CHECK;
        }

        @Override
        protected String getInputText(UUID input) {
            return input.toString();
        }
    }

    private abstract class MCLeaksChecker<T> extends CacheLoader<T, Boolean> {

        @Override
        public Boolean load(T value) throws Exception {
            URL url = new URL(API_PRE + getApiType() + "/" + this.getInputText(value));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", MCLeaksAPIImpl.this.userAgent + (testing ? "-testing" : ""));

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream())));

            StringBuilder json = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null)
                json.append(output);

            conn.disconnect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                MCLeaksError mcLeaksError;
                try {
                    mcLeaksError = gson.fromJson(json.toString(), MCLeaksError.class);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to properly decode error: \"" + json.toString() + "\" with response code \"" + conn.getResponseCode() + "\"", ex);
                }
                throw new RuntimeException("Failed request with response code \"" + conn.getResponseCode() + "\" " +
                        (mcLeaksError == null ? "No error message supplied" : "Error message: " + mcLeaksError.error));
            }
            try {
                return gson.fromJson(json.toString(), MCLeaksResponse.class).isMcleaks;
            } catch (Exception ex) {
                throw new RuntimeException("Failed to decode response \"" + json.toString() + "\", had OK response code.");
            }
        }

        protected abstract String getApiType();

        protected abstract String getInputText(T input);
    }
}
