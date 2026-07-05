package io.github.lxien.orbien.client.statemachine.agent.command;

import io.github.lxien.orbien.core.enums.TransportProtocol;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建连接命令
 * 封装创建隧道连接所需的参数
 */
@Getter
@Setter
public class ConnCreateCommand {

    /**
     * 传输协议（必填）
     */
    private TransportProtocol protocol;

    /**
     * 是否为多路复用连接（必填）
     */
    private Boolean multiplex;

    /**
     * 是否为加密连接（必填）
     */
    private Boolean encrypt;

    /**
     * 独立连接数量（可选，默认为1）
     * 仅当 multiplex=false 时有效
     */
    private Integer directCount = 1;

    /**
     * 创建多路复用连接命令
     *
     * @param encrypt 是否加密
     * @return 创建连接命令
     */
    public static ConnCreateCommand ofMultiplex(TransportProtocol protocol, boolean encrypt) {
        ConnCreateCommand command = new ConnCreateCommand();
        command.setProtocol(protocol);
        command.setMultiplex(true);
        command.setEncrypt(encrypt);
        return command;
    }

    /**
     * 创建独立连接命令
     *
     * @param encrypt 是否加密
     * @return 创建连接命令
     */
    public static ConnCreateCommand ofDirect(TransportProtocol protocol, boolean encrypt) {
        ConnCreateCommand command = new ConnCreateCommand();
        command.setProtocol(protocol);
        command.setMultiplex(false);
        command.setEncrypt(encrypt);
        return command;
    }

    public static ConnCreateCommand ofDirect(TransportProtocol protocol, boolean encrypt, int directCount) {
        ConnCreateCommand command = ofDirect(protocol, encrypt);
        command.setDirectCount(directCount);
        return command;
    }

    /**
     * 校验参数合法性
     *
     * @return 校验结果，null 表示校验通过，否则返回错误信息
     */
    public String validate() {
        if (multiplex == null) {
            return "multiplex 参数不能为空";
        }
        if (encrypt == null) {
            return "encrypt 参数不能为空";
        }
        if (protocol == null) {
            return "protocol 参数不能为空";
        }
        if (!multiplex && directCount != null && directCount <= 0) {
            return "directCount 必须大于 0";
        }
        return null;
    }

    /**
     * 获取有效的独立连接数量
     *
     * @return 独立连接数量，最小为1
     */
    public int getEffectiveDirectCount() {
        return (directCount != null && directCount > 0) ? directCount : 1;
    }

    /**
     * 是否为多路复用连接
     *
     * @return true 表示多路复用连接
     */
    public boolean isMultiplex() {
        return Boolean.TRUE.equals(multiplex);
    }

    /**
     * 是否为加密连接
     *
     * @return true 表示加密连接
     */
    public boolean isEncrypted() {
        return Boolean.TRUE.equals(encrypt);
    }
}