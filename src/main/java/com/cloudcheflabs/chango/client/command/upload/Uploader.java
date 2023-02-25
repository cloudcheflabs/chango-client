package com.cloudcheflabs.chango.client.command.upload;


import com.cloudcheflabs.chango.client.command.Console;
import com.cloudcheflabs.chango.client.util.CsvUploadUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "upload",
        subcommands = {JsonUploader.class, ExcelUploader.class, CsvUploader.class},
        description = {"Upload JSON / Excel data to Chango."}
)
public class Uploader implements Callable<Integer> {
    @ParentCommand
    private Console parent;

    public Uploader() {}

    public Integer call() throws Exception {
        return 0;
    }
}