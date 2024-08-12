package co.cloudcheflabs.chango.client.jetty;

import org.junit.Test;

public class LocalServerTestRunner {
    @Test
    public void runServer() throws Exception {
        EventLogsServer server = new EventLogsServer(8080, 100, 10, 120);
        Thread.sleep(Long.MAX_VALUE);
    }
}
