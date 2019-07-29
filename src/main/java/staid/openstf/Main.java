package staid.openstf;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.Properties;

public class Main {

    public static void main (String args[]){
        try{
            String pathConfig = args[0];
            Properties properties = loadPropertiesConfig(pathConfig);
            String url = properties.getProperty("stfUrl");
            String token = properties.getProperty("stfToken");
            String action = args[1];

            if (action.equalsIgnoreCase("connect")){
                if (args.length ==3 ){
                    //{path of config.prop} connect {serial}
                    String serial = args[2];
                    StaidOpenSTF staidOpenSTF = new StaidOpenSTF(url, token);
                    Device device = new Device(staidOpenSTF);
                    device.setSerial(serial);
                    device.connectToSTFDevice();
                    device.adbConnect();
                    System.out.println(device.getRemoteDeviceUrl());

                }else {
                    //{path of config.prop} connect {serial} {cap def file name.json}
                    String serial = args[2];
                    String pathFile = args[3];

                    StaidOpenSTF staidOpenSTF = new StaidOpenSTF(url, token);
                    Device device = new Device(staidOpenSTF);
                    device.setSerial(serial);
                    device.connectToSTFDevice();
                    device.adbConnect();

                    //produce file
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("udid", device.getRemoteDeviceUrl());

                    FileWriter fileWriter = new FileWriter(new File(pathFile));

                    fileWriter.write(jsonObject.toString());
                    fileWriter.flush();
                    fileWriter.close();

                }
            }else if (action.equalsIgnoreCase("disconnect")) {

                if (args.length >= 3){

                    if (args[2].contains("=")){
                        //{path of config.prop} disconnect serial={serial}/remoteDeviceUrl={deviceConnectedUrl}

                        StaidOpenSTF staidOpenSTF = new StaidOpenSTF(url, token);
                        Device device = new Device(staidOpenSTF);

                        if (args[2].split("=")[0].equals("serial")){
                            String serial = args[2].split("=")[1];

                            //disconnect by serial
                            device.setSerial(serial);
                            device.setRemoteDeviceUrl(device.getRemoteDeviceUrl(serial));
                            device.stopUsingOpenSTF();
                            device.adbDisconnect();

                        }else if (args[2].split("=")[0].equals("remoteDeviceUrl")){
                            String remoteDeviceUrl = args[2].split("=")[1];

                            device.setRemoteDeviceUrl(remoteDeviceUrl);
                            device.setSerial(device.getSerial(remoteDeviceUrl));
                            device.stopUsingOpenSTF();
                            device.adbDisconnect();

                        }else {
                            //throw exception
                            throw new ExceptionInInitializerError("Unknown command. Run : java - jar {file}.jar help");
                        }

                    }else {
                        //{path of config.prop} disconnect def-cap1.json def-cap2.json ...
                        for (int i = 2 ; i < args.length ; i++){
                            String path = args[i];
                            JSONObject defaultCap = getJsonObjectFromFile(path);

                            String remoteDeviceUrl = defaultCap.getString("udid");

                            StaidOpenSTF staidOpenSTF = new StaidOpenSTF(url, token);
                            Device device = new Device(staidOpenSTF);
                            device.setRemoteDeviceUrl(remoteDeviceUrl);
                            device.setSerial(device.getSerial(remoteDeviceUrl));

                            device.stopUsingOpenSTF();
                            device.adbDisconnect();
                        }
                    }


                }else {
                    //throw exception
                    throw new ExceptionInInitializerError("Unknown command. Run : java - jar {file}.jar help");
                }


            }else if (action.equalsIgnoreCase("help")){
                //throw error
                throw new ExceptionInInitializerError("Unknown command. Suggestion : \n" +
                        "- " + pathConfig + " connect {serial}" +
                        "- " + pathConfig + "connect {serial} {cap-def-file-name.json}" +
                        "- " + pathConfig + " disconnect def-cap1.json def-cap2.json ...\n" +
                        "- " + pathConfig + " disconnect serial={serial}/remoteDeviceUrl={deviceConnectedUrl}");

            }

        }catch (Exception e){
            System.err.println("Error! \n" + e.getMessage());
        }
    }

    private static Properties loadPropertiesConfig(String pathConfig) throws IOException {
        InputStream inputStream = new FileInputStream(new File(pathConfig));
        Properties properties = new Properties();

        properties.load(inputStream);

        return properties;
    }

    private static JSONObject getJsonObjectFromFile(String path) throws FileNotFoundException {
        InputStream is = new FileInputStream(new File(path));
        JSONTokener jsonTokener = new JSONTokener(is);
        return new JSONObject(jsonTokener);
    }
}
