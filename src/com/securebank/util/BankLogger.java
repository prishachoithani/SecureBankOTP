package com.securebank.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal thread-safe logger. Writes to both console and a log file
 * (data/bank_activity.log) using Java I/O streams, demonstrating
 * character-stream file writing from the syllabus.
 *
 * Synchronized so multiple threads (OTP expiry, fraud engine, main
 * transaction flow) can log concurrently without interleaved/garbled lines.
 */
public class BankLogger {

    private static final String LOG_FILE = "data/bank_activity.log";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static synchronized void info(String message) {
        write("INFO", message);
    }

    public static synchronized void warn(String message) {
        write("WARN", message);
    }

    public static synchronized void alert(String message) {
        write("ALERT", message);
    }

    private static void write(String level, String message) {
        String line = String.format("[%s] [%s] [%s] %s",
                LocalDateTime.now().format(FORMAT),
                level,
                Thread.currentThread().getName(),
                message);

        System.out.println(line);

        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
