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
import com.xiaoniucode.etp.server.web.dto.agent.AgentDTO;
import com.xiaoniucode.etp.server.web.param.agent.AgentBatchDeleteParam;

import java.util.List;
public interface AgentService {
    /**
     * 查询所有客户端
     */
    PageResult<AgentDTO> findByPage(PageQuery pageQuery);
    /**
     * 根据 ID 查询客户端
     */
    AgentDTO findById(String agentId);
    /**
     * 剔除在线客户端
     */
    void kickout(String agentId);

    void deleteBatch(AgentBatchDeleteParam param);

    List<AgentDTO> findAll();
}
