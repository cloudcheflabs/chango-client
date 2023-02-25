package com.cloudcheflabs.chango.client.command.upload;


import com.cloudcheflabs.chango.client.domain.ConfigProps;
import com.cloudcheflabs.chango.client.domain.ResponseHandler;
import com.cloudcheflabs.chango.client.domain.RestResponse;
import com.cloudcheflabs.chango.client.util.*;
import io.minio.DownloadObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.cloudcheflabs.chango.client.domain.RestResponse.STATUS_OK;

@Command(
        name = "s3",
        subcommands = {},
        description = {"Upload S3 CSV file to Chango."}
)
public class CsvS3Uploader implements Callable<Integer> {

    @CommandLine.ParentCommand
    private CsvUploader parent;

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
            names = {"--separator"},
            description = {"CSV separator. For instance, tab separator is 'TAB', comma separator is ','."},
            required = true
    )
    private String separator;

    @CommandLine.Option(
            names = {"--is-single-quote"},
            description = {"Escaped value is single-quoted. Default is false."},
            required = false
    )
    private String isSingleQuoted = "false";

    @CommandLine.Option(
            names = {"--bucket"},
            description = {"S3 bucket name."},
            required = true
    )
    private String bucket;

    @CommandLine.Option(
            names = {"--access-key"},
            description = {"S3 access key."},
            required = true
    )
    private String accessKey;

    @CommandLine.Option(
            names = {"--secret-key"},
            description = {"S3 secret key."},
            required = true
    )
    private String secretKey;

    @CommandLine.Option(
            names = {"--endpoint"},
            description = {"S3 endpoint url."},
            required = true
    )
    private String endpoint;

    @CommandLine.Option(
            names = {"--object-name"},
            description = {"Object name in the bucket."},
            required = true
    )
    private String objectName;

    public CsvS3Uploader() {}

    public Integer call() throws Exception {
        // data api server.
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();
        configProps.setDataApiServer(dataApiServer);
        ChangoConfigUtils.updateConfigProps(configProps);
        
        // download csv files from s3 and send csv list to chango using data api.
        String tempDirectory = FileUtils.createChangoTempDirectory();

        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();

        try {
            // Lists objects information recursively.
            Iterable<Result<Item>> results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

            List<String> selectedObjectNames = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                String currentObjectName = item.objectName();
                if (currentObjectName.equals(objectName)) {
                    selectedObjectNames.add(currentObjectName);
                } else if (currentObjectName.startsWith(objectName)) {
                    if (!objectName.endsWith("/")) {
                        objectName = objectName + "/";
                    }
                    String restPath = currentObjectName.replaceAll(objectName, "");
                    if (!restPath.contains("/")) {
                        selectedObjectNames.add(currentObjectName);
                    }
                }
            }
            for (String selectedObjectName : selectedObjectNames) {
                String[] fileName = selectedObjectName.split("/");
                String csvFileName = fileName[fileName.length - 1];
                String downloadedCsvFile = tempDirectory + "/" + csvFileName;
                minioClient.downloadObject(
                        DownloadObjectArgs.builder()
                                .bucket(bucket)
                                .object(selectedObjectName)
                                .filename(downloadedCsvFile)
                                .build());
                System.out.println("Object [" + selectedObjectName + "] downloaded.");
                CsvUploadUtils.uploadCsv(dataApiServer, downloadedCsvFile, schema, table, separator, Boolean.valueOf(isSingleQuoted), configProps);
                System.out.println("CSV file [" + new File(downloadedCsvFile).getName() + "] upload completed.");
            }
            if(selectedObjectNames.size() == 0) {
                System.out.println("Object [" + objectName + "] not found.");
            }
            FileUtils.deleteDirectory(tempDirectory);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }
}