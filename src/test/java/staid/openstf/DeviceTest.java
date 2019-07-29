package staid.openstf;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DeviceTest {

    private static StaidOpenSTF staidOpenSTF;

    @BeforeClass
    public static void getOpenSTF(){
        staidOpenSTF = new StaidOpenSTF("", "");
    }

    @Test
    public void testConnect() throws IOException, InterruptedException {
        Device device = new Device(staidOpenSTF);
        try {
            device.connectToSTFDevice();
            device.adbConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Device : " + device.getRemoteDeviceUrl());
        device.adbDisconnect();
        device.stopUsingOpenSTF();

    }

    @Test
    public void getAvailableDeviceByCriteria() {
        StaidOpenSTF staidOpenSTF = new StaidOpenSTF("", "");
        Device device = new Device(staidOpenSTF);

        Map<String, String> criteria = new HashMap<>();
        criteria.put("platform", "Android");
        criteria.put("version", "7.1.2");

        String serial = device.getAvailableDeviceByCriteria(criteria);
        System.out.println("serial : " + serial);
    }

}