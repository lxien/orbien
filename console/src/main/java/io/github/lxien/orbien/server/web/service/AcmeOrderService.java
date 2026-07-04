package io.github.lxien.orbien.server.web.service;

import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.acme.AcmeOrderDTO;
import io.github.lxien.orbien.server.web.param.acme.AcmeOrderCreateParam;

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
