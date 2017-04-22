package me.gong.mcleaks;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

class MCLeaksAPIImpl implements MCLeaksAPI {

    private static final String API_PRE = "https://mcleaks.themrgong.xyz/api/v2/",
            NAME_CHECK = "isnamemcleaks",
            UUID_CHECK = "isuuidmcleaks";

    private final ExecutorService service;
    private final LoadingCache<String, Boolean> nameCache;
    private final LoadingCache<UUID, Boolean> uuidCache;
    private final Gson gson = new Gson();

    private final boolean testing;

    MCLeaksAPIImpl(int threadCount, long expireAfter, TimeUnit unit, boolean testing) {
        this.service = Executors.newFixedThreadPool(threadCount);
        this.nameCache = createCache(expireAfter, unit, new MCLeaksNameChecker());
        this.uuidCache = createCache(expireAfter, unit, new MCLeaksUUIDChecker());

        this.testing = testing;
    }

    @Override
    public void checkAccount(String username, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        doResultHandling(() -> checkAccount(username), callback, errorHandler);
    }

    @Override
    public Result checkAccount(String username) {
        try {
            return new Result(nameCache.get(username));
        } catch (Exception ex) {
            return new Result(ex.getCause());
        }
    }

    @Override
    public void checkAccount(UUID uuid, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        doResultHandling(() -> checkAccount(uuid), callback, errorHandler);
    }

    @Override
    public Result checkAccount(UUID uuid) {
        try {
            return new Result(uuidCache.get(uuid));
        } catch (Exception ex) {
            return new Result(ex.getCause());
        }
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
        this.nameCache.cleanUp();
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

    private static class MCLeaksNameRequest {
        private String name;

        MCLeaksNameRequest(String name) {
            this.name = name;
        }
    }

    private static class MCLeaksUUIDRequest {
        private String uuid;

        MCLeaksUUIDRequest(UUID uuid) {
            this.uuid = uuid.toString();
        }
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
        protected String getInputAsJson(String input) {
            return gson.toJson(new MCLeaksNameRequest(input));
        }
    }

    private class MCLeaksUUIDChecker extends MCLeaksChecker<UUID> {

        @Override
        protected String getApiType() {
            return UUID_CHECK;
        }

        @Override
        protected String getInputAsJson(UUID input) {
            return gson.toJson(new MCLeaksUUIDRequest(input));
        }
    }

    private abstract class MCLeaksChecker<T> extends CacheLoader<T, Boolean> {

        @Override
        public Boolean load(T name) throws Exception {
            URL url = new URL(API_PRE + getApiType());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            String input = this.getInputAsJson(name);
            byte[] data = input.getBytes(StandardCharsets.UTF_8);

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", Integer.toString(data.length));
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("User-Agent", "MCLeaksApiClient" + (testing ? "-testing" : ""));

            OutputStream os = conn.getOutputStream();
            os.write(data);
            os.flush();


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
                    System.out.println("Failed to read error");
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

        protected abstract String getInputAsJson(T input);
    }
}
