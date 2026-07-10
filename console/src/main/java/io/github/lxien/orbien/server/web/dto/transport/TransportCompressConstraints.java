package io.github.lxien.orbien.server.web.dto.transport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TransportCompressConstraints implements Serializable {
    private boolean compressEditable = true;
    private boolean algorithmEditable = true;
    private List<String> allowedAlgorithms = List.of("snappy", "lz4","zstd");
}
