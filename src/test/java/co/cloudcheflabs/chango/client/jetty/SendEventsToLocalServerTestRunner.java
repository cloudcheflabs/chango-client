package co.cloudcheflabs.chango.client.jetty;

import co.cloudcheflabs.chango.client.component.ChangoClient;
import co.cloudcheflabs.chango.client.util.JsonUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendEventsToLocalServerTestRunner {

    private static Logger LOG = LoggerFactory.getLogger(SendEventsToLocalServerTestRunner.class);

    @Test
    public void sendTxLogs() throws Exception {
        sendLogs(true);
    }

    @Test
    public void sendLogs() throws Exception {
        sendLogs(false);
    }


    @Test
    public void sendTxLogsInBatch() throws Exception {
        sendLogsInBatch(true);
    }

    @Test
    public void sendLogsInBatch() throws Exception {
        sendLogsInBatch(false);
    }

    private void sendLogs(boolean tx) throws Exception{
        String token = "any-token";
        String dataApiServer = "http://localhost:8080";
        String table = "test_table";

        int batchSize = 10000;
        long interval = 1000;
        String schema = "iceberg_db";

        boolean transactional = tx;

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
        for(int k = 0; k < 3; k++) {
            int MAX = 50 * 1000;
            for (int i = 0; i < MAX; i++) {
                Map<String, Object> map = new HashMap<>();

                DateTime dt = DateTime.now();

                String year = String.valueOf(dt.getYear());
                String month = padZero(dt.getMonthOfYear());
                String day = padZero(dt.getDayOfMonth());
                long ts = dt.getMillis(); // in milliseconds.

                map.put("level", "INFO");
                map.put("message", "any log message ... [" + count + "]");
                map.put("ts", ts);
                map.put("year", year);
                map.put("month", month);
                map.put("day", day);

                String json = JsonUtils.toJson(map);

                try {
                    // send json.
                    changoClient.add(json);
                    //Thread.sleep(20);

                    count++;
                    LOG.info("count: {}", count);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Thread.sleep(10 * 1000);
        }
    }

    private void sendLogsInBatch(boolean tx) throws Exception{
        String token = "any-token";
        String dataApiServer = "http://localhost:8080";
        String table = "test_table";

        int batchSize = 10000;
        long interval = 1000;
        String schema = "iceberg_db";

        boolean transactional = tx;

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
        for(int k = 0; k < 25; k++) {
            int MAX = 15000;
            List<String> jsonList = new ArrayList<>();
            for (int i = 0; i < MAX; i++) {
                Map<String, Object> map = new HashMap<>();

                DateTime dt = DateTime.now();

                String year = String.valueOf(dt.getYear());
                String month = padZero(dt.getMonthOfYear());
                String day = padZero(dt.getDayOfMonth());
                long ts = dt.getMillis(); // in milliseconds.

                map.put("level", "INFO");
                map.put("message", "any log message ... [" + count + "]");
                map.put("ts", ts);
                map.put("year", year);
                map.put("month", month);
                map.put("day", day);

                String json = JsonUtils.toJson(map);
                jsonList.add(json);
                count++;
            }

            try {
                changoClient.add(jsonList);
                LOG.info("json sent: {}", count);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Thread.sleep(1000);
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
