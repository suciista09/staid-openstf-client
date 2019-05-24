package staid.openstf;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class DevicesTest {

    private static StaidOpenSTF staidOpenSTF;

    @BeforeClass
    public static void getOpenSTF(){
        staidOpenSTF = new StaidOpenSTF("", "");
    }

    @Test
    public void test() throws IOException, InterruptedException {
        Device device = new Device(staidOpenSTF, "J5AXGF01P964XVC");
        try {
            device.connectToSTFDevice();

        } catch (Exception e) {
            e.printStackTrace();
        }

        device.releaseOpenSTFDevice();
        device.adbDisconnect();

    }
}