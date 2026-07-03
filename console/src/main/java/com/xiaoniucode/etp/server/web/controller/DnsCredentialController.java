package com.xiaoniucode.etp.server.web.controller;

import com.xiaoniucode.etp.server.web.common.message.Ajax;
import com.xiaoniucode.etp.server.web.common.message.PageQuery;
import com.xiaoniucode.etp.server.web.param.dns.DnsCredentialSaveParam;
import com.xiaoniucode.etp.server.web.service.DnsCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dns-credential")
@RequiredArgsConstructor
public class DnsCredentialController {

    private final DnsCredentialService dnsCredentialService;

    @GetMapping("/list")
    public Ajax list() {
        return Ajax.success(dnsCredentialService.listAll());
    }

    @GetMapping("/providers")
    public Ajax providers() {
        return Ajax.success(dnsCredentialService.listProviderSchemas());
    }

    @PostMapping
    public Ajax save(@RequestBody @Validated DnsCredentialSaveParam param) {
        return Ajax.success(dnsCredentialService.save(param));
    }

    @DeleteMapping("/{id}")
    public Ajax delete(@PathVariable Long id) {
        dnsCredentialService.delete(id);
        return Ajax.success();
    }

    @PostMapping("/{id}/test")
    public Ajax test(@PathVariable Long id) {
        dnsCredentialService.test(id);
        return Ajax.success();
    }
}
