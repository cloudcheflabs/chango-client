package com.cloudcheflabs.chango.client.command.upload;


import com.cloudcheflabs.chango.client.domain.ConfigProps;
import com.cloudcheflabs.chango.client.domain.ResponseHandler;
import com.cloudcheflabs.chango.client.domain.RestResponse;
import com.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import com.cloudcheflabs.chango.client.util.CsvUploadUtils;
import com.cloudcheflabs.chango.client.util.RestUtils;
import com.cloudcheflabs.chango.client.util.SimpleHttpClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

import static com.cloudcheflabs.chango.client.domain.RestResponse.STATUS_OK;

@Command(
        name = "local",
        subcommands = {},
        description = {"Upload local CSV file to Chango."}
)
public class CsvLocalUploader implements Callable<Integer> {

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
            names = {"-f", "--file"},
            description = {"CSV file path."},
            required = false
    )
    private String filePath;

    @CommandLine.Option(
            names = {"-d", "--directory"},
            description = {"Directory path in which multiple csv files exist."},
            required = false
    )
    private String directoryPath;

    public CsvLocalUploader() {}

    public Integer call() throws Exception {
        // data api server.
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();
        configProps.setDataApiServer(dataApiServer);
        ChangoConfigUtils.updateConfigProps(configProps);

        try {
            if (filePath != null) {
                if(!new File(filePath).exists()) {
                    System.err.println("File path [" + filePath + "] does not exist!");
                    return -1;
                }
                CsvUploadUtils.uploadCsv(dataApiServer, filePath, schema, table, separator, Boolean.valueOf(isSingleQuoted), configProps);
                System.out.println("CSV file [" + new File(filePath).getName() + "] upload completed.");
                return 0;
            } else if (directoryPath != null) {
                File directory = new File(directoryPath);
                if (directory.exists()) {
                    for (File f : directory.listFiles()) {
                        CsvUploadUtils.uploadCsv(dataApiServer, f.getAbsolutePath(), schema, table, separator, Boolean.valueOf(isSingleQuoted), configProps);
                        System.out.println("CSV file [" + f.getName() + "] upload completed.");
                    }
                    return 0;
                } else {
                    System.err.println("Directory path [" + directoryPath + "] does not exist!");
                    return -1;
                }
            } else {
                System.err.println("Json file path or directory path needs to be specified.");
                return -1;
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }
}