import me.gong.mcleaks.MCLeaksAPI;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {

        MCLeaksAPI api = MCLeaksAPI.builder()
                .threadCount(2)
                .expireAfter(10, TimeUnit.MINUTES).build();

        api.checkAccount("BwA_BOOMSTICK", isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);

        // clean up (usually in onDisable)
        api.shutdown();
    }
}