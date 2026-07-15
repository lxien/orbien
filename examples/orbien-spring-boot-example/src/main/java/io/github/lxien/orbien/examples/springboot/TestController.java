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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;


@Controller
@RequestMapping("/")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Value("${spring.application.name}")
    private String appName;

    @RestController
    @RequestMapping("/api")
    public static class ApiController {

        @Value("${spring.application.name}")
        private String appName;


        @GetMapping("/hello")
        public String sayHello() {
            return "Hello " + appName;
        }


        @GetMapping("/headers")
        public ResponseEntity<Map<String, Object>> getHeaders(HttpServletRequest request) {
            Map<String, Object> response = new HashMap<>();

            Map<String, String> requestHeaders = new HashMap<>();
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                requestHeaders.put(headerName, request.getHeader(headerName));
            });
            response.put("requestHeaders", requestHeaders);

            logger.info("X-Forwarded-For: {}", request.getHeader("X-Forwarded-For"));

            return ResponseEntity.ok(response);
        }


        @PostMapping("/upload")
        public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
            Map<String, Object> response = new HashMap<>();

            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "上传文件为空");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("========== 文件上传信息 ==========");
            logger.info("文件名: {}", file.getOriginalFilename());
            logger.info("文件大小: {} bytes", file.getSize());
            logger.info("文件类型: {}", file.getContentType());
            logger.info("文件是否为空: {}", file.isEmpty());
            logger.info("文件对象名称: {}", file.getName());
            logger.info("===================================");

            response.put("success", true);
            response.put("message", "文件上传成功，文件信息已打印到日志");
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("forward:/index.html");
        return modelAndView;
    }
}
