package staid.openstf;

public class StaidOpenSTF {

    private String url;
    private String token;

    public StaidOpenSTF(String url, String token){
        this.url = url + "/api/v1";
        this.token = token;
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

}
