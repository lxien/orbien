/*
 *
 *  *    Copyright 2026 xiaoniucode
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xiaoniucode.etp.server.web.service.impl;

import com.xiaoniucode.etp.server.web.common.exception.BizException;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.common.utils.DateUtil;
import com.xiaoniucode.etp.server.web.common.utils.SslParser;
import com.xiaoniucode.etp.server.web.dto.ssl.SslCertDTO;
import com.xiaoniucode.etp.server.web.entity.SslCertificateDO;
import com.xiaoniucode.etp.server.web.enums.SslStatus;
import com.xiaoniucode.etp.server.web.param.ssl.SslCertSaveParam;
import com.xiaoniucode.etp.server.web.repository.SslCertificateRepository;
import com.xiaoniucode.etp.server.web.service.SslCertificateService;
import com.xiaoniucode.etp.server.web.service.converter.SslCertificateConvert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SslCertificateServiceImpl implements SslCertificateService {
    @Autowired
    private SslCertificateRepository sslCertificateRepository;
    @Autowired
    private SslCertificateConvert sslCertificateConvert;

    @Override
    public SslCertDTO saveCert(SslCertSaveParam param) {
        SslParser.SslInfo sslInfo = SslParser.parsePem(param.getFullChain());
        if (sslInfo.hasError()) {
            throw new BizException("证书不可用");
        }
        String sha256Fingerprint = sslInfo.getSha256Fingerprint();
        if (sslCertificateRepository.existsByFingerprint(sha256Fingerprint)) {
            throw new BizException("该证书已经存在");
        }
        
        SslCertificateDO sslCertificateDO = new SslCertificateDO();
        sslCertificateDO.setIssuer(sslInfo.issuer);
        sslCertificateDO.setOrg(sslInfo.organization);
        sslCertificateDO.setSanDomains(String.join(",", sslInfo.dns));
        sslCertificateDO.setFingerprint(sslInfo.sha256Fingerprint);
        LocalDate notBefore = DateUtil.toLocalDate(sslInfo.issuedAt);
        LocalDate notAfter = DateUtil.toLocalDate(sslInfo.expiresAt);
        sslCertificateDO.setNotBefore(notBefore);
        sslCertificateDO.setNotAfter(notAfter);

        LocalDate today = LocalDate.now();
        SslStatus status = (notAfter != null && today.isAfter(notAfter)) ? SslStatus.EXPIRED : SslStatus.ACTIVE;
        sslCertificateDO.setStatus(status);
        
        sslCertificateRepository.saveAndFlush(sslCertificateDO);
        return sslCertificateConvert.toDTO(sslCertificateDO);
    }

    @Override
    public PageResult<SslCertDTO> findByPage(PageQuery pageQuery) {
        int currentPage = Math.max(0, pageQuery.getCurrent() - 1);
        Pageable pageable = PageRequest.of(currentPage, pageQuery.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SslCertificateDO> resultPage = sslCertificateRepository.findAll(pageable);
        if (resultPage.isEmpty()) {
            return PageResult.empty(pageQuery.getCurrent(), pageQuery.getSize());
        }
        List<SslCertDTO> dtoList = sslCertificateConvert.toDTOList(resultPage.getContent());
        return PageResult.wrap(resultPage, dtoList);
    }
}
