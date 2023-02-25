package com.cloudcheflabs.chango.client.command.login;


import com.cloudcheflabs.chango.client.command.Console;
import com.cloudcheflabs.chango.client.domain.ConfigProps;
import com.cloudcheflabs.chango.client.domain.ResponseHandler;
import com.cloudcheflabs.chango.client.domain.RestResponse;
import com.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import com.cloudcheflabs.chango.client.util.JsonUtils;
import com.cloudcheflabs.chango.client.util.SimpleHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.cloudcheflabs.chango.client.domain.RestResponse.STATUS_OK;

@Command(
        name = "login",
        subcommands = {},
        description = {"Login to Chango."}
)
public class Login implements Callable<Integer> {
    @ParentCommand
    private Console parent;
    @CommandLine.Option(
            names = {"-s", "--admin-server"},
            description = {"Chango admin server url."},
            required = true
    )
    private String server;

    public Login() {
    }

    public Integer call() throws Exception {
        java.io.Console cnsl = System.console();
        if (cnsl == null) {
            System.out.println("No console available");
            return -1;
        } else {
            String user = cnsl.readLine("Enter username: ");
            char[] password = cnsl.readPassword("Enter password: ", new Object[0]);
            SimpleHttpClient simpleHttpClient = new SimpleHttpClient();

            String urlPath = server + "/v1/login";

            FormBody.Builder builder = new FormBody.Builder();
            builder.add("user", user);
            builder.add("password", new String(password));
            RequestBody body = builder.build();

            Request request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .post(body)
                    .build();
            try {
                RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
                if (restResponse.getStatusCode() == STATUS_OK) {
                    String authJson = restResponse.getSuccessMessage();
                    Map<String, Object> map = JsonUtils.toMap(new ObjectMapper(), authJson);
                    String expiration = (String) map.get("expiration");
                    String accessToken = (String) map.get("token");

                    // set user info to env.
                    ConfigProps configProps = new ConfigProps();
                    configProps.setAdminServer(server);
                    configProps.setUser(user);
                    configProps.setPassword(new String(password));
                    configProps.setAccessToken(accessToken);
                    configProps.setExpiration(expiration); // data format like '2022-11-14T15:36:10.000Z'.

                    ChangoConfigUtils.updateConfigProps(configProps);

                    System.out.println("Login success!");
                    Arrays.fill(password, ' ');
                    return 0;
                } else {
                    System.err.println(restResponse.getErrorMessage());
                    System.err.println("Login failed!");
                    return -1;
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.err.println("Login failed!");
                return -1;
            }
        }
    }
}