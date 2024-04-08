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

    public static void sendPartialJsonList(String dataApiServer,
                                           String schema,
                                           String table,
                                           String filePath,
                                           int maxCount,
                                           boolean transactional) {
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
                    sendJsonEvents(dataApiServer, schema, table, jsonList, transactional);
                    jsonList = new ArrayList<>();
                    count = 0;
                } else {
                    count++;
                }
                // read next line
                line = reader.readLine();
            }

            if(jsonList.size() > 0) {
                sendJsonEvents(dataApiServer, schema, table, jsonList, transactional);
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendJsonEvents(String dataApiServer,
                                      String schema,
                                      String table,
                                      List<String> jsonList,
                                      boolean transactional) throws RuntimeException{
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


        String apiPath = (transactional) ? "/v1/event/tx/create" : "/v1/scalable/multi_event_logs/create";
        String urlPath = dataApiServer + apiPath;

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
                throw new RuntimeException("Sending json lines failed.");
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
