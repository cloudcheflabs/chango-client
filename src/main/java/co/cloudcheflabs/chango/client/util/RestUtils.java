package co.cloudcheflabs.chango.client.util;

import co.cloudcheflabs.chango.client.domain.ConfigProps;
import co.cloudcheflabs.chango.client.domain.ResponseHandler;
import co.cloudcheflabs.chango.client.domain.RestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestUtils {

    public static String loginAgain() throws RuntimeException {
        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();

        String urlPath = configProps.getAdminServer() + "/v1/login";

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("user", configProps.getUser());
        builder.add("password", configProps.getPassword());
        RequestBody body = builder.build();

        try {
            Request request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .post(body)
                    .build();
            RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
            if (restResponse.getStatusCode() == RestResponse.STATUS_OK) {
                String authJson = restResponse.getSuccessMessage();
                Map<String, Object> map = JsonUtils.toMap(new ObjectMapper(), authJson);
                String expiration = (String) map.get("expiration");
                String accessToken = (String) map.get("token");

                // update access token.
                configProps.setAccessToken(accessToken);
                configProps.setExpiration(expiration);
                ChangoConfigUtils.updateConfigProps(configProps);
                return accessToken;
            } else {
                System.err.println(restResponse.getErrorMessage());
                System.err.println("Login failed!");
                throw new RuntimeException("Login failed!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Login failed!");
            throw new RuntimeException("Login failed!");
        }
    }

    public static void sendPartialJsonList(String dataApiServer,
                                           String schema,
                                           String table,
                                           String filePath,
                                           int maxCount) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            List<String> jsonList = new ArrayList<>();

            int count = 0;
            int MAX = maxCount;
            while (line != null) {
                jsonList.add(line);
                if(count == MAX -1) {
                    sendJsonEvents(dataApiServer, schema, table, jsonList);
                    jsonList = new ArrayList<>();
                    count = 0;
                } else {
                    count++;
                }
                // read next line
                line = reader.readLine();
            }

            if(jsonList.size() > 0) {
                sendJsonEvents(dataApiServer, schema, table, jsonList);
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendJsonEvents(String dataApiServer, String schema, String table, List<String> jsonList) throws RuntimeException{
        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> mapList = new ArrayList<>();
        for(String json : jsonList) {
            try {
                mapList.add(JsonUtils.toMap(mapper, json));
            } catch (Exception e) {
                System.err.println("This line [" + json + "] is not in json format.");
            }
        }
        int jsonListSize = mapList.size();


        String urlPath = dataApiServer + "/v1/scalable/multi_event_logs/create";

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("schema", schema);
        builder.add("table", table);
        builder.add("jsonList", JsonUtils.toJson(mapList));
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url(urlPath)
                .header("Accept-Encoding", "gzip")
                .header("Content-Encoding", "gzip")
                .addHeader("Authorization", "Bearer " + configProps.getAccessToken())
                .method("POST", gzip(body))
                .build();

        try {
            RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
            if (restResponse.getStatusCode() != RestResponse.STATUS_OK) {
                // get new access token with login again.
                String accessToken = loginAgain();
                System.out.println("Got new access token.");
                // try one more time.
                request = new Request.Builder()
                        .url(urlPath)
                        .header("Accept-Encoding", "gzip")
                        .header("Content-Encoding", "gzip")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .method("POST", gzip(body))
                        .build();
                restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
                if (restResponse.getStatusCode() != RestResponse.STATUS_OK) {
                    throw new RuntimeException("Sending json lines failed.");
                } else {
                    System.out.println("Json lines with count [" + jsonListSize + "] sent.");
                }
            } else {
                System.out.println("Json lines with count [" + jsonListSize + "] sent.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override public MediaType contentType() {
                return body.contentType();
            }

            @Override public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}
