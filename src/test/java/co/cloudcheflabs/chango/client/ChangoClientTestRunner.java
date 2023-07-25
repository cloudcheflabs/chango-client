package co.cloudcheflabs.chango.client;

import co.cloudcheflabs.chango.client.component.ChangoClient;
import co.cloudcheflabs.chango.client.util.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ChangoClientTestRunner {

    @Test
    public void sendJson() throws Exception {
        String token = System.getProperty("token");
        String dataApiServer = System.getProperty("dataApiServer");
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

        ChangoClient changoClient = new ChangoClient(
                token,
                dataApiServer,
                schema,
                table,
                batchSize,
                interval
        );

        for(String tempJson : jsonList) {
            changoClient.add(tempJson);
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
