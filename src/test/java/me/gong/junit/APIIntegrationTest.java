package me.gong.junit;

import me.gong.mcleaks.MCLeaksAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

@SuppressWarnings("unchecked")
public class APIIntegrationTest {

    private static final String KNOWN_MCLEAK_NAME = "BwA_BOOMSTICK";
    private static final UUID KNOWN_MCLEAK_UUID = UUID.fromString("071a1906-a49c-4eaa-9156-98df14c2c72b");
    private static final long MAX_CACHE_LOOKUP = TimeUnit.SECONDS.toMillis(5);

    private MCLeaksAPI api;

    @Before
    public void setupApi() {
        this.api = MCLeaksAPI.builder()
                .threadCount(2)
                .expireAfter(10, TimeUnit.MINUTES)
                .testing()
                .build();
    }

    @After
    public void cleanup() {
        this.api.shutdown();
    }

    @Test
    public void shouldThrowErrorInvalidName() {

        MCLeaksAPI.Result result = api.checkAccount(KNOWN_MCLEAK_NAME + "sefesfsesesefesfsefsefsef");
        assertTrue("No error was thrown for an invalid name", result.hasError());
    }

    @Test
    public void shouldBeMCLeaks() {
        handleShouldBeMCLeaks(api.checkAccount(KNOWN_MCLEAK_NAME), "name");
        handleShouldBeMCLeaks(api.checkAccount(KNOWN_MCLEAK_UUID), "uuid");
    }

    private void handleShouldBeMCLeaks(MCLeaksAPI.Result result, String checkType) {
        assertFalse(String.format("Error was thrown for valid %s : ", checkType) + result.getError(), result.hasError());
        assertTrue("Known MCLeaks account wasn't MCLeaks", result.isMCLeaks());
    }

    @Test
    public void resultShouldBeCached() {
        handleResultShouldBeCached(() -> api.checkAccount(KNOWN_MCLEAK_NAME));
        handleResultShouldBeCached(() -> api.checkAccount(KNOWN_MCLEAK_UUID));
    }

    private void handleResultShouldBeCached(Runnable checkRemote) {
        checkRemote.run(); // ignore result
        final long init = System.currentTimeMillis();
        checkRemote.run(); // check delay

        // if there's a better way to check if a result is cache'd, let me know at contact@mail.themrgong.xyz
        assertTrue("Previous result wasn't cache'd", System.currentTimeMillis() - init <= MAX_CACHE_LOOKUP);
    }
}
