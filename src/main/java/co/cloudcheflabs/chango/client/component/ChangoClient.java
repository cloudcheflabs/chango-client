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
import java.util.concurrent.locks.ReentrantLock;

public class ChangoClient {

    private static Logger LOG = LoggerFactory.getLogger(ChangoClient.class);

    private int batchSize;


    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private LinkedBlockingQueue<List<String>> queueForSender = new LinkedBlockingQueue<>();

    private long intervalInMillis;

    private AtomicReference<Throwable> ex = new AtomicReference<>();

    private boolean transactional = false;

    private EventsSender eventsSender;
    private ReentrantLock lock = new ReentrantLock();


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
        this(token, dataApiServer, schema, table, batchSize, intervalInMillis, false);
    }

    public ChangoClient(String token,
                        String dataApiServer,
                        String schema,
                        String table,
                        int batchSize,
                        long intervalInMillis,
                        boolean transactional) {
        this.batchSize = batchSize;
        this.intervalInMillis = intervalInMillis;
        this.transactional = transactional;

        // run timer.
        Timer timer = new Timer("Chango Client Timer");
        timer.schedule(new SendJsonTask(this), 1000, intervalInMillis);

        // tx events sender.
        if(transactional) {
            eventsSender = new EventsSender(
                    token,
                    dataApiServer,
                    schema,
                    table,
                    transactional
            );
        } else {
            // run sender thread.
            Thread senderThread = new Thread(new SenderRunnable(
                    queueForSender,
                    token,
                    dataApiServer,
                    schema,
                    table,
                    transactional));

            senderThread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
                ex.set(e);
            });
            senderThread.start();
        }
    }

    public void throwException() {
        throw new RuntimeException(ex.get());
    }

    private static class SenderRunnable implements Runnable {
        private LinkedBlockingQueue<List<String>> queueForSender;
        private EventsSender eventsSender;

        public SenderRunnable(LinkedBlockingQueue<List<String>> queueForSender,
                              String token,
                              String dataApiServer,
                              String schema,
                              String table,
                              boolean transactional) {
            this.queueForSender = queueForSender;

            eventsSender = new EventsSender(
                    token,
                    dataApiServer,
                    schema,
                    table,
                    transactional
            );
        }

        @Override
        public void run() {
            while (true) {
                List<String> jsonList = null;
                if(!queueForSender.isEmpty()) {
                    jsonList = queueForSender.remove();
                }
                if(jsonList != null && jsonList.size() > 0) {
                    eventsSender.sendJsonEvents(jsonList);
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
    }


    private static class EventsSender {

        private String dataApiServer;
        private String schema;
        private String table;
        private String accessToken;
        private ObjectMapper mapper = new ObjectMapper();
        private SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        private boolean transactional;

        public EventsSender(String token,
                            String dataApiServer,
                            String schema,
                            String table,
                            boolean transactional) {
            this.accessToken = token;
            this.dataApiServer = dataApiServer;
            this.schema = schema;
            this.table = table;
            this.transactional = transactional;
        }

        public void sendJsonEvents(List<String> jsonList) throws RuntimeException{

            List<Map<String, Object>> mapList = new ArrayList<>();
            for(String json : jsonList) {
                try {
                    mapList.add(JsonUtils.toMap(mapper, json));
                } catch (Exception e) {
                    LOG.error("This line [" + json + "] is not in json format.");
                }
            }
            int jsonListSize = mapList.size();


            String apiPath = (transactional) ? "/v1/event/tx/create" : "/v1/scalable/multi_event_logs/create";
            String urlPath = dataApiServer + apiPath;

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
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Json lines with count [" + jsonListSize + "] sent.");
                    }
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
        if(transactional) {
            lock.lock();
            try {
                sendEventsDirectly();
            } finally {
                lock.unlock();
            }
        } else {
            putToInternalQueue();
        }
    }

    private void putToInternalQueue() {
        if(!queue.isEmpty()) {
            String[] jsonArray = queue.toArray(new String[0]);
            List<String> jsonList = Arrays.asList(jsonArray);
            queueForSender.add(jsonList);
            queue.clear();
        }
    }

    private void sendEventsDirectly() {
        if(!queue.isEmpty()) {
            String[] jsonArray = queue.toArray(new String[0]);
            List<String> jsonList = Arrays.asList(jsonArray);
            try {
                eventsSender.sendJsonEvents(jsonList);
            } catch (Exception e) {
                ex.set(e);
            }
            queue.clear();
        }
    }

    public void add(String json) throws Exception {
        if(ex.get() != null) {
            throw new RuntimeException(ex.get());
        }

        if(transactional) {
            lock.lock();
            try {
                queue.add(json);
                int size = queue.size();
                if (batchSize == size) {
                    sendEventsDirectly();
                }
            } finally {
                lock.unlock();
            }
        } else {
            queue.add(json);
            int size = queue.size();
            if (batchSize == size) {
                putToInternalQueue();
            }
        }
    }
}
