package co.cloudcheflabs.chango.client.command;


import co.cloudcheflabs.chango.client.command.init.Init;
import co.cloudcheflabs.chango.client.command.upload.Uploader;
import picocli.CommandLine.Command;

@Command(
        name = "chango",
        subcommands = {Init.class, Uploader.class},
        version = {"2.0.0"},
        description = {"Chango Client Console."}
)
public class Console implements Runnable {


    public Console() {
    }

    public void run() {
    }
}
