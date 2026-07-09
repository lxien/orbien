package io.github.lxien.orbien.core.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class FileShareLimitsConfig implements Serializable {
    private String rootPath;
    private Long maxUploadSize;
    private boolean allowUpload = true;
    private boolean allowDelete = true;
    private boolean allowMkdir = true;

    public static final long DEFAULT_MAX_UPLOAD_SIZE = 524_288_000L;
}
