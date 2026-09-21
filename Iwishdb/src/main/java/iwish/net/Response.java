package iwish.net;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One response message. Built through ok()/error() so the "status" field is
 * always present and always first, exactly as PROTOCOL.md describes.
 */
public class Response {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private Response(String status) {
        fields.put(Protocol.STATUS, status);
    }

    public static Response ok() {
        return new Response(Protocol.STATUS_OK);
    }

    /** Success plus the standard human-readable "message" field. */
    public static Response ok(String message) {
        return ok().with(Protocol.MESSAGE, message);
    }

    /**
     * Error. PROTOCOL.md: on ERROR the response carries a "message" and
     * nothing else is guaranteed -- so nothing else is ever added here.
     */
    public static Response error(String message) {
        return new Response(Protocol.STATUS_ERROR).with(Protocol.MESSAGE, message);
    }

    public Response with(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    public boolean isOk() {
        return Protocol.STATUS_OK.equals(fields.get(Protocol.STATUS));
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public Map<String, Object> fields() {
        return fields;
    }

    /** The exact bytes that go on the wire, minus the terminating newline. */
    public String toJson() {
        return Json.write(fields);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
