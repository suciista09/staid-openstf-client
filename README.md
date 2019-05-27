# staid-openstf-client

## How to use
1. Produce jar 
```./gradlew jar```

2. Add jar as your library

## In your test
1. Set up
```
        try {
            StaidOpenSTF staidOpenSTF = new StaidOpenSTF("stfUrl", "stfToken");
            stfDevice = new Device(staidOpenSTF, stfDeviceSerial);

            stfDevice.connectToSTFDevice();
            stfRemoteDeviceUrl = stfDevice.getRemoteDeviceUrl();

            
        } catch (Exception e) {
            throw  new ExceptionInInitializerError("Can not connect device to open stf. Details : " + e.getMessage());
        }
         
        //adding to your capabilities
        desiredCapabilities.setCapability("udid", stfRemoteDeviceUrl);
```
2. Tear Down
```
            try {
                stfDevice.releaseOpenSTFDevice();
                stfDevice.adbDisconnect();
            } catch (IOException e) {
                //error handling
            } catch (InterruptedException e) {
                //error handling
            }
```

## Next Feature
- be able to run with random available device by providing criteria
