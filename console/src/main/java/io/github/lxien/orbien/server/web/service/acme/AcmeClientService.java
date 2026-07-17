package io.github.lxien.orbien.server.web.service.acme;

import io.github.lxien.orbien.server.web.common.exception.BizException;
import io.github.lxien.orbien.server.web.common.exception.SystemException;
import io.github.lxien.orbien.server.web.config.AcmeProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcmeClientService {

    private static final Logger logger = LoggerFactory.getLogger(AcmeClientService.class);
    /**
     * 缺少会导致浏览器不信任
     */
    private static final String PREFERRED_ROOT_ISSUER = "ISRG Root X1";

    private final AcmeProperties acmeProperties;

    public Session newSession() {
        return new Session(acmeProperties.resolveDirectoryUrl());
    }

    public Login loadOrCreateLogin(Session session) {
        try {
            File accountKeyFile = new File(acmeProperties.resolveAccountKeyPath());
            KeyPair accountKeyPair;
            if (accountKeyFile.exists()) {
                try (FileReader reader = new FileReader(accountKeyFile)) {
                    accountKeyPair = KeyPairUtils.readKeyPair(reader);
                }
                return new AccountBuilder()
                        .onlyExisting()
                        .useKeyPair(accountKeyPair)
                        .createLogin(session);
            }
            accountKeyFile.getParentFile().mkdirs();
            accountKeyPair = KeyPairUtils.createKeyPair(2048);
            Login login = new AccountBuilder()
                    .agreeToTermsOfService()
                    .useKeyPair(accountKeyPair)
                    .createLogin(session);
            try (FileWriter writer = new FileWriter(accountKeyFile)) {
                KeyPairUtils.writeKeyPair(accountKeyPair, writer);
            }
            return login;
        } catch (Exception e) {
            throw new SystemException("ACME 账户初始化失败", e);
        }
    }

    public PreparedOrder prepareOrder(Login login, List<String> domains) {
        try {
            Order order = login.newOrder().domains(domains.toArray(String[]::new)).create();
            List<PreparedChallenge> challenges = new ArrayList<>();
            for (Authorization auth : order.getAuthorizations()) {
                Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.class)
                        .orElseThrow(() -> new BizException("域名不支持 DNS 验证: " + auth.getIdentifier().getDomain()));
                String authDomain = auth.getIdentifier().getDomain();
                PreparedChallenge item = new PreparedChallenge();
                item.setDomain(authDomain);
                item.setRecordName(DnsNameHelper.buildTxtRecordName(authDomain));
                item.setRecordValue(challenge.getDigest());
                item.setChallengeUrl(challenge.getLocation().toString());
                challenges.add(item);
            }
            PreparedOrder preparedOrder = new PreparedOrder();
            preparedOrder.setOrderUrl(order.getLocation().toString());
            preparedOrder.setChallenges(challenges);
            return preparedOrder;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("创建 ACME 订单失败", e);
        }
    }

    public IssuedCertificate issueCertificate(Login login, String orderUrl) {
        try {
            Order order = login.bindOrder(URI.create(orderUrl).toURL());
            order.fetch();
            if (order.getStatus() != Status.VALID && order.getStatus() != Status.READY) {
                throw new BizException("ACME 订单尚未通过验证，当前状态: " + order.getStatus());
            }
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(2048);
            order.execute(domainKeyPair);
            try {
                Thread.sleep(Duration.ofSeconds(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            order.fetch();
            Certificate certificate = selectPreferredCertificateChain(order.getCertificate());
            IssuedCertificate issued = new IssuedCertificate();
            issued.setKeyPem(writeKeyPem(domainKeyPair));
            issued.setFullChainPem(writeCertificatePem(certificate));
            return issued;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("ACME 证书签发失败", e);
        }
    }

    public void triggerChallenges(Login login, String orderUrl) {
        try {
            Order order = login.bindOrder(URI.create(orderUrl).toURL());
            for (Authorization auth : order.getAuthorizations()) {
                auth.findChallenge(Dns01Challenge.class).ifPresent(challenge -> {
                    try {
                        challenge.trigger();
                    } catch (Exception e) {
                        throw new SystemException("触发 DNS 验证失败: " + auth.getIdentifier().getDomain(), e);
                    }
                });
            }
        } catch (SystemException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("触发 ACME 验证失败", e);
        }
    }

    public boolean pollAuthorizations(Login login, String orderUrl, int maxAttempts, int intervalSeconds) {
        try {
            Order order = login.bindOrder(URI.create(orderUrl).toURL());
            for (int i = 0; i < maxAttempts; i++) {
                boolean allValid = true;
                for (Authorization auth : order.getAuthorizations()) {
                    auth.fetch();
                    if (auth.getStatus() == Status.INVALID) {
                        throw new BizException("域名验证失败: " + auth.getIdentifier().getDomain());
                    }
                    if (auth.getStatus() != Status.VALID) {
                        allValid = false;
                    }
                }
                if (allValid) {
                    order.fetch();
                    return order.getStatus() == Status.VALID || order.getStatus() == Status.READY;
                }
                Thread.sleep(Duration.ofSeconds(intervalSeconds));
            }
            return false;
        } catch (BizException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            throw new SystemException("轮询 ACME 验证状态失败", e);
        }
    }

    public void reloadChallenge(Login login, String challengeUrl) {
        if (!StringUtils.hasText(challengeUrl)) {
            return;
        }
        try {
            Dns01Challenge challenge = login.bindChallenge(URI.create(challengeUrl).toURL(), Dns01Challenge.class);
            challenge.fetch();
        } catch (Exception ignored) {
        }
    }

    /**
     * 优先选择能接到 ISRG Root X1 的证书链（含 Gen Y 交叉签名），避免浏览器无法建立信任路径。
     */
    private Certificate selectPreferredCertificateChain(Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        List<Certificate> candidates = new ArrayList<>();
        candidates.add(certificate);
        try {
            candidates.addAll(certificate.getAlternateCertificates());
        } catch (Exception e) {
            logger.warn("加载 ACME 备用证书链失败，将使用默认链", e);
        }
        for (Certificate candidate : candidates) {
            if (chainContainsIssuerCn(candidate, PREFERRED_ROOT_ISSUER)) {
                logCertificateChain("选用可信任根链", candidate);
                return candidate;
            }
        }
        logCertificateChain("未找到 ISRG Root X1 备用链，沿用默认链", certificate);
        return certificate;
    }

    private static boolean chainContainsIssuerCn(Certificate certificate, String issuerCn) {
        String needle = "CN=" + issuerCn;
        for (X509Certificate cert : certificate.getCertificateChain()) {
            String name = cert.getIssuerX500Principal().getName();
            if (nameEqualsOrContainsCn(name, needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean nameEqualsOrContainsCn(String rfc2253Name, String cnAttr) {
        if (!StringUtils.hasText(rfc2253Name)) {
            return false;
        }
        if (rfc2253Name.equals(cnAttr) || rfc2253Name.startsWith(cnAttr + ",")) {
            return true;
        }
        return rfc2253Name.contains("," + cnAttr + ",") || rfc2253Name.endsWith("," + cnAttr);
    }

    private void logCertificateChain(String message, Certificate certificate) {
        List<X509Certificate> chain = certificate.getCertificateChain();
        List<String> issuers = new ArrayList<>(chain.size());
        for (X509Certificate cert : chain) {
            issuers.add(cert.getIssuerX500Principal().getName());
        }
        logger.info("{}: depth={}, issuers={}", message, chain.size(), issuers);
    }

    private String writeKeyPem(KeyPair keyPair) throws IOException {
        StringWriter writer = new StringWriter();
        KeyPairUtils.writeKeyPair(keyPair, writer);
        return writer.toString();
    }

    private String writeCertificatePem(Certificate certificate) throws IOException {
        StringWriter writer = new StringWriter();
        certificate.writeCertificate(writer);
        return writer.toString();
    }

    @Data
    public static class PreparedOrder {
        private String orderUrl;
        private List<PreparedChallenge> challenges;
    }

    @Data
    public static class PreparedChallenge {
        private String domain;
        private String recordName;
        private String recordValue;
        private String challengeUrl;
    }

    @Data
    public static class IssuedCertificate {
        private String keyPem;
        private String fullChainPem;
    }
}
