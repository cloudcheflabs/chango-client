package co.cloudcheflabs.chango.client.jetty;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventLogsServer {

    private static Logger LOG = LoggerFactory.getLogger(EventLogsServer.class);

    private Server server;
    private int port;
    private int maxThreads;
    private int minThreads;
    private int idleTimeout;

    public EventLogsServer(int port, int maxThreads, int minThreads, int idleTimeout) {
        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.idleTimeout = idleTimeout;

        setup();
        try {
            startServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setup() {
        QueuedThreadPool threadPool =
                new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
        server = new Server(threadPool);

        server.setStopAtShutdown(true);

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(4 * 1024 * 1024);
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        connector.setHost("0.0.0.0");
        connector.setPort(port);
        connector.setName("Event Logs Server");
        connector.setAccepting(true);
        connector.setAcceptedTcpNoDelay(true);
        connector.setReuseAddress(true);
        server.addConnector(connector);

        ConnectHandler connectHandler = new ConnectHandler();
        server.setHandler(connectHandler);

        ServletHolder multipleEventLogsServletHolder = new ServletHolder(new MultipleEventLogsServlet());
        ServletHolder txEventLogsServletHolder = new ServletHolder(new TxEventLogsServlet());

        // Setup proxy servlet
        ServletContextHandler context =
                new ServletContextHandler(connectHandler, "/", ServletContextHandler.SESSIONS);
        context.addServlet(multipleEventLogsServletHolder, "/v1/scalable/multi_event_logs/create");
        context.addServlet(txEventLogsServletHolder, "/v1/event/tx/create");
        context.setMaxFormContentSize(3 * 1024 * 1024 * 1024);

        HandlerCollection handlers = new HandlerCollection();

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setMinGzipSize(0);
        gzipHandler.addIncludedMimeTypes(
                "application/javascript",
                "application/json",
                "application/vnd.go.cd.v1+json",
                "application/vnd.go.cd.v2+json",
                "application/vnd.go.cd.v3+json",
                "application/vnd.go.cd.v4+json",
                "application/vnd.go.cd.v5+json",
                "application/vnd.go.cd.v6+json",
                "application/vnd.go.cd.v7+json",
                "application/vnd.go.cd.v8+json",
                "application/vnd.go.cd.v9+json",
                "application/xhtml+xml",
                "image/svg+xml",
                "text/css",
                "text/html",
                "text/plain",
                "text/xml"
        );
        gzipHandler.addIncludedMethods("HEAD",
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE");
        gzipHandler.addIncludedPaths("/v1/scalable/multi_event_logs/create", "/v1/event/tx/create");
        gzipHandler.setHandler(context);
        gzipHandler.setInflateBufferSize(4 * 1024 * 1024);
        handlers.addHandler(gzipHandler);

        server.setHandler(handlers);
    }

    private void startServer() throws Exception {
        server.start();
        LOG.info("Gzip Handler Server is running on {}...", port);
    }
}
