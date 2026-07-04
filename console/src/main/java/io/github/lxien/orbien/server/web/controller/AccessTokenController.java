/*
 *    Copyright 2026 lxien
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
package io.github.lxien.orbien.server.web.controller;
import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.common.message.PageResult;
import io.github.lxien.orbien.server.web.dto.accesstoken.AccessTokenDTO;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenBatchDeleteParam;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenCreateParam;
import io.github.lxien.orbien.server.web.param.accesstoken.AccessTokenUpdateParam;
import io.github.lxien.orbien.server.web.service.AccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/access-tokens")
public class AccessTokenController {
    @Autowired
    private AccessTokenService accessTokenService;
    
    @GetMapping
    public Ajax findByPage(@ModelAttribute PageQuery pageQuery) {
        PageResult<AccessTokenDTO> accessTokens = accessTokenService.findByPage(pageQuery);
        return Ajax.success(accessTokens);
    }
    
    @GetMapping("/{id}")
    public Ajax getById(@PathVariable Integer id) {
        AccessTokenDTO accessToken = accessTokenService.findById(id);
        return Ajax.success(accessToken);
    }
    
    @PostMapping
    public Ajax create(@RequestBody @Validated AccessTokenCreateParam param) {
        AccessTokenDTO createdToken = accessTokenService.create(param);
        return Ajax.success(createdToken);
    }
    
    @PutMapping
    public Ajax update(@RequestBody @Validated AccessTokenUpdateParam param) {
        accessTokenService.update(param);
        return Ajax.success();
    }
    
    @DeleteMapping("/{id}")
    public Ajax delete(@PathVariable Integer id) {
        accessTokenService.delete(id);
        return Ajax.success();
    }
    
    @DeleteMapping
    public Ajax batchDelete(@RequestBody @Validated AccessTokenBatchDeleteParam param) {
        accessTokenService.deleteBatch(param);
        return Ajax.success();
    }
}
