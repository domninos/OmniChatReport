package net.omni.chatreport.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    private static final Pattern PATTERN = Pattern.compile("(\\d+)([a-z]+)");

    public static String getTimeRemainingString(long timeMillis) {
        long remaining = timeMillis - System.currentTimeMillis();

        if (remaining <= 0)
            return "Expired";

        long seconds = remaining / 1000;

        long years = seconds / (365L * 24 * 3600);
        seconds %= (365L * 24 * 3600);

        long months = seconds / (30L * 24 * 3600);
        seconds %= (30L * 24 * 3600);

        long weeks = seconds / (7 * 24 * 3600);
        seconds %= (7 * 24 * 3600);

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder result = new StringBuilder();

        appendTime(result, years, "year");
        appendTime(result, months, "month");
        appendTime(result, weeks, "week");
        appendTime(result, days, "day");
        appendTime(result, hours, "hour");
        appendTime(result, minutes, "minute");
        appendTime(result, seconds, "second");

        if (result.isEmpty())
            result.append("-");

        return result.toString().trim();
    }

    private static void appendTime(StringBuilder sb, long value, String unit) {
        if (value > 0) {
            sb.append(value)
                    .append(" ")
                    .append(unit)
                    .append(value == 1 ? " " : "s ");
        }
    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty())
            throw new IllegalArgumentException("Time cannot be empty");

        input = input.toLowerCase();

        if (input.matches("\\d+")) {
            long timestamp = Long.parseLong(input);
            if (timestamp > System.currentTimeMillis())
                return timestamp;
            else
                throw new IllegalArgumentException("Timestamp is in the past: " + input);
        }

        long totalMillis = 0;

        Matcher matcher = PATTERN.matcher(input);

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s":
                    totalMillis += value * 1_000L;
                    break;
                case "m":
                    totalMillis += value * 60_000L;
                    break;
                case "h":
                    totalMillis += value * 3_600_000L;
                    break;
                case "d":
                    totalMillis += value * 86_400_000L;
                    break;
                case "w":
                    totalMillis += value * 604_800_000L;
                    break;
                case "mo":
                    totalMillis += value * 2_592_000_000L; // 30 days
                    break;
                case "y":
                    totalMillis += value * 31_536_000_000L; // 365 days
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown time unit: " + unit +
                                    " | Supported: s, m, h, d, w, mo, y"
                    );
            }
        }

        if (totalMillis != 0)
            totalMillis += System.currentTimeMillis();
        else
            throw new IllegalArgumentException("Could not parse time duration from: " + input);

        return totalMillis;
    }

}
