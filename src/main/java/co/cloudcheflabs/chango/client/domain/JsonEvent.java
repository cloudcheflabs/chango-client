package co.cloudcheflabs.chango.client.domain;

public class JsonEvent {
    private String schema;
    private String table;
    private String json;

    public JsonEvent(String schema, String table, String json) {
        this.schema = schema;
        this.table = table;
        this.json = json;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public String getJson() {
        return json;
    }
}
