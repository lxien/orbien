package io.github.lxien.orbien.server.web.dto.transport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TransportEncryptConstraints implements Serializable {
    private Boolean encryptEditable;
    private Boolean encryptLocked;
    private String encryptLockedReason;
    private Boolean globalTlsEnabled;
    private List<Boolean> allowedEncryptValues;
}
