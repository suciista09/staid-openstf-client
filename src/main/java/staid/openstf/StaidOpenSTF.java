package staid.openstf;

public class StaidOpenSTF {

    private String url;
    private String token;
    private String email;

    public StaidOpenSTF(String url, String email, String token){
        this.url = url + "/api/v1";
        this.token = token;
        this.email = email;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }
}
