/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serializable;

@Data
public class TransportProperties implements Serializable {
    @NestedConfigurationProperty
    private TlsProperties tls = new TlsProperties();
    @Data
    static class TlsProperties {
        private Boolean enabled = true;
        private String certFile;
        private String keyFile;
        private String caFile;
        private String keyPassword;
    }
}
