package co.cloudcheflabs.chango.client.util;

import co.cloudcheflabs.chango.client.domain.ResponseHandler;
import co.cloudcheflabs.chango.client.domain.RestResponse;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;

public class RestCaller {
    private SimpleHttpClient simpleHttpClient;
    private String accessToken;

    public RestCaller(SimpleHttpClient simpleHttpClient, String accessToken) {
        this.simpleHttpClient = simpleHttpClient;
        this.accessToken = accessToken;
    }


    public void callRestAPI(String urlPath, FormBody.Builder builder, boolean post) throws IOException {
        RequestBody body = builder.build();
        Request request = null;
        // post method call.
        if(post) {
            request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .post(body)
                    .build();
        }
        // put method call.
        else {
            request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .put(body)
                    .build();
        }
        RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
        if(restResponse.getStatusCode() == RestResponse.STATUS_OK) {
        } else {
            throw new RuntimeException(restResponse.getErrorMessage());
        }
    }

    public String callGetRestAPI(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
        if(restResponse.getStatusCode() == RestResponse.STATUS_OK) {
            return restResponse.getSuccessMessage();
        } else {
            throw new RuntimeException(restResponse.getErrorMessage());
        }
    }

    public void callRestAPI(String urlPath, FormBody.Builder builder, String method) throws IOException {
        RequestBody body = builder.build();
        Request request = null;
        // post method call.
        if(method.equals("post")) {
            request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .post(body)
                    .build();
        }
        // put method call.
        else if(method.equals("put")) {
            request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .put(body)
                    .build();
        }
        // delete.
        else if(method.equals("delete")) {
            request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
                    .delete(body)
                    .build();
        }
        RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
        String errorLog = null;
        if(restResponse.getStatusCode() == RestResponse.STATUS_OK) {
        } else {
            errorLog = restResponse.getErrorMessage();
            throw new RuntimeException(errorLog);
        }
    }
}
