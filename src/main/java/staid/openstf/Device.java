package staid.openstf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class Device {

    private static final Logger LOGGER = LogManager.getLogger(Device.class);
    private StaidOpenSTF staidOpenSTF;
    private String serial;
    private String remoteDeviceUrl;

    public Device(StaidOpenSTF openSTF, String serial){
        this.staidOpenSTF = openSTF;
        this.serial = serial;
    }

    public Device(StaidOpenSTF openSTF){
        this.staidOpenSTF = openSTF;
    }

    public String getDevices(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .accept("application/json")
                .get(this.staidOpenSTF.getUrl() + "/devices");

        if (!response.getBody().asString().contains("success")){
            LOGGER.error("Error while getDevices()");
            return null;
        }

        // LOGGER.info(response.asString());
        return response.asString();
    }

    public String getUserDevices(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .accept("application/json")
                .get(this.staidOpenSTF.getUrl() + "/user/devices");

        if (!response.getBody().asString().contains("success")){
            LOGGER.error("Error while getUserDevices()");
            return null;
        }

        LOGGER.info(response.asString());
        return response.asString();
    }

    public String getDevice(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .accept("application/json")
                .get(this.staidOpenSTF.getUrl() + "/devices/" + serial);

        if (!response.getBody().asString().contains("success")){
            LOGGER.error("Error while get serial device : " + serial);
        }
        LOGGER.info(response.asString());

        return response.asString();
    }

    public boolean isDeviceFound(){
        String response = getDevice();

        JSONObject jsonObject = new JSONObject(response);
        boolean success = jsonObject.getBoolean("success");
        if (!success){
            LOGGER.error(jsonObject.getString("description"));
            return false;
        }
        LOGGER.info("device " + serial + " is found");
        return true;

    }

    private boolean isDeviceFound(JSONObject jsonObject){
        boolean success = jsonObject.getBoolean("success");
        if (!success){
            LOGGER.error(jsonObject.getString("description"));
            return false;
        }

        LOGGER.info("device is found");
        return true;
    }

    public boolean isDeviceAvailable(){
        String response = getDevice();

        JSONObject jsonObject = new JSONObject(response);

        if (!isDeviceFound(jsonObject)){
            LOGGER.error("device " + serial + " is not found.");
            return false;
        }

        JSONObject jsonObject_device = jsonObject.getJSONObject("device");
        boolean present = jsonObject_device.getBoolean("present");
        boolean ready = jsonObject_device.getBoolean("ready");
        boolean using = jsonObject_device.getBoolean("using");

        if (!present || !ready || using || !jsonObject.isNull("owner")){
            LOGGER.error("Device is not available.");
            return false;
        }

        LOGGER.info("device " + serial + " is available");
        return true;
    }

    public String getOnlyAvailableDevice() {
        String response = getDevices();
        ArrayList<String> listofAvailableDevice = new ArrayList<String>();
        
        JSONObject jsonObject = new JSONObject(response);
        JSONArray listOfDevices = jsonObject.getJSONArray("devices");

        listOfDevices.forEach( device -> {
            JSONObject deviceObj = (JSONObject)device;
            boolean present = deviceObj.getBoolean("present");
            boolean using = deviceObj.getBoolean("using");
            boolean isConnected = deviceObj.getBoolean("ready");
            boolean isOwnerNull = deviceObj.isNull("owner");
            String deviceSerial = deviceObj.getString("serial");
            
            if (isConnected && isOwnerNull && present && !using) {
                listofAvailableDevice.add(deviceSerial);
            }
        });
        
        System.out.println("Jumlah Device " +listofAvailableDevice.size());
        String targetDevice = listofAvailableDevice.size() > 0 ? listofAvailableDevice.get(0) : null;
        return targetDevice;
    }

    public String waitAvailableDevice() {
        String targetUdid = "";
        
        int counter = 0;
        LOGGER.info("Waiting available device... ");
        try {
                while ((targetUdid == "" || targetUdid == null) && counter < 120) {
                targetUdid = getOnlyAvailableDevice();
                TimeUnit.MINUTES.sleep(1);
                counter++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return targetUdid;
    }

    private boolean openDevice(){

        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .contentType("application/json")
                .body("{\"serial\":\""+serial+"\"}")
                .post(this.staidOpenSTF.getUrl() + "/user/devices");

        if (response.getStatusCode()!=200){
            LOGGER.error("Error while use devices. Error code : " + response.getStatusCode() + ". Error Details : " + response.asString());
            return false;
        }

        JSONObject jsonObject = new JSONObject(response.asString());
        LOGGER.info(jsonObject.getString("description"));
        return jsonObject.getBoolean("success");

    }

    public void stopUsingOpenSTF(){
        disconnectRemoteDevice();
        releaseOpenSTFDevice();
    }

    public boolean releaseOpenSTFDevice(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .delete(this.staidOpenSTF.getUrl() + "/user/devices/" + serial);

        if (response.getStatusCode()!=200){
            LOGGER.error("Error while releasing device " + serial + ". Please release manual with this following command : \n" +
                    "curl -X DELETE -H \"Authorization: Bearer YOUR-TOKEN-HERE\" "+this.staidOpenSTF.getUrl()+"/user/devices/"+serial+"\n" +
                    " Error code : " + response.getStatusCode() + ". Error Details : " + response.asString());
            return false;
        }

        JSONObject jsonObject = new JSONObject(response.asString());
        LOGGER.info(jsonObject.getString("description"));
        return jsonObject.getBoolean("success");
    }

    private String connectUrlRemoteDevice() throws Exception {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .contentType("application/json")
                .post(this.staidOpenSTF.getUrl() + "/user/devices/" + serial + "/remoteConnect");

        if (response.getBody().asString().contains("success")){
            JSONObject jsonObject = new JSONObject(response.asString());
            if (jsonObject.getBoolean("success")){
                return jsonObject.getString("remoteConnectUrl");
            }else {
                throw new Exception("Error while connect to remote device " + serial + ". Error details : " + jsonObject.getString("description"));
            }
        }else {
            throw new Exception("Error while connect to remote device " + serial + ". Status code : " + response.getStatusCode() + ". Details : " + response.asString());
        }
    }

    public boolean disconnectRemoteDevice(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .delete(this.staidOpenSTF.getUrl() + "/user/devices/" + serial + "/remoteConnect");

        if (response.getStatusCode()!=200 || !response.getBody().asString().contains("success")){
            //put logger
            LOGGER.info("Disconnect device " + serial + " is failed. please disconnect manual with this following command \n" +
                    "curl -X DELETE -H \"Authorization: Bearer YOUR-TOKEN-HERE\" https://stf.example.org/api/v1/user/devices/"+serial+"/remoteConnect\n" +
                    "Status Code  : " +response.getStatusCode());
            return false;
        }

        JSONObject jsonObject = new JSONObject(response.asString());
        boolean success = jsonObject.getBoolean("success");;
        if (!success){
            LOGGER.info("Disconnect device " + serial + " is failed. please disconnect manual with this following command \n" +
                    "curl -X DELETE -H \"Authorization: Bearer YOUR-TOKEN-HERE\" https://stf.example.org/api/v1/user/devices/"+serial+"/remoteConnect\n" +
                    "Details : " + jsonObject.getString("description"));
            return false;
        }

        LOGGER.info(jsonObject.getString("description"));
        return success;
    }

    public void adbConnect() throws IOException, InterruptedException {
        executeCommand("adb connect " + remoteDeviceUrl);
        LOGGER.info("adb connect " + remoteDeviceUrl);
    }

    public void adbDisconnect() throws IOException, InterruptedException {
        executeCommand("adb disconnect " + remoteDeviceUrl);
        LOGGER.info("adb disconnect " +remoteDeviceUrl);
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (SystemUtils.IS_OS_WINDOWS){
            processBuilder.command("cmd.exe", "/c", command);
        }else {
            processBuilder.command("sh", "-c", command);
        }

        processBuilder.directory(new File(System.getProperty("user.home")));
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = bufferedReader.readLine()) != null){
            output.append(line).append("\n");
        }

        int exitVal = process.waitFor();
        if (exitVal == 0){
            LOGGER.info(command +" is success.");
            LOGGER.info(output);

        }else {
            LOGGER.error(command + " is failed.");
        }

    }

    public void connectToSTFDevice() throws Exception {

        if (!isAlreadyConnectedToDevice()) {
            LOGGER.info("Not connected yet to openSTF. Connecting...");
            openDevice();
            this.remoteDeviceUrl = connectUrlRemoteDevice();
            //adbConnect();
        }else {
            LOGGER.info("Already connected to openSTF with serial " + this.serial);
        }

    }

    private  boolean isAlreadyConnectedToDevice() {
        String json = getDevice();
        String email = (new User(staidOpenSTF)).retrieveUserInformation().getEmail();

        JSONObject jsonObject = new JSONObject(json);
        if (!jsonObject.getJSONObject("device").isNull("owner") && jsonObject.getJSONObject("device").getJSONObject("owner").getString("email").equals(email)
                && jsonObject.getJSONObject("device").getBoolean("remoteConnect") ){
            this.remoteDeviceUrl = jsonObject.getJSONObject("device").getString("remoteConnectUrl");

            return true;
        }

        return false;
    }

    public String getAvailableDeviceByCriteria(Map<String,String> criteria){

        //get list all of devices
        String devices_s = getDevices();

        if (devices_s != null){
            JSONObject jsonObject = new JSONObject(devices_s);

            JSONArray devices = jsonObject.getJSONArray("devices");

            for (int i = 0 ; i < devices.length(); i++){
                JSONObject device = devices.getJSONObject(i);

                if (isMatchWithCriteria(device, criteria)){
                    this.serial = device.getString("serial");
                    if (isDeviceAvailable()){
                        return getSerial();
                    }

                }
            }
        }

        LOGGER.error("Can not find device with criteria " + criteria);
        return null;
    }

    private boolean isMatchWithCriteria(JSONObject device, Map<String, String> criteria) {

        int counter_true = 0;
        for (Map.Entry entry : criteria.entrySet()){

            if (device.has(entry.getKey().toString())){
                if (device.getString(entry.getKey().toString()).equals(entry.getValue().toString())){
                    counter_true ++;
                }else {
                    return false;
                }
            }else {
                return false;
            }
        }

        if (counter_true == criteria.size()){
            return true;
        }else {
            return false;
        }
    }

    public String getSerial() {
        return serial;
    }

    public String getSerial(String remoteDeviceUrl){
        String json = getUserDevices();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray devices = jsonObject.getJSONArray("devices");

        for (int i = 0 ; i < devices.length() ; i++){
            if (devices.getJSONObject(i).getString("remoteConnectUrl").equalsIgnoreCase(remoteDeviceUrl)){
                return devices.getJSONObject(i).getString("serial");
            }
        }

        return null;
    }

    public String getRemoteDeviceUrl() {
        return remoteDeviceUrl;
    }

    public String getRemoteDeviceUrl(String serial){
        String json = getUserDevices();

        JSONObject jsonObject = new JSONObject(json);
        JSONArray devices = jsonObject.getJSONArray("devices");

        for (int i = 0 ; i < devices.length() ; i++){
            if (devices.getJSONObject(i).getString("serial").equalsIgnoreCase(serial)){
                return devices.getJSONObject(i).getString("remoteConnectUrl");
            }
        }

        return null;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setRemoteDeviceUrl(String remoteDeviceUrl) {
        this.remoteDeviceUrl = remoteDeviceUrl;
    }

}
