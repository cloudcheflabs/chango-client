package co.cloudcheflabs.chango.client.command.init;


import co.cloudcheflabs.chango.client.command.Console;
import co.cloudcheflabs.chango.client.domain.ConfigProps;
import co.cloudcheflabs.chango.client.util.ChangoConfigUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(
        name = "init",
        subcommands = {},
        description = {"Initialize Chango."}
)
public class Init implements Callable<Integer> {
    @ParentCommand
    private Console parent;

    public Init() {
    }

    public Integer call() throws Exception {
        java.io.Console cnsl = System.console();
        if (cnsl == null) {
            System.out.println("No console available");
            return -1;
        } else {
            String token = cnsl.readLine("Enter Token: ");

            // set user info to env.
            ConfigProps configProps = new ConfigProps();
            configProps.setAccessToken(token);

            ChangoConfigUtils.updateConfigProps(configProps);

            System.out.println("Initialization success!");
        }

        return 0;
    }
}