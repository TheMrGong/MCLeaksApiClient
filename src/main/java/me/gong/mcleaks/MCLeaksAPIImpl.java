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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class MCLeaksAPIImpl implements MCLeaksAPI {

    private static final String API_URL = "http://mcleaks.themrgong.xyz/api/v1/ismcleaks";

    private final ExecutorService service;
    private final LoadingCache<String, Boolean> cache;
    private final Gson gson = new Gson();
    private final String apiKey;

    MCLeaksAPIImpl(String apiKey, int threadCount, long expireAfter, TimeUnit unit) {
        this.service = Executors.newFixedThreadPool(threadCount);
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireAfter, unit).build(new McleaksFetcher());
        this.apiKey = apiKey;
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
        private String name, apiKey;

        MCLeaksRequest(String name, String apiKey) {
            this.name = name;
            this.apiKey = apiKey;
        }
    }

    private static class MCLeaksResponse {
        private boolean isMcleaks;
    }

    private class McleaksFetcher extends CacheLoader<String, Boolean> {

        @Override
        public Boolean load(String name) throws Exception {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String input = gson.toJson(new MCLeaksRequest(name, apiKey));

            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            StringBuilder json = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null)
                json.append(output);

            conn.disconnect();
            return gson.fromJson(json.toString(), MCLeaksResponse.class).isMcleaks;
        }
    }
}
