package io.github.lxien.orbien.server.web.dto.binding;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CertBindResultDTO implements Serializable {
    private int successCount;
    private int failedCount;
    private List<CertBindItemResultDTO> results = new ArrayList<>();
}
