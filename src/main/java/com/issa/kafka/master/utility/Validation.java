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
    public static boolean isMoreThanMinLength(String key, long length) {
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
                "cleanup.policy",           // Determines log compaction or deletion
                "delete.retention.ms",      // Time to retain deleted records
                "max.compaction.lag.ms",    // Maximum delay for compaction
                "min.compaction.lag.ms",    // Minimum delay for compaction
                "segment.ms",               // Time before creating a new log segment
                "segment.bytes",            // Size of each log segment in bytes
                "segment.jitter.ms",        // Jitter for log segment rolling
                "retention.ms",             // Time to retain log records
                "retention.bytes",          // Maximum size of the log before deletion
                "file.delete.delay.ms",     // Delay before deleting a log file

                // Compression settings
                "compression.type",         // Compression type for topic data

                // Message handling and size
                "max.message.bytes",        // Maximum size of a single message
                "message.timestamp.type",   // Type of timestamp used in messages
                "message.timestamp.difference.max.ms", // Maximum allowable difference for timestamps
                "message.format.version",   // Message format version
                "message.downconversion.enable", // Enable or disable message down-conversion

                // Flush settings
                "flush.messages",           // Number of messages to flush to disk
                "flush.ms",                 // Maximum time before flushing data

                // Replica settings
                "min.insync.replicas",      // Minimum in-sync replicas for writes
                "unclean.leader.election.enable", // Allow unclean leader election
                "follower.replication.throttled.replicas", // Throttle replication for followers
                "leader.replication.throttled.replicas",   // Throttle replication for leaders

                // Quotas and throttling
                "quota.consumer.default",   // Default consumer quota
                "quota.producer.default",   // Default producer quota

                // Log index and cache settings
                "log.index.interval.bytes", // Interval for indexing log entries
                "log.index.size.max.bytes", // Maximum size of the index
                "log.segment.delete.delay.ms", // Delay before deleting a log segment
                "log.roll.ms",              // Time before rolling the log
                "log.roll.jitter.ms",       // Jitter for log rolling

                // General configuration
                "preallocate",              // Preallocate disk space for log segments
                "delete.retention.bytes",   // Retention size for deleted records

                // Cleanup and other policies
                "log.cleaner.enable",       // Enable the log cleaner
                "log.cleaner.min.compaction.lag.ms", // Minimum compaction lag
                "log.cleaner.threads",      // Number of log cleaner threads
                "log.retention.check.interval.ms", // Interval to check retention

                // Tiered storage settings (if supported in your Kafka version)
                "remote.log.storage.enable",   // Enable remote log storage
                "remote.log.segment.bytes",   // Size of remote log segments
                "remote.log.retention.ms",    // Retention time for remote logs

                // Add more keys if required based on specific use cases
                "index.interval.bytes",      // Interval for indexing records
                "delete.topic.enable",       // Allow topics to be deleted
                "auto.create.topics.enable"  // Allow automatic topic creation
        );
        return validKeys.contains(configKey);
    }

    // value validation
    public static boolean isValidConfigValue(String configKey, String configValue) {
        // Validate the configuration value based on the key
        try {
            return switch (configKey) {
                case "cleanup.policy" ->
                    // Valid values: "delete" or "compact"
                        configValue.equals("delete") || configValue.equals("compact");
                case "compression.type" ->
                    // Valid values: Compression algorithms supported by Kafka
                        Arrays.asList("gzip", "snappy", "lz4", "zstd", "uncompressed").contains(configValue);
                case "delete.retention.ms", "retention.ms", "file.delete.delay.ms", "log.retention.check.interval.ms",
                     "log.segment.delete.delay.ms", "segment.jitter.ms" ->
                    // Value should be a non-negative long
                        Long.parseLong(configValue) >= 0;
                case "flush.messages" ->
                    // Value should be a positive long
                        Long.parseLong(configValue) > 0;
                case "min.insync.replicas", "max.message.bytes", "segment.bytes", "log.index.interval.bytes",
                     "log.index.size.max.bytes", "remote.log.segment.bytes" ->
                    // Value should be a positive integer
                        Integer.parseInt(configValue) > 0;
                case "segment.ms" ->
                    // Value should be a non-negative long (time in milliseconds)
                        Long.parseLong(configValue) >= 0;
                case "message.timestamp.type" ->
                    // Valid values: "CreateTime" or "LogAppendTime"
                        configValue.equals("CreateTime") || configValue.equals("LogAppendTime");
                case "log.cleaner.enable", "unclean.leader.election.enable", "preallocate", "remote.log.storage.enable",
                     "auto.create.topics.enable", "delete.topic.enable", "message.downconversion.enable" ->
                    // Valid values: "true" or "false"
                        configValue.equals("true") || configValue.equals("false");
                case "message.format.version" ->
                    // Value should match valid Kafka versions (e.g., "2.8", "3.0")
                        configValue.matches("\\d+\\.\\d+"); // Regex for version format like "2.8"

                case "quota.producer.default", "quota.consumer.default" ->
                    // Value should be a non-negative double
                        Double.parseDouble(configValue) >= 0;
                case "leader.replication.throttled.replicas", "follower.replication.throttled.replicas" ->
                    // Value should be a comma-separated list of replica IDs (e.g., "1,2,3")
                        configValue.matches("^\\d+(,\\d+)*$"); // Regex for IDs like "1,2,3"

                default ->
                    // Default case for keys with no specific validation logic
                        true;
            };
        } catch (NumberFormatException e) {
            // If parsing fails, the value is invalid
            return false;
        }
    }
}
