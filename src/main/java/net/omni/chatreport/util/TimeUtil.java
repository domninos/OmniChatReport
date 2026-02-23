package net.omni.chatreport.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-HH-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    public static String format(long timeMillis) {
        return FORMATTER.format(Instant.ofEpochMilli(timeMillis));
    }

    public static String getTimeRemainingString(long timeMillis) {
        long remaining = timeMillis - System.currentTimeMillis();

        if (remaining <= 0)
            return "Expired";

        long seconds = remaining / 1000;

        long weeks = seconds / (7 * 24 * 3600);
        seconds %= (7 * 24 * 3600);

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder result = new StringBuilder();

        if (weeks > 0) result.append(weeks).append("w ");
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0) result.append(seconds).append("s ");

        return result.toString().trim();

    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty())
            throw new IllegalArgumentException("Time cannot be empty");

        input = input.toLowerCase();

        long totalMillis = 0;
        StringBuilder number = new StringBuilder();

        for (char c : input.toCharArray()) {

            if (Character.isDigit(c)) {
                number.append(c);
            } else {

                if (number.isEmpty())
                    throw new IllegalArgumentException("Invalid time format");

                long value = Long.parseLong(number.toString());
                number.setLength(0);

                switch (c) {
                    case 's':
                        totalMillis += value * 1000L;
                        break;
                    case 'm':
                        totalMillis += value * 60_000L;
                        break;
                    case 'h':
                        totalMillis += value * 3_600_000L;
                        break;
                    case 'd':
                        totalMillis += value * 86_400_000L;
                        break;
                    case 'w':
                        totalMillis += value + 604_800_000L;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown time unit: " + c
                                + " | Supported: s - seconds, m - minutes, h - hour, d - day, w - week");
                }
            }
        }

        if (!number.isEmpty())
            throw new IllegalArgumentException("Time format must end with a unit");

        return totalMillis;
    }

}
