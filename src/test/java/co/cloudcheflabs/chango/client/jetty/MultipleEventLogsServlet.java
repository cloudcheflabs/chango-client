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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MultipleEventLogsServlet extends HttpServlet {

    private static Logger LOG = LoggerFactory.getLogger(MultipleEventLogsServlet.class);

    @Autowired
    private Environment env;


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

        resp.setStatus(Response.SC_OK);
        resp.setHeader("Content-Encoding", "");
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String success = "{ 'result': 'SUCCESS'}";
        PrintWriter writer = resp.getWriter();
        try {
            writer.write(success);
        } finally {
            if(writer != null) {
                writer.close();
            }
        }
    }
}
