package co.cloudcheflabs.chango.client.command.upload;


import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "json",
        subcommands = {JsonLocalUploader.class, JsonS3Uploader.class},
        description = {"Upload JSON to Chango."}
)
public class JsonUploader implements Callable<Integer> {
    @ParentCommand
    private Uploader parent;

    public JsonUploader() {}

    public Integer call() throws Exception {
        return 0;
    }
}