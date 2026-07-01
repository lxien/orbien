/*
 *    Copyright 2026 xiaoniucode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http:
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.proxy.HttpProxyListDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.HttpProxyDetailDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.TcpProxyListDTO;
import com.xiaoniucode.etp.server.web.dto.proxy.TcpProxyDetailDTO;
import com.xiaoniucode.etp.server.web.param.proxy.*;

public interface ProxyService {
    /**
     * 创建 TCP 代理
     */
    void createTcpProxy(TcpProxyCreateParam param);

    /**
     * 创建 HTTP 代理
     */
    void createHttpProxy(HttpProxyCreateParam param);

    /**
     * 更新 TCP 代理
     */
    void updateTcpProxy(TcpProxyUpdateParam param);

    /**
     * 更新 HTTP 代理
     */
    void updateHttpProxy(HttpProxyUpdateParam param);

    /**
     * 根据 ID 查询tcp代理
     */
    TcpProxyDetailDTO getTcpProxyById(String id);

    HttpProxyDetailDTO getHttpProxyById(String id);

    void batchDeleteProxies(ProxyBatchDeleteParam param);

    void setProxyStatus(String id, Integer status);

    PageResult<TcpProxyListDTO> findTcpProxies(PageQuery pageQuery);

    PageResult<HttpProxyListDTO> findHttpProxies(PageQuery pageQuery);

    PageResult<HttpProxyListDTO> findHttpsProxies(PageQuery pageQuery);
}