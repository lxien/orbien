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
package io.github.lxien.orbien.server.web.service.converter;

import io.github.lxien.orbien.core.domain.HeaderRewriteRule;
import io.github.lxien.orbien.core.enums.HeaderAction;
import io.github.lxien.orbien.core.enums.HeaderDirection;
import io.github.lxien.orbien.server.web.dto.headerrewrite.HeaderRewriteDetailDTO;
import io.github.lxien.orbien.server.web.dto.headerrewrite.HeaderRewriteRuleDTO;
import io.github.lxien.orbien.server.web.entity.HeaderRewriteDO;
import io.github.lxien.orbien.server.web.entity.HeaderRewriteRuleDO;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleAddParam;
import io.github.lxien.orbien.server.web.param.headerrewrite.HeaderRewriteRuleUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface HeaderRewriteConvert {

    @Mapping(target = "requestRules", expression = "java(toRequestRuleDTOs(rules))")
    @Mapping(target = "responseRules", expression = "java(toResponseRuleDTOs(rules))")
    HeaderRewriteDetailDTO toDetailDTO(HeaderRewriteDO rewriteDO, List<HeaderRewriteRuleDO> rules);

    @Mapping(target = "direction", expression = "java(ruleDO.getDirection() == null ? null : ruleDO.getDirection().getCode())")
    @Mapping(target = "action", expression = "java(ruleDO.getAction() == null ? null : ruleDO.getAction().getCode())")
    HeaderRewriteRuleDTO toRuleDTO(HeaderRewriteRuleDO ruleDO);

    List<HeaderRewriteRuleDTO> toRuleDTOList(List<HeaderRewriteRuleDO> rules);

    default List<HeaderRewriteRuleDTO> toRequestRuleDTOs(List<HeaderRewriteRuleDO> rules) {
        return filterByDirection(rules, HeaderDirection.REQUEST);
    }

    default List<HeaderRewriteRuleDTO> toResponseRuleDTOs(List<HeaderRewriteRuleDO> rules) {
        return filterByDirection(rules, HeaderDirection.RESPONSE);
    }

    default List<HeaderRewriteRuleDTO> filterByDirection(List<HeaderRewriteRuleDO> rules, HeaderDirection direction) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<HeaderRewriteRuleDTO> result = new ArrayList<>();
        for (HeaderRewriteRuleDO rule : rules) {
            if (rule.getDirection() == direction) {
                result.add(toRuleDTO(rule));
            }
        }
        return result;
    }

    default HeaderRewriteRuleDO toRuleDO(HeaderRewriteRuleAddParam param) {
        HeaderRewriteRuleDO ruleDO = new HeaderRewriteRuleDO();
        ruleDO.setProxyId(param.getProxyId());
        ruleDO.setDirection(HeaderDirection.fromCode(param.getDirection()));
        ruleDO.setAction(HeaderAction.fromCode(param.getAction()));
        ruleDO.setName(param.getName());
        ruleDO.setValue(param.getValue());
        return ruleDO;
    }

    default void updateRuleDO(HeaderRewriteRuleDO ruleDO, HeaderRewriteRuleUpdateParam param) {
        ruleDO.setDirection(HeaderDirection.fromCode(param.getDirection()));
        ruleDO.setAction(HeaderAction.fromCode(param.getAction()));
        ruleDO.setName(param.getName());
        ruleDO.setValue(param.getValue());
    }

    default HeaderRewriteRule toDomainRule(HeaderRewriteRuleDO ruleDO) {
        return new HeaderRewriteRule(ruleDO.getAction(), ruleDO.getName(), ruleDO.getValue());
    }
}
