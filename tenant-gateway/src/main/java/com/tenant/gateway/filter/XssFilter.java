package com.tenant.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS防护全局过滤器
 * 在网关层对请求体和请求参数进行XSS攻击检测与过滤
 * <p>防护策略：
 * <ul>
 *   <li>请求体过滤：对POST/PUT/PATCH请求的JSON请求体进行XSS脚本检测和清除</li>
 *   <li>请求参数过滤：对URL查询参数进行XSS脚本检测</li>
 *   <li>响应头加固：添加X-Content-Type-Options、X-XSS-Protection等安全头</li>
 * </ul>
 * <p>执行顺序：在认证过滤器之后（不需要认证的请求也需要XSS防护）
 *
 * @author Aze
 */
@Component
public class XssFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(XssFilter.class);

    /**
     * XSS脚本特征正则（匹配<script>标签、javascript:协议、事件处理器等）
     */
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<\\s*script[^>]*>|</\\s*script>|javascript\\s*:|on\\s*(error|load|click|mouseover|focus|blur)\\s*=|<\\s*iframe[^>]*>|<\\s*img[^>]+on\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 危险Content-Type（需要检测请求体）
     */
    private static final String JSON_CONTENT_TYPE = "application/json";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 检查URL查询参数中的XSS
        String query = request.getURI().getRawQuery();
        if (containsXss(query)) {
            log.warn("XSS攻击检测：URL参数包含恶意脚本，path={}, query={}", path, sanitizeLog(query));
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            String body = "{\"code\":400,\"message\":\"请求参数包含非法字符\",\"timestamp\":" + System.currentTimeMillis() + "}";
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        // 2. 对JSON请求体进行XSS过滤
        HttpMethod method = request.getMethod();
        if ((method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)
                && isJsonRequest(request)) {
            return filterRequestBody(exchange, chain);
        }

        // 3. 添加安全响应头
        return chain.filter(addSecurityHeaders(exchange));
    }

    /**
     * 对请求体进行XSS过滤
     * 读取请求体内容，检测并清除XSS脚本后重新包装请求
     */
    private Mono<Void> filterRequestBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String body = new String(bytes, StandardCharsets.UTF_8);

                    if (containsXss(body)) {
                        log.warn("XSS攻击检测：请求体包含恶意脚本，path={}", path);
                        String cleanedBody = cleanXss(body);
                        ServerHttpRequestDecorator decorator = getServerHttpRequestDecorator(cleanedBody, request);

                        return chain.filter(addSecurityHeaders(exchange.mutate().request(decorator).build()));
                    }

                    return chain.filter(addSecurityHeaders(exchange));
                })
                // 如果请求体为空（empty Mono），直接继续过滤器链
                .then(chain.filter(addSecurityHeaders(exchange)));
    }

    private static ServerHttpRequestDecorator getServerHttpRequestDecorator(String cleanedBody, ServerHttpRequest request) {
        byte[] cleanedBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);

        return new ServerHttpRequestDecorator(request) {
            @Override
            @NonNull
            public Flux<DataBuffer> getBody() {
                DataBuffer buffer = new DefaultDataBufferFactory()
                        .wrap(cleanedBytes);
                return Flux.just(buffer);
            }

            @Override
            @NonNull
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setContentLength(cleanedBytes.length);
                return headers;
            }
        };
    }

    /**
     * 添加安全响应头
     * <ul>
     *   <li>X-Content-Type-Options: nosniff — 防止浏览器MIME嗅探</li>
     *   <li>X-XSS-Protection: 1; mode=block — 启用浏览器XSS过滤器</li>
     *   <li>X-Frame-Options: DENY — 防止点击劫持</li>
     *   <li>Referrer-Policy: strict-origin-when-cross-origin — 控制Referrer泄露</li>
     * </ul>
     */
    private ServerWebExchange addSecurityHeaders(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
        return exchange;
    }

    /**
     * 检测字符串是否包含XSS攻击特征
     */
    private boolean containsXss(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return XSS_PATTERN.matcher(value).find();
    }

    /**
     * 清除XSS攻击脚本
     * 将匹配到的XSS特征替换为空字符串
     */
    private String cleanXss(String value) {
        return XSS_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * 判断是否为JSON请求
     */
    private boolean isJsonRequest(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return contentType != null && contentType.contains(JSON_CONTENT_TYPE);
    }

    /**
     * 日志脱敏：截断过长的查询参数，防止日志注入
     */
    private String sanitizeLog(String value) {
        if (value.length() > 200) {
            return value.substring(0, 200) + "...(truncated)";
        }
        return value;
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行（XSS防护不需要认证），但在路由转发之前
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}