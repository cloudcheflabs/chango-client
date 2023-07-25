package co.cloudcheflabs.chango.client.domain;

import java.io.Serializable;

public class ConfigProps implements Serializable {

    private String accessToken;
    private String dataApiServer;

    public ConfigProps() {}

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getDataApiServer() {
        return dataApiServer;
    }

    public void setDataApiServer(String dataApiServer) {
        this.dataApiServer = dataApiServer;
    }
}
