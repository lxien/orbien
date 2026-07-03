package com.xiaoniucode.etp.server.web.entity;

import com.xiaoniucode.etp.server.web.entity.converter.AcmeOrderStatusConverter;
import com.xiaoniucode.etp.server.web.entity.converter.AcmeValidationModeConverter;
import com.xiaoniucode.etp.server.web.entity.converter.DnsProviderTypeConverter;
import com.xiaoniucode.etp.server.web.enums.AcmeOrderStatus;
import com.xiaoniucode.etp.server.web.enums.AcmeValidationMode;
import com.xiaoniucode.etp.server.web.enums.DnsProviderType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "acme_cert_order")
public class AcmeCertOrderDO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Convert(converter = AcmeOrderStatusConverter.class)
    @Column(nullable = false)
    private AcmeOrderStatus status = AcmeOrderStatus.DRAFT;

    @Column(name = "domains", nullable = false, columnDefinition = "TEXT")
    private String domains;

    @Convert(converter = AcmeValidationModeConverter.class)
    @Column(name = "validation_mode", nullable = false)
    private AcmeValidationMode validationMode;

    @Column(name = "dns_credential_id")
    private Long dnsCredentialId;

    @Convert(converter = DnsProviderTypeConverter.class)
    @Column(name = "dns_provider")
    private DnsProviderType dnsProvider;

    @Column(name = "acme_order_url", columnDefinition = "TEXT")
    private String acmeOrderUrl;

    @Column(name = "cert_id")
    private String certId;

    @Column(name = "bind_proxy_domain_ids", columnDefinition = "TEXT")
    private String bindProxyDomainIds;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
