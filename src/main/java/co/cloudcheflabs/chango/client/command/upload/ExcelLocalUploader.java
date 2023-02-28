package co.cloudcheflabs.chango.client.command.upload;


import co.cloudcheflabs.chango.client.domain.ConfigProps;
import co.cloudcheflabs.chango.client.domain.ResponseHandler;
import co.cloudcheflabs.chango.client.domain.RestResponse;
import co.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import co.cloudcheflabs.chango.client.util.SimpleHttpClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

import static co.cloudcheflabs.chango.client.domain.RestResponse.STATUS_OK;

@Command(
        name = "local",
        subcommands = {},
        description = {"Upload local excel file to Chango."}
)
public class ExcelLocalUploader implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ExcelUploader parent;

    @CommandLine.Option(
            names = {"--data-api-server"},
            description = {"Chango Data API Server URL."},
            required = true
    )
    private String dataApiServer;

    @CommandLine.Option(
            names = {"-s", "--schema"},
            description = {"Schema name in trino iceberg catalog."},
            required = true
    )
    private String schema;

    @CommandLine.Option(
            names = {"-t", "--table"},
            description = {"Table name in trino iceberg schema."},
            required = true
    )
    private String table;

    @CommandLine.Option(
            names = {"-f", "--file"},
            description = {"Excel file path."},
            required = true
    )
    private String filePath;

    public ExcelLocalUploader() {}

    public Integer call() throws Exception {
        // data api server.
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();
        configProps.setDataApiServer(dataApiServer);
        ChangoConfigUtils.updateConfigProps(configProps);

        try {
            SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
            File file = new File(filePath);
            if(!file.exists()) {
                System.err.println("File [" + filePath + "] not exist.");
                return -1;
            }
            String urlPath = dataApiServer + "/v1/scalable/excel/upload";

            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), file))
                    .addFormDataPart("schema", schema)
                    .addFormDataPart("table", table)
                    .build();

            Request request = new Request.Builder()
                    .url(urlPath)
                    .addHeader("Authorization", "Bearer " + configProps.getAccessToken())
                    .post(requestBody)
                    .build();
            RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
            if (restResponse.getStatusCode() != STATUS_OK) {
                System.err.println(restResponse.getErrorMessage());
                return -1;
            } else {
                System.out.println("Excel file [" + filePath + "] upload completed.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }
}