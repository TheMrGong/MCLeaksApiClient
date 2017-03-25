import me.gong.mcleaks.MCLeaksAPI;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) {
        MCLeaksAPI api = MCLeaksAPI.newAPI("e96ed09c36e04471be526d7078ac2c98", 2, 5, TimeUnit.MINUTES);

        api.checkMCLeak("BwA_BOOMSTICK", isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);
    }
}