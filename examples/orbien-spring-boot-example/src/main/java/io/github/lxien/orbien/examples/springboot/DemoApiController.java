/*
 *
 *  *    Copyright 2026 lxien
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package io.github.lxien.orbien.examples.springboot;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoApiController {

    private static final Logger logger = LoggerFactory.getLogger(DemoApiController.class);

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/hello")
    public Map<String, Object> sayHello() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Hello " + appName);
        body.put("time", Instant.now().toString());
        return body;
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> getHeaders(HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName ->
                requestHeaders.put(headerName, request.getHeader(headerName)));
        response.put("requestHeaders", requestHeaders);
        response.put("replay", isReplay(request));
        response.put("replaySourceId", request.getHeader("Orbien-Replay-Original-Request-ID"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public Map<String, Object> echoJson(@RequestBody(required = false) Map<String, Object> payload,
                                        HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("received", payload != null ? payload : Map.of());
        response.put("time", Instant.now().toString());
        response.put("replay", isReplay(request));
        response.put("replaySourceId", request.getHeader("Orbien-Replay-Original-Request-ID"));
        response.put("xff", request.getHeader("X-Forwarded-For"));
        return response;
    }

    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> submitForm(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) String message,
                                          HttpServletRequest request) {
        Map<String, Object> received = new LinkedHashMap<>();
        received.put("name", name);
        received.put("message", message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("received", received);
        response.put("time", Instant.now().toString());
        response.put("replay", isReplay(request));
        response.put("replaySourceId", request.getHeader("Orbien-Replay-Original-Request-ID"));
        return response;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "empty file");
            return ResponseEntity.badRequest().body(response);
        }

        logger.info("upload name={} size={} type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        response.put("success", true);
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("contentType", file.getContentType());
        response.put("replay", isReplay(request));
        response.put("replaySourceId", request.getHeader("Orbien-Replay-Original-Request-ID"));

        return ResponseEntity.ok(response);
    }

    private static boolean isReplay(HttpServletRequest request) {
        String id = request.getHeader("Orbien-Replay-Original-Request-ID");
        return id != null && !id.isBlank();
    }
}
