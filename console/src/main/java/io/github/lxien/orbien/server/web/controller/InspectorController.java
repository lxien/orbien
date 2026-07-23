package io.github.lxien.orbien.server.web.controller;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import io.github.lxien.orbien.server.web.param.inspector.InspectorConfigUpdateParam;
import io.github.lxien.orbien.server.web.param.inspector.ReplayRequestParam;
import io.github.lxien.orbien.server.web.service.inspector.InspectorService;
import io.github.lxien.orbien.server.web.service.inspector.InspectorSseHub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/inspector")
@RequiredArgsConstructor
public class InspectorController {
    private final InspectorService inspectorService;
    private final InspectorSseHub inspectorSseHub;

    @GetMapping("/requests")
    public Ajax listRequests(@RequestParam String proxyId,
                             @RequestParam(defaultValue = "50") int limit) {
        return Ajax.success(inspectorService.listRequests(proxyId, limit));
    }

    @GetMapping("/requests/{id}")
    public Ajax getRequest(@PathVariable String id) {
        return Ajax.success(inspectorService.getRequest(id));
    }

    @PostMapping("/requests/{id}/replay")
    public Ajax replay(@PathVariable String id,
                       @RequestBody(required = false) ReplayRequestParam param) {
        return Ajax.success(inspectorService.replay(id, param));
    }

    @DeleteMapping("/requests")
    public Ajax clearRequests(@RequestParam String proxyId) {
        inspectorService.clearRequests(proxyId);
        return Ajax.success();
    }

    @GetMapping("/config")
    public Ajax getConfig(@RequestParam String proxyId) {
        return Ajax.success(inspectorService.getConfig(proxyId));
    }

    @PutMapping("/config")
    public Ajax updateConfig(@RequestBody @Valid InspectorConfigUpdateParam param) {
        return Ajax.success(inspectorService.updateConfig(param));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String proxyId) {
        return inspectorSseHub.subscribe(proxyId);
    }
}
