package co.cloudcheflabs.chango.client.jetty;

import co.cloudcheflabs.chango.client.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TxEventLogsServlet extends HttpServlet {

    private static Logger LOG = LoggerFactory.getLogger(TxEventLogsServlet.class);

    private ObjectMapper mapper = new ObjectMapper();

    private AtomicLong count = new AtomicLong(0);
    private AtomicLong totalCount = new AtomicLong(0);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // handle multiple json list.
        String schema = req.getParameter("schema");
        LOG.info("schema: {}", schema);
        String table = req.getParameter("table");
        LOG.info("table: {}", table);
        String jsonList = req.getParameter("jsonList");
        List<Map<String, Object>> mapList = JsonUtils.toMapList(mapper, jsonList);
        LOG.info("json list size: {}", mapList.size());

        long currentTotalCount = totalCount.addAndGet(mapList.size());
        LOG.info("total count: {}", currentTotalCount);

        PrintWriter writer = null;
        try {
            // throw exception.
            if(count.incrementAndGet() % 20 == 0) {
                throw new ServletException("Exception occurred.");
            }

            resp.setStatus(Response.SC_OK);
            resp.setHeader("Content-Encoding", "");
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String success = "{ 'result': 'SUCCESS'}";
            writer = resp.getWriter();
            writer.write(success);
        } catch (Exception e) {
            resp.setStatus(Response.SC_INTERNAL_SERVER_ERROR);
            resp.setHeader("Content-Encoding", "");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("application/json");
            String failure = "{ 'result': 'FAILURE', 'message': '" + e.getMessage() + "'}";
            writer = resp.getWriter();
            writer.write(failure);
        }
        finally {
            if(writer != null) {
                writer.close();
            }
        }
    }
}
