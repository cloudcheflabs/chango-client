package co.cloudcheflabs.chango.client;

import co.cloudcheflabs.chango.client.component.ChangoClient;
import co.cloudcheflabs.chango.client.util.JsonUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChangoClientHiddenPartitionTransactionalTestRunner {

    @Test
    public void sendJson() throws Exception {
        String token = System.getProperty("token");
        String dataApiServer = System.getProperty("dataApiServer");
        String schema = System.getProperty("schema", "iceberg_db");
        String table = System.getProperty("table", "hidden_partitioning");

        int batchSize = 10000;
        long interval = 1000;
        boolean transactional = true;

        ChangoClient changoClient = new ChangoClient(
                token,
                dataApiServer,
                schema,
                table,
                batchSize,
                interval,
                transactional
        );

        long count = 0;

        while (true) {
            List<String> jsonList = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                count++;

                Map<String, Object> map = new HashMap<>();
                map.put("id", count);
                map.put("log", "any log " + count);
                map.put("ts", DateTime.now().toString());

                String json = JsonUtils.toJson(map);
                jsonList.add(json);
            }


            for (String tempJson : jsonList) {
                changoClient.add(tempJson);
            }

            TimeUnit.SECONDS.sleep(5);
            System.out.println("message sent: " + count);
        }
    }
}
