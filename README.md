# staid-openstf-client

## How to use
1. Produce jar 
```./gradlew jar```

2. Add jar as your library
3. execute as jar

## Add Jar As Library
1. Set up
```
        try {
            StaidOpenSTF staidOpenSTF = new StaidOpenSTF("stfUrl", "stfToken");
            stfDevice = new Device(staidOpenSTF);

            stfDevice.setSerial("xxxxxx");
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

## Execute As Jar
1. create config file (e.g. config.properties)
```
stfUrl=
stfToken=
```
2. list command
- connect to specific serial device
```
{path of config.prop} connect {serial}
```
- connect to specific serial and add to json. This feature will be usefull to run distributed test / parallel test. "udid" : "{deviceRemoteUrl}" will be added in json
```
{path of config.prop} connect {serial} {cap def file name.json}
```
- disconnect from specific serial or specific deviceRemoteUrl
```
{path of config.prop} disconnect serial={serial}/remoteDeviceUrl={deviceConnectedUrl}
```
- disconnect from json file. It will search for udid
```
{path of config.prop} disconnect def-cap1.json def-cap2.json ...
```
