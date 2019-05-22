package staid.openstf;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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
        boolean present = jsonObject.getBoolean("present");
        boolean ready = jsonObject.getBoolean("ready");
        boolean using = jsonObject.getBoolean("using");

        if (!present || !ready || using || !jsonObject.isNull("owner")){
            LOGGER.error("Device is not available.");
            return false;
        }

        LOGGER.info("device " + serial + " is available");
        return true;
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

    private boolean disconnectRemoteDevice(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .delete(this.staidOpenSTF.getUrl() + "/user/devices/" + serial + "/remoteConnect");

        if (response.getStatusCode()!=200 || !response.getBody().asString().contains("success")){
            //put logger
            LOGGER.warn("Disconnect device " + serial + " is failed. please disconnect manual with this following command \n" +
                    "curl -X DELETE -H \"Authorization: Bearer YOUR-TOKEN-HERE\" https://stf.example.org/api/v1/user/devices/"+serial+"/remoteConnect");
            return false;
        }

        JSONObject jsonObject = new JSONObject(response.asString());
        boolean success = jsonObject.getBoolean("success");;
        if (!success){
            LOGGER.warn("Disconnect device " + serial + " is failed. please disconnect manual with this following command \n" +
                    "curl -X DELETE -H \"Authorization: Bearer YOUR-TOKEN-HERE\" https://stf.example.org/api/v1/user/devices/"+serial+"/remoteConnect");
            return false;
        }

        return success;
    }

    public void adbConnect() throws IOException, InterruptedException {
        executeCommand("adb connect " + remoteDeviceUrl);
    }

    public void adbDisconnect() throws IOException, InterruptedException {
        executeCommand("adb disconnect " + remoteDeviceUrl);
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
            openDevice();
            this.remoteDeviceUrl = connectUrlRemoteDevice();
            adbConnect();
        }

    }

    private  boolean isAlreadyConnectedToDevice() {
        String json = getDevice();

        JSONObject jsonObject = new JSONObject(json);
        if (!jsonObject.getJSONObject("device").isNull("owner") && jsonObject.getJSONObject("device").getJSONObject("owner").getString("email").equals(staidOpenSTF.getEmail())
                && jsonObject.getJSONObject("device").getBoolean("remoteConnect") ){
            this.remoteDeviceUrl = jsonObject.getJSONObject("device").getString("remoteConnectUrl");

            return true;
        }

        return false;
    }

    public String getSerial() {
        return serial;
    }

    public String getRemoteDeviceUrl() {
        return remoteDeviceUrl;
    }

}
