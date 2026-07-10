package io.github.lxien.orbien.server.web.service.support;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.dto.transport.TransportCompressConstraints;

import java.util.List;

public final class TransportCompressConstraintSupport {

    private static final List<String> ALLOWED_ALGORITHMS = List.of("snappy", "lz4", "zstd");
    private static final String DEFAULT_ALGORITHM = "snappy";

    private TransportCompressConstraintSupport() {
    }

    public static TransportCompressConstraints build() {
        TransportCompressConstraints constraints = new TransportCompressConstraints();
        constraints.setCompressEditable(true);
        constraints.setAlgorithmEditable(true);
        constraints.setAllowedAlgorithms(ALLOWED_ALGORITHMS);
        return constraints;
    }

    public static void validate(Boolean compress, String compressAlgorithm) {
        if (compress == null) {
            throw new BizException("compress 不能为空");
        }
        if (!compress) {
            return;
        }
        String normalized = normalizeAlgorithm(compressAlgorithm);
        if (!ALLOWED_ALGORITHMS.contains(normalized)) {
            throw new BizException("不支持的压缩算法，仅支持 snappy / lz4 / zstd");
        }
    }

    public static String resolveStoredAlgorithm(Boolean compress, String compressAlgorithm) {
        if (!Boolean.TRUE.equals(compress)) {
            return DEFAULT_ALGORITHM;
        }
        if (compressAlgorithm == null || compressAlgorithm.isBlank()) {
            return DEFAULT_ALGORITHM;
        }
        return compressAlgorithm.trim().toLowerCase();
    }

    public static String resolveEffectiveAlgorithm(Boolean compress, String compressAlgorithm) {
        if (!Boolean.TRUE.equals(compress)) {
            return "none";
        }
        return resolveStoredAlgorithm(true, compressAlgorithm);
    }

    public static String normalizeAlgorithm(String compressAlgorithm) {
        if (compressAlgorithm == null || compressAlgorithm.isBlank()) {
            return DEFAULT_ALGORITHM;
        }
        return compressAlgorithm.trim().toLowerCase();
    }

    public static String toStorageValue(Boolean compress, String compressAlgorithm) {
        if (!Boolean.TRUE.equals(compress)) {
            return "none";
        }
        return normalizeAlgorithm(compressAlgorithm);
    }
}
