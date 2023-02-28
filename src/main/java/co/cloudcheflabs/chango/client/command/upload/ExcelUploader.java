package co.cloudcheflabs.chango.client.command.upload;


import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "excel",
        subcommands = {ExcelLocalUploader.class},
        description = {"Upload Excel to Chango."}
)
public class ExcelUploader implements Callable<Integer> {
    @ParentCommand
    private Uploader parent;

    public ExcelUploader() {}

    public Integer call() throws Exception {
        return 0;
    }
}