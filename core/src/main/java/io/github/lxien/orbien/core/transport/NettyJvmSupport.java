package io.github.lxien.orbien.core.transport;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Java 22+ 运行 Netty 原生传输时需要 {@code --enable-native-access=ALL-UNNAMED}。
 * 该参数无法在运行时注入，此处仅检测并给出一次性提示。
 * @author lxien
 */
public final class NettyJvmSupport {

    private static final String NATIVE_ACCESS_ARG = "--enable-native-access=ALL-UNNAMED";
    private static volatile boolean checked;

    private NettyJvmSupport() {
    }

    public static void ensureNativeAccess() {
        if (checked) {
            return;
        }
        checked = true;
        if (Runtime.version().feature() < 22) {
            return;
        }
        if (isNativeAccessEnabled()) {
            return;
        }
        System.err.println("[orbien] Java " + Runtime.version().feature()
                + " 运行 Netty 建议添加 JVM 参数: " + NATIVE_ACCESS_ARG);
    }

    private static boolean isNativeAccessEnabled() {
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : inputArguments) {
            if (arg.startsWith("--enable-native-access")) {
                return true;
            }
        }
        return false;
    }
}
