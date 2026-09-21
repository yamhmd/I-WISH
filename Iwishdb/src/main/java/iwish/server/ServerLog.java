package iwish.server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiny logger. Person 2's task 12 is "run the iwish.server and watch logs for
 * thread/connection issues during multi-client testing", so every line
 * carries the thread name -- that is how a leaked or stuck client thread
 * shows itself.
 */
public final class ServerLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private ServerLog() { }

    public static void info(String message)  { print("INFO ", message); }
    public static void warn(String message)  { print("WARN ", message); }
    public static void error(String message) { print("ERROR", message); }

    private static void print(String level, String message) {
        System.out.println(LocalTime.now().format(TIME) + " [" + level + "] ["
            + Thread.currentThread().getName() + "] " + message);
    }
}
