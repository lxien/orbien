package io.github.lxien.orbien.client.common;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UUIDGenerator {

    private UUIDGenerator() {
    }

    /**
     * 生成32位的UUID
     * @return 32位的UUID字符串
     */
    public static String generate() {
        long mostSigBits = ThreadLocalRandom.current().nextLong();
        long leastSigBits = ThreadLocalRandom.current().nextLong();
        return new UUID(mostSigBits, leastSigBits).toString().replace("-", "");
    }
}