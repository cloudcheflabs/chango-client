package co.cloudcheflabs.chango.client.component;

import co.cloudcheflabs.chango.client.domain.ResponseHandler;
import co.cloudcheflabs.chango.client.domain.RestResponse;
import co.cloudcheflabs.chango.client.util.JsonUtils;
import co.cloudcheflabs.chango.client.util.SimpleHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class ChangoClient {

    private static Logger LOG = LoggerFactory.getLogger(ChangoClient.class);

    private int batchSize;


    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private LinkedBlockingQueue<List<String>> queueForSender = new LinkedBlockingQueue<>();

    private long intervalInMillis;

    private AtomicReference<Throwable> ex = new AtomicReference<>();

    private boolean runExceptionChecker = false;

    public ChangoClient(String token,
                        String dataApiServer,
                        String schema,
                        String table) {
        this(token, dataApiServer, schema, table, 10000, 1000);
    }

    public ChangoClient(String token,
                        String dataApiServer,
                        String schema,
                        String table,
                        int batchSize,
                        long intervalInMillis) {
        this.batchSize = batchSize;
        this.intervalInMillis = intervalInMillis;

        // run timer.
        Timer timer = new Timer("Chango Client Timer");
        timer.schedule(new SendJsonTask(this), 1000, intervalInMillis);

        // run sender thread.
        Thread senderThread = new Thread(new SenderRunnable(
                queueForSender,
                token,
                dataApiServer,
                schema,
                table));

        senderThread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            LOG.error(e.getMessage());
            ex.set(e);
        });
        senderThread.start();
    }

    private void checkCaughtException() {
        while (true) {
            if(ex.get() != null) {
                LOG.info("exception caught!!!!");
                throw new RuntimeException(ex.get());
            } else {
                LOG.info("exception not caught...");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    private static class SenderRunnable implements Runnable {

        private LinkedBlockingQueue<List<String>> queueForSender;
        private String dataApiServer;
        private String schema;
        private String table;
        private String accessToken;
        private ObjectMapper mapper = new ObjectMapper();
        private SimpleHttpClient simpleHttpClient = new SimpleHttpClient();

        public SenderRunnable(LinkedBlockingQueue<List<String>> queueForSender,
                              String token,
                              String dataApiServer,
                              String schema,
                              String table) {
            this.queueForSender = queueForSender;
            this.accessToken = token;
            this.dataApiServer = dataApiServer;
            this.schema = schema;
            this.table = table;
        }

        @Override
        public void run() {
            while (true) {
                List<String> jsonList = null;
                if(!queueForSender.isEmpty()) {
                    jsonList = queueForSender.remove();
                }
                if(jsonList != null && jsonList.size() > 0) {
                    sendJsonEvents(dataApiServer, schema, table, jsonList);
                } else {
                    pause(1000);
                }
            }
        }

        private void pause(long pause) {
            try {
                Thread.sleep(pause);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void sendJsonEvents(String dataApiServer, String schema, String table, List<String> jsonList) throws RuntimeException{

            List<Map<String, Object>> mapList = new ArrayList<>();
            for(String json : jsonList) {
                try {
                    mapList.add(JsonUtils.toMap(mapper, json));
                } catch (Exception e) {
                    LOG.error("This line [" + json + "] is not in json format.");
                }
            }
            int jsonListSize = mapList.size();


            String urlPath = dataApiServer + "/v1/scalable/multi_event_logs/create";

            FormBody.Builder builder = new FormBody.Builder();
            builder.add("schema", schema);
            builder.add("table", table);
            builder.add("jsonList", JsonUtils.toJson(mapList));
            RequestBody body = builder.build();

            Request request = new Request.Builder()
                    .url(urlPath)
                    .header("Accept-Encoding", "gzip")
                    .header("Content-Encoding", "gzip")
                    .addHeader("Authorization", "Bearer " + this.accessToken)
                    .method("POST", gzip(body))
                    .build();

            try {
                RestResponse restResponse = ResponseHandler.doCall(simpleHttpClient.getClient(), request);
                if (restResponse.getStatusCode() != RestResponse.STATUS_OK) {
                    throw new RuntimeException("Sending json lines failed.");
                } else {
                    LOG.info("Json lines with count [" + jsonListSize + "] sent.");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private RequestBody gzip(final RequestBody body) {
            return new RequestBody() {
                @Override public MediaType contentType() {
                    return body.contentType();
                }

                @Override public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override public void writeTo(BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }

    private static class SendJsonTask extends TimerTask {
        private ChangoClient changoClient;

        public SendJsonTask(ChangoClient changoClient) {
            this.changoClient = changoClient;
        }

        @Override
        public void run() {
            changoClient.fire();
        }
    }

    public void fire() {
        putToInternalQueue();
    }

    private void putToInternalQueue() {
        if(!queue.isEmpty()) {
            String[] jsonArray = queue.toArray(new String[0]);
            List<String> jsonList = Arrays.asList(jsonArray);
            queueForSender.add(jsonList);
            queue.clear();
        }
    }

    public void add(String json) {
        if(!runExceptionChecker) {
            checkCaughtException();
        }

        queue.add(json);
        int size = queue.size();
        if(batchSize == size) {
            putToInternalQueue();
        }
    }
}
