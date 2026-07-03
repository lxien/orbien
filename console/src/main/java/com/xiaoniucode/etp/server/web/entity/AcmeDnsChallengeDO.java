package com.xiaoniucode.etp.server.web.entity;

import com.xiaoniucode.etp.server.web.entity.converter.AcmeChallengeStatusConverter;
import com.xiaoniucode.etp.server.web.enums.AcmeChallengeStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "acme_dns_challenge")
public class AcmeDnsChallengeDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String domain;

    @Column(name = "record_name", nullable = false)
    private String recordName;

    @Column(name = "host_record")
    private String hostRecord;

    @Column(name = "dns_zone")
    private String dnsZone;

    /**
     * 厂商 API 清理记录时使用的区域标识（域名或 ZoneId）
     */
    @Column(name = "provider_zone")
    private String providerZone;

    @Column(name = "record_value", nullable = false, columnDefinition = "TEXT")
    private String recordValue;

    @Column(name = "record_type", nullable = false)
    private String recordType = "TXT";

    @Column(name = "provider_record_id")
    private String providerRecordId;

    @Column(name = "challenge_url", columnDefinition = "TEXT")
    private String challengeUrl;

    @Convert(converter = AcmeChallengeStatusConverter.class)
    @Column(nullable = false)
    private AcmeChallengeStatus status = AcmeChallengeStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
}
