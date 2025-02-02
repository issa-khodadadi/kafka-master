package com.issa.kafka.master.utility;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Validation {
    private static final String IPV4_PATTERN =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                    + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                    + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                    + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final String PORT_PATTERN = "^(6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|[1-5][0-9]{4}|[1-9][0-9]{0,4})$";

    private static final Pattern ipPattern = Pattern.compile(IPV4_PATTERN);
    private static final Pattern portPattern = Pattern.compile(PORT_PATTERN);



    // VALIDATION METHODS
    public static boolean isMoreThanSelectedLength(String key, long length) {
        if (key == null) {
            return true;
        } else {
            return (long)key.length() >= length;
        }
    }

    public static boolean isNullOrBlank(String key) {
        return key == null ? true : key.isBlank();
    }

    public static boolean isNullOrZero(Long key) {
        if (key == null) {
            return true;
        } else {
            return key == 0L;
        }
    }

    public static boolean isNullOrZero(Integer key) {
        if (key == null) {
            return true;
        } else {
            return key == 0;
        }
    }

    public static boolean isServerNameMaxLengthValid(String serverName) {
        if (serverName == null) {
            return true;
        }
        return serverName.length() <= 50;
    }

    public static boolean isValidIP(String ip) {
        if (ip.equalsIgnoreCase("localhost")) {
            return true;
        }

        return ipPattern.matcher(ip).matches();
    }

    public static boolean isValidPort(String port) {
        return portPattern.matcher(port).matches();
    }

    // date time validation
    public static boolean isDateTimeValid(String dateTimeString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime.parse(dateTimeString, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // validate config key
    public static boolean isValidConfigKey(String configKey) {
        List<String> validKeys = Arrays.asList(
                // Log cleanup and compaction settings
                "cleanup.policy", "delete.retention.ms", "max.compaction.lag.ms",
                "min.compaction.lag.ms", "segment.ms", "segment.bytes", "segment.jitter.ms",
                "retention.ms", "retention.bytes", "file.delete.delay.ms",

                // Compression settings
                "compression.type",

                // Message handling and size
                "max.message.bytes", "message.timestamp.type", "message.timestamp.difference.max.ms",
                "message.format.version", "message.downconversion.enable",

                // Flush settings
                "flush.messages", "flush.ms",

                // Replica settings
                "min.insync.replicas", "unclean.leader.election.enable",
                "follower.replication.throttled.replicas", "leader.replication.throttled.replicas",

                // Quotas and throttling
                "quota.consumer.default", "quota.producer.default",

                // Log index and cache settings
                "log.index.interval.bytes", "log.index.size.max.bytes", "log.segment.delete.delay.ms",
                "log.roll.ms", "log.roll.jitter.ms", "segment.index.bytes", // Added segment.index.bytes

                // General configuration
                "preallocate", "delete.retention.bytes",

                // Cleanup and other policies
                "log.cleaner.enable", "log.cleaner.min.compaction.lag.ms", "log.cleaner.threads",
                "log.retention.check.interval.ms",

                // Tiered storage settings
                "remote.log.storage.enable", "remote.log.segment.bytes", "remote.log.retention.ms",

                // Additional settings
                "index.interval.bytes", "delete.topic.enable", "auto.create.topics.enable"
        );

        return validKeys.contains(configKey);
    }


    // value validation
    public static boolean isValidConfigValue(String configKey, String configValue) {
        try {
            return switch (configKey) {
                case "cleanup.policy" ->
                        configValue.equals("delete") || configValue.equals("compact");

                case "compression.type" ->
                        Arrays.asList("gzip", "snappy", "lz4", "zstd", "uncompressed", "producer").contains(configValue);

                case "delete.retention.ms", "retention.ms", "file.delete.delay.ms", "log.retention.check.interval.ms",
                     "log.segment.delete.delay.ms", "segment.jitter.ms" ->
                        Long.parseLong(configValue) >= 0;

                case "flush.messages", "min.insync.replicas", "max.message.bytes", "segment.bytes",
                     "log.index.interval.bytes", "log.index.size.max.bytes", "remote.log.segment.bytes",
                     "segment.index.bytes" -> // Added validation for segment.index.bytes
                        Integer.parseInt(configValue) > 0;

                case "segment.ms" ->
                        Long.parseLong(configValue) >= 0;

                case "message.timestamp.type" ->
                        configValue.equals("CreateTime") || configValue.equals("LogAppendTime");

                case "log.cleaner.enable", "unclean.leader.election.enable", "preallocate",
                     "remote.log.storage.enable", "auto.create.topics.enable", "delete.topic.enable",
                     "message.downconversion.enable" ->
                        configValue.equals("true") || configValue.equals("false");

                case "message.format.version" ->
                        configValue.matches("\\d+\\.\\d+"); // Kafka version format like "2.8", "3.0"

                case "quota.producer.default", "quota.consumer.default" ->
                        Double.parseDouble(configValue) >= 0;

                case "leader.replication.throttled.replicas", "follower.replication.throttled.replicas" ->
                        configValue.matches("^\\d+(,\\d+)*$"); // Comma-separated replica IDs

                default -> true; // Default case for unvalidated keys
            };
        } catch (NumberFormatException e) {
            return false; // Return false if number parsing fails
        }
    }
}
