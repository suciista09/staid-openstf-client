package staid.openstf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Device device = new Device(staidOpenSTF);

        Map<String, String> criteria = new HashMap<>();
        criteria.put("platform", "Android");
        criteria.put("version", "7.1.2");

        String serial = device.getAvailableDeviceByCriteria(criteria);
        System.out.println("serial : " + serial);
    }

    @Test
    public void getOnlyAvailableDevice() {
        Device device = new Device(staidOpenSTF);

        String serialDevice = device.waitAvailableDevice();
        System.out.println("Available device : " +serialDevice);
        Assert.assertFalse("it should not be false", serialDevice.equalsIgnoreCase(""));
    }

    @Test
    public void getRandomDevice() {
        Device device = new Device(staidOpenSTF);

        String serialDevice = device.getOnlyAvailableDevice();
        System.out.println("Available device : " +serialDevice);
        Assert.assertFalse("it should not be false", serialDevice.equalsIgnoreCase(""));
    }

}