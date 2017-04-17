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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class MCLeaksAPIImpl implements MCLeaksAPI {

    private static final String API_URL = "http://mcleaks.themrgong.xyz/api/v1/ismcleaks";

    private final ExecutorService service;
    private final LoadingCache<String, Boolean> cache;
    private final Gson gson = new Gson();

    MCLeaksAPIImpl(int threadCount, long expireAfter, TimeUnit unit) {
        this.service = Executors.newFixedThreadPool(threadCount);
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireAfter, unit).build(new McleaksFetcher());
    }

    @Override
    public void checkAccount(String username, Consumer<Boolean> callback, Consumer<Throwable> errorHandler) {
        service.submit(() -> {
            try {
                callback.accept(cache.get(username));
            } catch (Exception e) {
                errorHandler.accept(e);
            }
        });
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
        this.cache.cleanUp();
    }

    private static class MCLeaksRequest {
        private String name;

        MCLeaksRequest(String name) {
            this.name = name;
        }
    }

    private static class MCLeaksResponse {
        private boolean isMcleaks;
    }

    private static class MCLeaksError {
        private String error;
    }

    private class McleaksFetcher extends CacheLoader<String, Boolean> {

        @Override
        public Boolean load(String name) throws Exception {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            String input = gson.toJson(new MCLeaksRequest(name));
            byte[] data = input.getBytes(StandardCharsets.UTF_8);

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", Integer.toString(data.length));
            conn.setRequestProperty("charset", "utf-8");

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
                final MCLeaksError mcLeaksError = gson.fromJson(json.toString(), MCLeaksError.class);
                throw new RuntimeException("Failed request with response code \"" + conn.getResponseCode() + "\" " +
                        (mcLeaksError == null ? "No error message supplied" : "Error message: " + mcLeaksError.error));
            }
            return gson.fromJson(json.toString(), MCLeaksResponse.class).isMcleaks;
        }
    }
}
