package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.dto.socks5auth.Socks5AuthDetailDTO;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5AuthUpdateParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserAddParam;
import io.github.lxien.orbien.server.web.param.socks5auth.Socks5UserUpdateParam;
import io.github.lxien.orbien.server.web.service.Socks5AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/socks5-auth")
public class Socks5AuthController {

    @Autowired
    private Socks5AuthService socks5AuthService;

    @GetMapping("/{proxyId}")
    public Ajax get(@PathVariable String proxyId) {
        Socks5AuthDetailDTO detail = socks5AuthService.getByProxyId(proxyId);
        return Ajax.success(detail);
    }

    @PutMapping
    public Ajax update(@RequestBody @Validated Socks5AuthUpdateParam param) {
        socks5AuthService.update(param);
        return Ajax.success();
    }

    @PostMapping("/user")
    public Ajax addUser(@RequestBody @Validated Socks5UserAddParam param) {
        socks5AuthService.addUser(param);
        return Ajax.success();
    }

    @PutMapping("/user")
    public Ajax updateUser(@RequestBody @Validated Socks5UserUpdateParam param) {
        socks5AuthService.updateUser(param);
        return Ajax.success();
    }

    @DeleteMapping("/user/{id}")
    public Ajax deleteUser(@PathVariable Long id) {
        socks5AuthService.deleteUser(id);
        return Ajax.success();
    }
}
