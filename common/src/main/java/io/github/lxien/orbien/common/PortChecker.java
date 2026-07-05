package io.github.lxien.orbien.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * 端口可用性检查
 * @author lxien
 */
public class PortChecker {
    /**
     * 检查指定端口是否被占用
     *
     * @param port 端口号
     * @return true 如果端口被占用
     */
    public static boolean isPortOccupied(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * 获取占用指定端口的进程ID (适用于Windows和Unix-like系统)
     *
     * @param port 端口号
     * @return 进程ID列表
     */
    public static List<String> getProcessIdByPort(int port) {
        List<String> pids = new ArrayList<>();
        try {
            String command;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                command = "netstat -aon | findstr :" + port;
            } else {
                command = "lsof -iTCP:" + port + " -sTCP:LISTEN -t";
            }

            Process process = Runtime.getRuntime().exec(command);
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            if (!output.isEmpty()) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 5 && parts[1].endsWith(":" + port)) {
                            pids.add(parts[4]);
                        }
                    } else {
                        pids.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pids;
    }

    /**
     * 强制终止占用指定端口的进程
     *
     * @param port 端口号
     * @return true 如果成功终止
     */
    public static boolean killProcessByPort(int port) {
        List<String> pids = getProcessIdByPort(port);
        if (pids.isEmpty()) {
            return false;
        }

        boolean success = true;
        try {
            String commandPrefix = System.getProperty("os.name").toLowerCase().contains("win") ? "taskkill /F /PID " : "kill -9 ";
            for (String pid : pids) {
                Runtime.getRuntime().exec(commandPrefix + pid);
            }
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static void killPort(int port) {
        if (isPortOccupied(port)) {
            killProcessByPort(port);
        }
    }
}
