package io.github.lxien.orbien.server.web.enums;

import lombok.Getter;

@Getter
public enum MetricQueryType {

    /**
     * 最近24小时
     */
    LAST_24_HOURS("24h", 0, "最近24小时"),

    /**
     * 最近3天
     */
    LAST_3_DAYS("3d", 3, "最近3天"),

    /**
     * 最近7天
     */
    LAST_7_DAYS("7d", 7, "最近7天"),

    /**
     * 最近15天
     */
    LAST_30_DAYS("15d", 15, "最近15天"),

    /**
     * 自定义日期范围
     */
    CUSTOM("custom", -1, "自定义日期");

    /**
     * 前端下拉框传入的参数标识
     */
    private final String code;

    /**
     * 该查询类型对应的历史跨度天数
     */
    private final int days;

    /**
     * 界面展示的中文描述
     */
    private final String description;

    MetricQueryType(String code, int days, String description) {
        this.code = code;
        this.days = days;
        this.description = description;
    }

    public static MetricQueryType fromCode(String code) {
        for (MetricQueryType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效值: " + code);
    }
}
