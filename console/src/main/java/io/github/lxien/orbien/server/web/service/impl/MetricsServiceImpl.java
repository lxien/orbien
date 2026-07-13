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

package io.github.lxien.orbien.server.web.service.impl;

import io.github.lxien.orbien.server.metrics.HourlyTraffic;
import io.github.lxien.orbien.server.metrics.Metrics;
import io.github.lxien.orbien.server.metrics.MetricsCollector;
import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.metrics.DailyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.HourlyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.ProxyTrafficQueryResult;
import io.github.lxien.orbien.server.web.dto.metrics.Traffic24LineDTO;
import io.github.lxien.orbien.server.web.dto.metrics.TrafficChartVO;
import io.github.lxien.orbien.server.web.dto.metrics.TrafficCountDTO;
import io.github.lxien.orbien.server.web.dto.proxy.ProxyListQueryResult;
import io.github.lxien.orbien.server.web.enums.MetricQueryType;
import io.github.lxien.orbien.server.web.enums.TimeUnit;
import io.github.lxien.orbien.server.web.param.metrics.ProxyQueryParam;
import io.github.lxien.orbien.server.web.repository.MetricsRepository;
import io.github.lxien.orbien.server.web.repository.ProxyRepository;
import io.github.lxien.orbien.server.web.service.MetricsService;
import io.github.lxien.orbien.server.web.service.converter.MetricConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MetricsServiceImpl implements MetricsService {
    @Autowired
    private MetricsCollector metricsCollector;
    @Autowired
    private MetricsRepository metricsRepository;
    @Autowired
    private ProxyRepository proxyRepository;
    @Autowired
    private MetricConvert metricConvert;

    @Override
    public TrafficChartVO getTotal24hTraffic() {
        TrafficChartVO trafficChartVO = buildTrafficChartVO(metricsCollector.getTotal24hTraffic(), metricsCollector.getTotalWriteBytes(),
                metricsCollector.getTotalReadBytes(),
                metricsCollector.getTotalWriteBytesRate(),
                metricsCollector.getTotalReadBytesRate()
        );
        trafficChartVO.setActiveChannels(metricsCollector.getTotalActiveChannels());
        return trafficChartVO;
    }

    @Override
    public TrafficChartVO getProxy24hTraffic(ProxyQueryParam param) {
        String proxyId = param.getProxyId();
        MetricQueryType queryType = MetricQueryType.fromCode(param.getQueryType());
        TrafficChartVO result;
        switch (queryType) {
            case LAST_24_HOURS: {
                List<HourlyTraffic> list = metricsCollector.get24hTraffic(proxyId);
                Metrics metrics = metricsCollector.getProxyMetrics(proxyId);
                if (metrics == null) {
                    result = buildTrafficChartVO(list, 0L, 0L, 0.0, 0.0);
                } else {
                    result = buildTrafficChartVO(list, metrics.getWriteBytes(), metrics.getReadBytes(),
                            metrics.getWriteRate(), metrics.getReadRate());
                }
                break;
            }
            case LAST_3_DAYS:
            case LAST_7_DAYS:
            case LAST_30_DAYS: {
                int days = queryType.getDays();
                LocalDate startDate = LocalDate.now().minusDays(days - 1);
                LocalDateTime startTime = startDate.atStartOfDay();
                LocalDateTime endTime = LocalDate.now().atTime(23, 59, 59);
                List<DailyTrafficQueryResult> results =
                        metricsRepository.queryDailyTrafficByRange(proxyId, startTime, endTime);
                result = buildDailyTrafficChartVO(results, startDate, LocalDate.now());
                break;
            }
            case CUSTOM: {
                LocalDate startDate = param.getStartDate();
                LocalDate endDate = param.getEndDate();
                if (startDate == null || endDate == null) {
                    throw new BizException("起始日期不能为空");
                }
                long spanDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                if (spanDays <= 0 || spanDays > 15) {
                    throw new BizException("自定义查询天数跨度必须在 1 ~ 15 天之间");
                }
                if (spanDays == 1) {
                    result = buildHistoricalDayHourlyChartVO(metricsRepository.queryHourlyTrafficByRange(
                            proxyId, startDate.atStartOfDay(), startDate.plusDays(1).atStartOfDay()));
                } else {
                    List<DailyTrafficQueryResult> results =
                            metricsRepository.queryDailyTrafficByRange(proxyId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
                    result = buildDailyTrafficChartVO(results, startDate, endDate);
                }
                break;
            }
            default:
                return null;
        }
        result.setActiveChannels(metricsCollector.getActiveChannels(proxyId));
        return result;
    }

    /**
     * 历史单日图表，24个刻度（01-24）
     */
    private TrafficChartVO buildHistoricalDayHourlyChartVO(List<HourlyTrafficQueryResult> sparse) {
        Map<Integer, HourlyTrafficQueryResult> byHour = new HashMap<>(24);
        if (sparse != null) {
            for (HourlyTrafficQueryResult ht : sparse) {
                if (ht.getStatHour() != null) {
                    byHour.put(ht.getStatHour().getHour(), ht);
                }
            }
        }

        Traffic24LineDTO upDto = new Traffic24LineDTO();
        Traffic24LineDTO downDto = new Traffic24LineDTO();
        List<String> xAxis = new ArrayList<>(24);
        List<Long> upYAxis = new ArrayList<>(24);
        List<Long> downYAxis = new ArrayList<>(24);
        long upTotal = 0L;
        long downTotal = 0L;

        for (int slot = 1; slot <= 24; slot++) {
            int hourIndex = slot - 1;
            HourlyTrafficQueryResult ht = byHour.get(hourIndex);

            xAxis.add(String.format("%02d", slot));
            long up = ht != null && ht.getWriteBytes() != null ? ht.getWriteBytes() : 0L;
            long down = ht != null && ht.getReadBytes() != null ? ht.getReadBytes() : 0L;
            upYAxis.add(up);
            downYAxis.add(down);
            upTotal += up;
            downTotal += down;
        }

        upDto.setXAxis(xAxis);
        upDto.setYAxis(upYAxis);
        downDto.setXAxis(xAxis);
        downDto.setYAxis(downYAxis);

        return TrafficChartVO.builder().up(upDto).down(downDto).upTotal(upTotal).downTotal(downTotal)
                .upRate(0.0)
                .downRate(0.0)
                .timeUnit(TimeUnit.HOUR.getCode())
                .build();
    }

    /**
     * 滚动最近 24 小时，X 轴为实际钟点 00~23
     */
    private TrafficChartVO buildTrafficChartVO(List<HourlyTraffic> list, long upTotal,
                                               long downTotal, double upRate, double downRate) {
        Traffic24LineDTO upDto = new Traffic24LineDTO();
        Traffic24LineDTO downDto = new Traffic24LineDTO();

        List<String> xAxis = new ArrayList<>(24);
        List<Long> upYAxis = new ArrayList<>(24);
        List<Long> downYAxis = new ArrayList<>(24);

        for (HourlyTraffic ht : list) {
            String hourStr = String.format("%02d", ht.getHour().getHour());
            long up = ht.getWriteBytes();
            long down = ht.getReadBytes();

            xAxis.add(hourStr);
            upYAxis.add(up);
            downYAxis.add(down);
        }

        upDto.setXAxis(xAxis);
        upDto.setYAxis(upYAxis);
        downDto.setXAxis(xAxis);
        downDto.setYAxis(downYAxis);

        return TrafficChartVO.builder().up(upDto).down(downDto).upTotal(upTotal).downTotal(downTotal)
                .downRate(downRate)
                .upRate(upRate)
                .timeUnit(TimeUnit.HOUR.getCode())
                .build();
    }

    /**
     * 构建每日流量数据的，按天统计（历史数据）
     */
    private TrafficChartVO buildDailyTrafficChartVO(List<DailyTrafficQueryResult> results,
                                                    LocalDate startDate, LocalDate endDate) {
        Traffic24LineDTO upDto = new Traffic24LineDTO();
        Traffic24LineDTO downDto = new Traffic24LineDTO();

        List<String> xAxis = new ArrayList<>();
        List<Long> upYAxis = new ArrayList<>();
        List<Long> downYAxis = new ArrayList<>();

        long upTotal = 0L;
        long downTotal = 0L;

        java.util.Map<LocalDate, DailyTrafficQueryResult> resultMap = new java.util.HashMap<>();
        if (results != null) {
            for (DailyTrafficQueryResult result : results) {
                resultMap.put(result.getDateStr(), result);
            }
        }

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = String.format("%02d-%02d", currentDate.getMonthValue(), currentDate.getDayOfMonth());

            DailyTrafficQueryResult result = resultMap.get(currentDate);
            long write = result != null && result.getTotalWrite() != null ? result.getTotalWrite() : 0L;
            long read = result != null && result.getTotalRead() != null ? result.getTotalRead() : 0L;

            xAxis.add(dateStr);
            upYAxis.add(write);
            downYAxis.add(read);

            upTotal += write;
            downTotal += read;

            currentDate = currentDate.plusDays(1);
        }

        upDto.setXAxis(xAxis);
        upDto.setYAxis(upYAxis);

        downDto.setXAxis(xAxis);
        downDto.setYAxis(downYAxis);

        return TrafficChartVO.builder().up(upDto).down(downDto).upTotal(upTotal).downTotal(downTotal)
                .upRate(0.0)
                .downRate(0.0)
                .timeUnit(TimeUnit.DAY.getCode())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProxyId(String proxyId) {
        metricsRepository.deleteByProxyId(proxyId);
        metricsCollector.removeByProxyId(proxyId);
    }

    @Override
    public PageResult<TrafficCountDTO> queryPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize());
        Page<ProxyTrafficQueryResult> page = metricsRepository.pageTrafficByProxy(pageable);
        if (page.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<TrafficCountDTO> records = metricConvert.toDTOList(page.getContent());
        enrichProxyInfo(records);
        return PageResult.wrap(page, records);
    }

    private void enrichProxyInfo(List<TrafficCountDTO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<String> proxyIds = records.stream().map(TrafficCountDTO::getProxyId).distinct().toList();
        if (proxyIds.isEmpty()) {
            return;
        }
        Map<String, ProxyListQueryResult> proxyMap = proxyRepository.findWithAgentByIdIn(proxyIds).stream()
                .collect(Collectors.toMap(r -> r.getProxyDO().getId(), Function.identity(),
                        (a, _) -> a));
        for (TrafficCountDTO dto : records) {
            metricConvert.enrichProxyInfo(dto, proxyMap.get(dto.getProxyId()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteOldMetrics(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        long count = metricsRepository.countByCreatedAtBefore(cutoffTime);
        metricsRepository.deleteByCreatedAtBefore(cutoffTime);
        return (int) count;
    }
}
