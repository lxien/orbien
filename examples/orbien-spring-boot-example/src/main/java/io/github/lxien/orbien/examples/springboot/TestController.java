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

/**
 * 测试控制器
 * 提供示例 API 接口和页面路由
 */
@Controller
@RequestMapping("/")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Value("${spring.application.name}")
    private String appName;

    /**
     * REST API 控制器
     * 提供后端接口服务
     */
    @RestController
    @RequestMapping("/api")
    public static class ApiController {
        
        @Value("${spring.application.name}")
        private String appName;

        /**
         * 打招呼接口
         * @return 问候信息
         */
        @GetMapping("/hello")
        public String sayHello() {
            return "Hello " + appName;
        }

        /**
         * 获取请求头信息接口
         * @param request HTTP 请求对象
         * @return 包含 X-Forwarded-For 的响应
         */
        @GetMapping("/headers")
        public ResponseEntity<Map<String, Object>> getHeaders(HttpServletRequest request) {
            Map<String, Object> response = new HashMap<>();
            String visitorIp = request.getHeader("X-Forwarded-For");
            response.put("X-Forwarded-For", visitorIp);
            logger.info("X-Forwarded-For: {}", visitorIp);
            return ResponseEntity.ok(response);
        }

        /**
         * 文件上传接口
         * @param file 上传的文件
         * @return 上传结果
         */
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

    /**
     * 首页路由
     * 转发到 index.html
     * @return ModelAndView
     */
    @GetMapping("")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("forward:/index.html");
        return modelAndView;
    }
}
