package co.cloudcheflabs.chango.client;

import co.cloudcheflabs.chango.client.component.ChangoClient;
import co.cloudcheflabs.chango.client.util.JsonUtils;
import co.cloudcheflabs.chango.client.util.JvmUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SendLogsToDataAPI {

    private static Logger LOG = LoggerFactory.getLogger(SendLogsToDataAPI.class);

    @Test
    public void sendLogs() throws Exception {
        String token = System.getProperty("token");
        String dataApiServer = System.getProperty("dataApiServer");
        String table = System.getProperty("table");

        int batchSize = 10000;
        long interval = 1000;
        String schema = "iceberg_db";

        ChangoClient changoClient = new ChangoClient(
                token,
                dataApiServer,
                schema,
                table,
                batchSize,
                interval
        );

        long count = 0;
        while (true) {
            int MAX = 50 * 1000;
            for (int i = 0; i < MAX; i++) {
                Map<String, Object> map = new HashMap<>();

                DateTime dt = DateTime.now();

                map.put("level", "INFO");
                map.put("message", "any log message ... [" + count + "]");
                map.put("ts", dt.toString());

                String json = JsonUtils.toJson(map);

                try {
                    // send json.
                    changoClient.add(json);

                    count++;
                } catch (Exception e) {
                    LOG.error(e.getMessage());

                    // reconstruct chango client.
                    changoClient = new ChangoClient(
                            token,
                            dataApiServer,
                            schema,
                            table,
                            batchSize,
                            interval
                    );
                    LOG.info("Chango client reconstructed.");
                    Thread.sleep(1000);
                }
            }
            Thread.sleep(10 * 1000);
            LOG.info("log [{}] sent...", count);
            LOG.info("total memory: {}, max memory: {}, free memory: {}, used memory: {}",
                    JvmUtils.getTotalMemoryInMiB(),
                    JvmUtils.getMaxMemoryInMiB(),
                    JvmUtils.getFreeMemoryInMiB(),
                    JvmUtils.getUsedMemoryInMiB());
        }
    }


    @Test
    public void sendLogsTx() throws Exception {
        String token = System.getProperty("token");
        String dataApiServer = System.getProperty("dataApiServer");
        String table = System.getProperty("table");
        boolean tx = Boolean.valueOf(System.getProperty("tx", "false"));

        int batchSize = 10000;
        long interval = 1000;
        String schema = "iceberg_db";

        ChangoClient changoClient = new ChangoClient(
                token,
                dataApiServer,
                schema,
                table,
                batchSize,
                interval,
                tx
        );

        long count = 0;
        while (true) {
            int MAX = 10;
            for (int i = 0; i < MAX; i++) {
                Map<String, Object> map = new HashMap<>();

                DateTime dt = DateTime.now();

                map.put("level", "INFO");
                map.put("message", "any log message ... [" + count + "]");
                map.put("ts", dt.toString());

                String json = JsonUtils.toJson(map);

                try {
                    // send json.
                    changoClient.add(json);

                    count++;
                } catch (Exception e) {
                    LOG.error(e.getMessage());

                    // reconstruct chango client.
                    changoClient = new ChangoClient(
                            token,
                            dataApiServer,
                            schema,
                            table,
                            batchSize,
                            interval
                    );
                    LOG.info("Chango client reconstructed.");
                    Thread.sleep(1000);
                }
            }
            Thread.sleep(5 * 1000);
            LOG.info("log [{}] sent...", count);
            LOG.info("total memory: {}, max memory: {}, free memory: {}, used memory: {}",
                    JvmUtils.getTotalMemoryInMiB(),
                    JvmUtils.getMaxMemoryInMiB(),
                    JvmUtils.getFreeMemoryInMiB(),
                    JvmUtils.getUsedMemoryInMiB());
        }
    }

    private String padZero(int value) {
        String strValue = String.valueOf(value);
        if(strValue.length() == 1) {
            strValue = "0" + strValue;
        }
        return strValue;
    }
}
