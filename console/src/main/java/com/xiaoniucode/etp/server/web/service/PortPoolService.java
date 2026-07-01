/*
 *    Copyright 2026 xiaoniucode
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

package com.xiaoniucode.etp.server.web.service;

import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.common.message.PageResult;
import com.xiaoniucode.etp.server.web.dto.portpool.PortPoolDTO;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolBatchDeleteParam;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolCreateParam;
import com.xiaoniucode.etp.server.web.param.portpool.PortPoolUpdateParam;

public interface PortPoolService {
    PageResult<PortPoolDTO> findByPage(PageQuery pageQuery);

    PortPoolDTO getById(Long id);

    PortPoolDTO create(PortPoolCreateParam param);

    void update(PortPoolUpdateParam param);

    void deleteBatch(PortPoolBatchDeleteParam param);
}
