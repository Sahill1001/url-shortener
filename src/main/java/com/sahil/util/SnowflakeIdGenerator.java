package com.sahil.util;

import lombok.Getter;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Twitter Snowflake-style distributed ID generator
 * 
 * ID Structure (64 bits):
 * - 1 sign bit (unused)
 * - 41 bits for timestamp (milliseconds since epoch) - supports ~69 years
 * - 5 bits for data center ID (0-31)
 * - 5 bits for machine ID (0-31)
 * - 12 bits for sequence (0-4095)
 * 
 * This ensures:
 * - Globally unique IDs across distributed systems
 * - Time-ordered for indexing efficiency
 * - No central coordination needed
 */
@Component
@Getter
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00 UTC
    private static final long TIMESTAMP_BITS = 41;
    private static final long DATACENTER_BITS = 5;
    private static final long MACHINE_BITS = 5;
    private static final long SEQUENCE_BITS = 12;

    private static final long DATACENTER_MASK = (1L << DATACENTER_BITS) - 1;
    private static final long MACHINE_MASK = (1L << MACHINE_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private static final long TIMESTAMP_SHIFT = DATACENTER_BITS + MACHINE_BITS + SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = MACHINE_BITS + SEQUENCE_BITS;
    private static final long MACHINE_SHIFT = SEQUENCE_BITS;

    private final long datacenterId;
    private final long machineId;
    private long lastTimestamp;
    private long sequence;

    public SnowflakeIdGenerator() {
        this.datacenterId = getDatacenterId();
        this.machineId = getMachineId();
        this.lastTimestamp = System.currentTimeMillis();
        this.sequence = 0;
    }

    /**
     * Generates a unique distributed ID
     * @return A 64-bit unique ID
     */
    public synchronized long generateId() {
        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards!");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // Sequence overflow, wait for next millisecond
                currentTimestamp = waitNextMillis();
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT) |
               ((datacenterId & DATACENTER_MASK) << DATACENTER_SHIFT) |
               ((machineId & MACHINE_MASK) << MACHINE_SHIFT) |
               (sequence & SEQUENCE_MASK);
    }

    private long waitNextMillis() {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * Gets data center ID from environment or defaults to 0
     */
    private long getDatacenterId() {
        String datacenterId = System.getenv("DATACENTER_ID");
        if (datacenterId != null) {
            return Long.parseLong(datacenterId) & DATACENTER_MASK;
        }
        return 0;
    }

    /**
     * Gets machine ID from hostname or defaults to 0
     */
    private long getMachineId() {
        String machineId = System.getenv("MACHINE_ID");
        if (machineId != null) {
            return Long.parseLong(machineId) & MACHINE_MASK;
        }
        try {
            byte[] addr = InetAddress.getLocalHost().getAddress();
            return (addr[2] << 8 | addr[3]) & MACHINE_MASK;
        } catch (UnknownHostException e) {
            return 0;
        }
    }
}
