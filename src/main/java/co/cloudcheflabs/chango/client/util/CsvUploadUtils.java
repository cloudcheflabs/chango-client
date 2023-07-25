package co.cloudcheflabs.chango.client.util;

import co.cloudcheflabs.chango.client.domain.ConfigProps;
import co.cloudcheflabs.chango.client.domain.ResponseHandler;
import co.cloudcheflabs.chango.client.domain.RestResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.File;

import static co.cloudcheflabs.chango.client.domain.RestResponse.STATUS_OK;

public class CsvUploadUtils {

    public static void uploadCsv(String dataApiServer,
                                String filePath,
                                String schema,
                                String table,
                                String separator,
                                boolean isSingleQuoted,
                                ConfigProps configProps) {
        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        File file = new File(filePath);
        if(!file.exists()) {
            throw new RuntimeException("File [" + filePath + "] not exist.");
        }
        String urlPath = dataApiServer + "/v1/scalable/csv/upload";

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("text/csv"), file))
                .addFormDataPart("schema", schema)
                .addFormDataPart("separator", separator)
                .addFormDataPart("is_single_quote", String.valueOf(isSingleQuoted))
                .addFormDataPart("table", table)
                .build();

        Request request = new Request.Builder()
                .url(urlPath)
                .addHeader("Authorization", "Bearer " + configProps.getAccessToken())
                .post(requestBody)
                .build();
        RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
        if (restResponse.getStatusCode() != STATUS_OK) {
            throw new RuntimeException(restResponse.getErrorMessage());
        }
    }
}
