package io.github.lxien.orbien.server.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UUIDGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public String uuid32() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new UUID(random.nextLong(), random.nextLong()).toString().replace("-", "");
    }
    public String uuid16(boolean upperCase) {
        long random64 = SECURE_RANDOM.nextLong();
        String hex = String.format("%016x", random64);
        return upperCase ? hex.toUpperCase() : hex;
    }
}
