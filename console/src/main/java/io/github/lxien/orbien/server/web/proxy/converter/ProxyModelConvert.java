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

package io.github.lxien.orbien.server.web.proxy.converter;

import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.server.web.entity.*;
import io.github.lxien.orbien.core.domain.*;
import io.github.lxien.orbien.core.enums.AccessControl;
import io.github.lxien.orbien.server.web.entity.*;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ProxyModelConvert {
    @Mapping(target = "proxyId", source = "id")
    ProxyConfig toProxyConfig(ProxyDO proxyDO);

    List<Target> toTargetModel(List<ProxyTargetDO> proxyTargetDos);

    AccessControlConfig toAccessControlConfig(AccessControlDO accessControlDO);

    BasicAuthConfig toBasicAuthConfig(BasicAuthDO basicAuthDO);

    Socks5AuthConfig toSocks5AuthConfig(Socks5AuthDO socks5AuthDO);

    @Mapping(target = "users", ignore = true)
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(fileShareAuthDO.getEnabled()))")
    FileShareAuthConfig toFileShareAuthConfig(FileShareAuthDO fileShareAuthDO);

    FileShareLimitsConfig toFileShareLimitsConfig(FileShareLimitsDO limitsDO);

    default List<Socks5AuthConfig.Socks5User> toSocks5UserConfig(List<Socks5UserDO> users) {
        return mapSocks5Users(users);
    }

    default List<FileShareAuthConfig.FileShareUser> toFileShareUserConfig(List<FileShareUserDO> users) {
        return mapFileShareUsers(users);
    }

    default List<Socks5AuthConfig.Socks5User> mapSocks5Users(List<Socks5UserDO> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(user -> new Socks5AuthConfig.Socks5User(user.getUsername(), user.getPassword()))
                .toList();
    }

    default List<FileShareAuthConfig.FileShareUser> mapFileShareUsers(List<FileShareUserDO> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(user -> new FileShareAuthConfig.FileShareUser(
                        user.getUsername(), user.getPassword(), user.getPermission()))
                .toList();
    }

    Set<HttpUser> toBasicUserDomains(List<BasicUserDO> basicUsers);

    @Mapping(target = "proxyId", source = "id")
    List<ProxyConfig> toProxyConfig(List<ProxyDO> proxyDOS);

    Set<HttpUser> toBasicAuthUserConfig(List<BasicUserDO> basicUsers);

    //-----------------------------------------------ModelToDO---------------------------------------------------------
    @Mapping(target = "id", source = "proxyId")
    ProxyDO toProxyDO(ProxyConfig config);

    @Mapping(target = "limitTotal", expression = "java(bandwidth.getTotalBps())")
    @Mapping(target = "limitIn", expression = "java(bandwidth.getInBps())")
    @Mapping(target = "limitOut", expression = "java(bandwidth.getOutBps())")
    void updateProxyDO(@MappingTarget ProxyDO proxyDO, BandwidthConfig bandwidth);

    default AccessControlDO toAccessControlDO(AccessControlConfig accessControl, String proxyId) {
        if (accessControl == null) {
            return null;
        }
        AccessControlDO accessControlDO = new AccessControlDO();
        accessControlDO.setProxyId(proxyId);
        accessControlDO.setEnabled(accessControl.isEnabled());
        accessControlDO.setMode(accessControl.getMode());
        return accessControlDO;
    }

    default List<AccessControlRuleDO> toAccessControlRuleDO(AccessControlConfig accessControl, String proxyId) {
        if (accessControl == null) {
            return List.of();
        }
        List<AccessControlRuleDO> rules = new ArrayList<>();

        if (accessControl.getAllowView() != null) {
            for (String cidr : accessControl.getAllowView()) {
                AccessControlRuleDO ruleDO = new AccessControlRuleDO();
                ruleDO.setProxyId(proxyId);
                ruleDO.setCidr(cidr);
                ruleDO.setMode(AccessControl.ALLOW);
                rules.add(ruleDO);
            }
        }

        if (accessControl.getDenyView() != null) {
            for (String cidr : accessControl.getDenyView()) {
                AccessControlRuleDO ruleDO = new AccessControlRuleDO();
                ruleDO.setProxyId(proxyId);
                ruleDO.setCidr(cidr);
                ruleDO.setMode(AccessControl.DENY);
                rules.add(ruleDO);
            }
        }

        return rules;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proxyId", source = "proxyId")
    ProxyTargetDO toProxyTargetDO(Target target, String proxyId);

    default List<ProxyTargetDO> toProxyTargetDOList(List<Target> targets, String proxyId) {
        if (targets == null) {
            return List.of();
        }
        List<ProxyTargetDO> proxyTargets = new ArrayList<>();
        for (Target target : targets) {
            ProxyTargetDO proxyTargetDO = new ProxyTargetDO();
            proxyTargetDO.setProxyId(proxyId);
            proxyTargetDO.setHost(target.getHost());
            proxyTargetDO.setPort(target.getPort());
            proxyTargetDO.setWeight(target.getWeight());
            proxyTargetDO.setName(target.getName());
            proxyTargets.add(proxyTargetDO);
        }
        return proxyTargets;
    }

    @Mapping(target = "proxyId", expression = "java(proxyId)")
    BasicUserDO toBasicUserDO(HttpUser httpUser, @Context String proxyId);
    List<BasicUserDO> toBasicUserDOList(Set<HttpUser> basicUsers, @Context String proxyId);
}
