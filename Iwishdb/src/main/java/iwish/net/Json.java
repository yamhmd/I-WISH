package iwish.net;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, strict JSON reader/writer -- enough for PROTOCOL.md and nothing more.
 *
 * Written by hand on purpose: the protocol is one flat object per line, and
 * this keeps the build to "javac + the MySQL driver" with no extra jar for
 * anyone to forget to add. If the team later puts Gson on the classpath,
 * only this class has to change.
 *
 * Parsed types: Map&lt;String,Object&gt;, List&lt;Object&gt;, String, BigDecimal,
 * Boolean, null. Numbers come back as BigDecimal so money keeps its exact
 * decimal value (0.1 + 0.2 problems stay out of the protocol layer).
 */
public final class Json {

    /** Thrown for any input that is not valid JSON. Callers turn this into the standard ERROR response. */
    public static class JsonException extends Exception {
        public JsonException(String message) { super(message); }
    }

    private Json() { }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeValue(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                writeValue(o, sb);
            }
            sb.append(']');
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof BigDecimal bd) {
            // PROTOCOL.md: "Money amounts are numbers (floats), two decimal
            // places of precision expected."
            sb.append(bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        } else if (v instanceof Number) {
            sb.append(v.toString());
        } else {
            writeString(v.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    /** Parses a full JSON object. Anything else (array, bare value, trailing junk) is an error. */
    public static Map<String, Object> parseObject(String text) throws JsonException {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new JsonException("Top-level JSON value must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    public static Object parse(String text) throws JsonException {
        if (text == null) {
            throw new JsonException("Empty input");
        }
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new JsonException("Unexpected trailing characters at position " + p.pos);
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return pos >= s.length(); }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object readValue() throws JsonException {
            skipWhitespace();
            if (atEnd()) {
                throw new JsonException("Unexpected end of input");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't', 'f' -> readBoolean();
                case 'n' -> readNull();
                default -> readNumber();
            };
        }

        Map<String, Object> readObject() throws JsonException {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; return map; }
                throw new JsonException("Expected ',' or '}' at position " + pos);
            }
        }

        List<Object> readArray() throws JsonException {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; return list; }
                throw new JsonException("Expected ',' or ']' at position " + pos);
            }
        }

        String readString() throws JsonException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw new JsonException("Unterminated string");
                char c = s.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (atEnd()) throw new JsonException("Unterminated escape sequence");
                char esc = s.charAt(pos++);
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'u'  -> {
                        if (pos + 4 > s.length()) throw new JsonException("Truncated \\u escape");
                        String hex = s.substring(pos, pos + 4);
                        pos += 4;
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw new JsonException("Invalid \\u escape: " + hex);
                        }
                    }
                    default -> throw new JsonException("Invalid escape character: \\" + esc);
                }
            }
        }

        Boolean readBoolean() throws JsonException {
            if (s.startsWith("true", pos))  { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw new JsonException("Invalid literal at position " + pos);
        }

        Object readNull() throws JsonException {
            if (s.startsWith("null", pos)) { pos += 4; return null; }
            throw new JsonException("Invalid literal at position " + pos);
        }

        BigDecimal readNumber() throws JsonException {
            int start = pos;
            if (peek() == '-' || peek() == '+') pos++;
            while (!atEnd()) {
                char c = s.charAt(pos);
                if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    pos++;
                } else {
                    break;
                }
            }
            String raw = s.substring(start, pos);
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException e) {
                throw new JsonException("Invalid number: " + raw);
            }
        }

        char peek() throws JsonException {
            if (atEnd()) throw new JsonException("Unexpected end of input");
            return s.charAt(pos);
        }

        void expect(char expected) throws JsonException {
            if (peek() != expected) {
                throw new JsonException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }
    }
}
