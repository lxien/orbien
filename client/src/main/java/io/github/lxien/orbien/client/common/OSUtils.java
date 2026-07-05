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

package io.github.lxien.orbien.client.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 获取当前操作系统相关的系统信息
 *
 * @author lxien
 */
public class OSUtils {

    /**
     * 获取当前操作系统名称
     * @return
     */
    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "Windows";
        } else if (os.contains("mac")) {
            return "macOS";
        } else if (os.contains("linux")) {
            return "Linux";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "Unix";
        } else {
            return "Other";
        }
    }
    /**
     * 获取系统架构
     */
    public static String getOSArch() {
        return System.getProperty("os.arch");
    }
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
