package com.cloudcheflabs.chango.client.command;


import com.cloudcheflabs.chango.client.command.login.Login;
import com.cloudcheflabs.chango.client.command.upload.Uploader;
import picocli.CommandLine.Command;

@Command(
        name = "chango",
        subcommands = {Login.class, Uploader.class},
        version = {"1.0.0"},
        description = {"Chango Client Console."}
)
public class Console implements Runnable {


    public Console() {
    }

    public void run() {
    }
}
