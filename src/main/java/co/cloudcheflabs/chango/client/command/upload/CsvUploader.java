package co.cloudcheflabs.chango.client.command.upload;


import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "csv",
        subcommands = {CsvLocalUploader.class},
        description = {"Upload CSV to Chango."}
)
public class CsvUploader implements Callable<Integer> {
    @ParentCommand
    private Uploader parent;

    public CsvUploader() {}

    public Integer call() throws Exception {
        return 0;
    }
}