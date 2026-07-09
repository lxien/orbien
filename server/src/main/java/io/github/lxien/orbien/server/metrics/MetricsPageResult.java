package io.github.lxien.orbien.server.metrics;

import java.util.List;

/**
 * 服务端指标列表分页结果
 */
public class MetricsPageResult<T> {

    private final Integer page;
    private final Integer size;
    private final Long total;
    private final List<T> records;

    public MetricsPageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public Long getTotal() {
        return total;
    }

    public List<T> getRecords() {
        return records;
    }
}
