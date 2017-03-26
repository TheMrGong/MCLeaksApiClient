import me.gong.mcleaks.MCLeaksAPI;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {

        MCLeaksAPI api = MCLeaksAPI.builder()
                .key("e96ed09c36e04471be526d7078ac2c98")
                .threadCount(2)
                .expireAfter(5, TimeUnit.MINUTES).build();

        api.checkAccount("BwA_BOOMSTICK", isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);
    }
}