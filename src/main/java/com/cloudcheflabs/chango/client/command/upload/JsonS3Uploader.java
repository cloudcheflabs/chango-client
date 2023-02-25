package com.cloudcheflabs.chango.client.command.upload;


import com.cloudcheflabs.chango.client.domain.ConfigProps;
import com.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import com.cloudcheflabs.chango.client.util.FileUtils;
import com.cloudcheflabs.chango.client.util.RestUtils;
import io.minio.DownloadObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "s3",
        subcommands = {},
        description = {"Upload S3 JSON files to Chango."}
)
public class JsonS3Uploader implements Callable<Integer> {

    @CommandLine.ParentCommand
    private JsonUploader parent;

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

    @CommandLine.Option(
            names = {"--batch-size"},
            description = {"Batch size of json list which will be sent to chango in batch."},
            required = false
    )
    private int batchSize = 100000;

    public JsonS3Uploader() {}

    public Integer call() throws Exception {
        // data api server.
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();
        configProps.setDataApiServer(dataApiServer);
        ChangoConfigUtils.updateConfigProps(configProps);

        // download json files from s3 and send json list to chango using data api.
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
                String jsonFileName = fileName[fileName.length - 1];
                String downloadedJsonFile = tempDirectory + "/" + jsonFileName;
                minioClient.downloadObject(
                        DownloadObjectArgs.builder()
                                .bucket(bucket)
                                .object(selectedObjectName)
                                .filename(downloadedJsonFile)
                                .build());
                System.out.println("Object [" + selectedObjectName + "] downloaded.");
                RestUtils.sendPartialJsonList(dataApiServer, schema, table, downloadedJsonFile, batchSize);
                System.out.println("Sending json lines in the file [" + jsonFileName + "] completed.");
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