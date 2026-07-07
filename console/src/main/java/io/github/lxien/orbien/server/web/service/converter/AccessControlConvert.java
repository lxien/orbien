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

import io.github.lxien.orbien.core.domain.AccessControlConfig;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.server.web.dto.accesscontrol.AccessControlDetailDTO;
import io.github.lxien.orbien.server.web.dto.accesscontrol.AccessControlRuleDTO;
import io.github.lxien.orbien.server.web.entity.AccessControlDO;
import io.github.lxien.orbien.server.web.entity.AccessControlRuleDO;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlRuleAddParam;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlRuleUpdateParam;
import io.github.lxien.orbien.server.web.param.accesscontrol.AccessControlUpdateParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",imports = {AccessControl.class})
public interface AccessControlConvert {

    @Mapping(expression = "java(AccessControl.fromCode(param.getRuleType()))", target = "mode")
    AccessControlRuleDO toRuleDO(AccessControlRuleAddParam param);

    @Mapping(expression = "java(AccessControl.fromCode(param.getMode()))", target = "mode")
    @Mapping(target = "proxyId",ignore = true)
    void updateDO(@MappingTarget AccessControlDO accessControlDO, AccessControlUpdateParam param);

    @Mapping(expression = "java(AccessControl.fromCode(param.getRuleType()))", target = "mode")
    void updateRuleDO(@MappingTarget AccessControlRuleDO accessControlRuleDO, AccessControlRuleUpdateParam param);

    @Mapping(expression = "java(accessControlDO.getMode().getCode())", target = "mode")
    @Mapping(expression = "java(toRuleDTOList(rules))", target = "rules")
    AccessControlDetailDTO toDetailDTO(AccessControlDO accessControlDO, List<AccessControlRuleDO> rules);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "proxyId", target = "proxyId")
    @Mapping(source = "cidr", target = "cidr")
    @Mapping(expression = "java(ruleDO.getMode().getCode())", target = "ruleType")
    AccessControlRuleDTO toRuleDTO(AccessControlRuleDO ruleDO);

    List<AccessControlRuleDTO> toRuleDTOList(List<AccessControlRuleDO> rules);

    @Mapping(expression = "java(toRuleDTOList(accessControl))", target = "rules")
    @Mapping(expression = "java(accessControl.getMode().getCode())", target = "mode")
    AccessControlDetailDTO toDetailDTO(AccessControlConfig accessControl);


   default List<AccessControlRuleDTO> toRuleDTOList(AccessControlConfig accessControl){
       Set<String> deny = accessControl.getDeny();
       Set<String> allow = accessControl.getAllow();
       
       List<AccessControlRuleDTO> rules = new java.util.ArrayList<>();
       
       if (deny != null) {
           for (String cidr : deny) {
               AccessControlRuleDTO ruleDTO = new AccessControlRuleDTO();
               ruleDTO.setCidr(cidr);
               ruleDTO.setRuleType(AccessControl.DENY.getCode());
               rules.add(ruleDTO);
           }
       }
       
       if (allow != null) {
           for (String cidr : allow) {
               AccessControlRuleDTO ruleDTO = new AccessControlRuleDTO();
               ruleDTO.setCidr(cidr);
               ruleDTO.setRuleType(AccessControl.ALLOW.getCode());
               rules.add(ruleDTO);
           }
       }
       
       return rules;
   }






}

