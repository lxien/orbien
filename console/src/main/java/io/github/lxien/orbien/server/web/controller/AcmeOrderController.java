package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.common.message.PageQuery;
import io.github.lxien.orbien.server.web.param.acme.AcmeOrderCreateParam;
import io.github.lxien.orbien.server.web.service.AcmeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acme-order")
@RequiredArgsConstructor
public class AcmeOrderController {

    private final AcmeOrderService acmeOrderService;

    @PostMapping("/create")
    public Ajax create(@RequestBody @Validated AcmeOrderCreateParam param) {
        return Ajax.success(acmeOrderService.createAndSubmit(param));
    }

    @GetMapping("/{orderId}")
    public Ajax detail(@PathVariable Long orderId) {
        return Ajax.success(acmeOrderService.getDetail(orderId));
    }

    @GetMapping
    public Ajax page(@ModelAttribute PageQuery pageQuery) {
        return Ajax.success(acmeOrderService.findByPage(pageQuery));
    }

    @PostMapping("/{orderId}/verify")
    public Ajax verify(@PathVariable Long orderId) {
        acmeOrderService.verify(orderId);
        return Ajax.success();
    }

    @PostMapping("/{orderId}/cancel")
    public Ajax cancel(@PathVariable Long orderId) {
        acmeOrderService.cancel(orderId);
        return Ajax.success();
    }

    @PostMapping("/{orderId}/retry")
    public Ajax retry(@PathVariable Long orderId) {
        acmeOrderService.retry(orderId);
        return Ajax.success();
    }

    @DeleteMapping
    public Ajax deleteByIds(@RequestBody List<Long> ids) {
        acmeOrderService.deleteByIds(ids);
        return Ajax.success();
    }
}
