package iwish.net;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * One parsed request message.
 *
 * The typed getters below are the only way handlers should read fields.
 * They throw ProtocolException for a missing or wrong-typed field, which
 * ClientHandler turns into the standard ERROR response -- so a client that
 * sends {"action":"CONTRIBUTE","amount":"lots"} gets a clean error instead
 * of a ClassCastException killing its thread (Person 2, task 11).
 */
public class Request {

    /** A request that is valid JSON but does not fit the protocol. */
    public static class ProtocolException extends Exception {
        public ProtocolException(String message) { super(message); }
    }

    private final Map<String, Object> fields;

    public Request(Map<String, Object> fields) {
        this.fields = fields;
    }

    public String action() {
        Object a = fields.get(Protocol.ACTION);
        return a instanceof String s ? s : null;
    }

    public Map<String, Object> raw() {
        return Collections.unmodifiableMap(fields);
    }

    public boolean has(String key) {
        return fields.get(key) != null;
    }

    public String getString(String key) throws ProtocolException {
        Object v = fields.get(key);
        if (v == null) throw missing(key);
        if (!(v instanceof String s)) throw new ProtocolException("Field '" + key + "' must be a string");
        if (s.isBlank()) throw new ProtocolException("Field '" + key + "' must not be empty");
        return s;
    }

    public int getInt(String key) throws ProtocolException {
        Object v = fields.get(key);
        if (v == null) throw missing(key);
        if (v instanceof BigDecimal bd) {
            try {
                return bd.intValueExact();
            } catch (ArithmeticException e) {
                throw new ProtocolException("Field '" + key + "' must be a whole number");
            }
        }
        throw new ProtocolException("Field '" + key + "' must be an integer");
    }

    /** Money. PROTOCOL.md sends amounts as numbers; a quoted number is accepted too, since clients differ. */
    public BigDecimal getAmount(String key) throws ProtocolException {
        Object v = fields.get(key);
        if (v == null) throw missing(key);
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                throw new ProtocolException("Field '" + key + "' must be a number");
            }
        }
        throw new ProtocolException("Field '" + key + "' must be a number");
    }

    private ProtocolException missing(String key) {
        return new ProtocolException("Missing required field '" + key + "'");
    }

    @Override
    public String toString() {
        // Never log a password, not even at debug level (PROTOCOL.md: passwords
        // are never sent back in any response, ever -- same spirit for logs).
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append('=')
              .append(e.getKey().equals("password") ? "***" : e.getValue());
        }
        return sb.append('}').toString();
    }
}
