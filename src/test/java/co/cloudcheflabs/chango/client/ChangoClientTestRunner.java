package co.cloudcheflabs.chango.client;

import co.cloudcheflabs.chango.client.component.ChangoClient;
import co.cloudcheflabs.chango.client.util.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ChangoClientTestRunner {

    @Test
    public void sendJson() throws Exception {
        String adminServer = "https://chango-admin-oci.cloudchef-labs.com";
        String user = System.getProperty("user");
        String password = System.getProperty("password");
        String dataApiServer = "https://chango-data-api-jetty-oci-user1.cloudchef-labs.com";
        int batchSize = 10000;
        long interval = 1000;
        String schema = "iceberg_db";
        String table = "test_iceberg";

        String json = StringUtils.fileToString("data/test.json", true);
        String lines[] = json.split("\\r?\\n");

        String line = lines[0];
        List<String> jsonList = new ArrayList<>();
        for(int i = 0; i < 99900; i++) {
            jsonList.add(line);
        }

        ChangoClient changoClient = new ChangoClient(adminServer,
                user,
                password,
                dataApiServer,
                schema,
                table,
                batchSize,
                interval);
        int count = 0;
        for(String tempJson : jsonList) {
            changoClient.add(json);
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
