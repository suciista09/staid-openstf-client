package staid.openstf;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class User {

    private static final Logger LOGGER = LogManager.getLogger(User.class);
    private StaidOpenSTF staidOpenSTF;
    private String email;

    public User(StaidOpenSTF staidOpenSTF){
        this.staidOpenSTF = staidOpenSTF;
    }

    public User retrieveUserInformation(){
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + this.staidOpenSTF.getToken())
                .get(this.staidOpenSTF.getUrl() + "/user");

        if (response.getBody().asString().contains("success")){
            JSONObject jsonObject = new JSONObject(response.asString());

            if (jsonObject.getBoolean("success")){
                email = jsonObject.getJSONObject("user").getString("email");
                LOGGER.info("User email : " + email);
            }else {
                LOGGER.error("Can not get user information. Status code : " + response.getStatusCode() + "Error : " + jsonObject.getString("description"));
            }
        }else {
            LOGGER.error("Can not get user information. Status code : " + response.getStatusCode() + ". Error : " + response.getBody().asString());
        }

        return this;
    }

    public String getEmail(){
        return email;
    }
}
