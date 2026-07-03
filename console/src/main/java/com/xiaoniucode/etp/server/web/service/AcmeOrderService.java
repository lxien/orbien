package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.acme.AcmeOrderDTO;
import com.xiaoniucode.etp.server.web.param.acme.AcmeOrderCreateParam;

import java.util.List;

public interface AcmeOrderService {

    AcmeOrderDTO createAndSubmit(AcmeOrderCreateParam param);

    AcmeOrderDTO getDetail(Long orderId);

    PageResult<AcmeOrderDTO> findByPage(PageQuery pageQuery);

    void verify(Long orderId);

    void cancel(Long orderId);

    void retry(Long orderId);

    void deleteByIds(List<Long> ids);
}
