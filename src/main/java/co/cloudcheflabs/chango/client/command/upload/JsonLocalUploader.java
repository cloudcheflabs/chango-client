package co.cloudcheflabs.chango.client.command.upload;


import co.cloudcheflabs.chango.client.domain.ConfigProps;
import co.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import co.cloudcheflabs.chango.client.util.RestUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Command(
        name = "local",
        subcommands = {},
        description = {"Upload local JSON files to Chango."}
)
public class JsonLocalUploader implements Callable<Integer> {

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
            names = {"-f", "--file"},
            description = {"Json file path."},
            required = false
    )
    private String filePath;

    @CommandLine.Option(
            names = {"-d", "--directory"},
            description = {"Directory path in which multiple json files exist."},
            required = false
    )
    private String directoryPath;

    @CommandLine.Option(
            names = {"--batch-size"},
            description = {"Batch size of json list which will be sent to chango in batch."},
            required = false
    )
    private int batchSize = 100000;

    @CommandLine.Option(
            names = {"--tx"},
            description = {"Use transactional data api."},
            required = false
    )
    private boolean transactional = false;

    public JsonLocalUploader() {}

    public Integer call() throws Exception {
        // data api server.
        ConfigProps configProps = ChangoConfigUtils.getConfigProps();
        configProps.setDataApiServer(dataApiServer);
        ChangoConfigUtils.updateConfigProps(configProps);

        // read local json files and send json list to chango using data api.
        try {
            if (filePath != null) {
                if(!new File(filePath).exists()) {
                    System.err.println("File path [" + filePath + "] does not exist!");
                    return -1;
                }
                RestUtils.sendPartialJsonList(dataApiServer, schema, table, filePath, batchSize, transactional);
                System.out.println("Sending json lines in the file [" + filePath + "] completed.");
            } else if (directoryPath != null) {
                File directory = new File(directoryPath);
                if (directory.exists()) {
                    for (File f : directory.listFiles()) {
                        RestUtils.sendPartialJsonList(dataApiServer, schema, table, f.getAbsolutePath(), batchSize, transactional);
                        System.out.println("Sending json lines in the file [" + f.getName() + "] completed.");
                    }
                } else {
                    System.err.println("Directory path [" + directoryPath + "] does not exist!");
                    return -1;
                }
            } else {
                System.err.println("Json file path or directory path needs to be specified.");
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return -1;
        }

        return 0;
    }
}