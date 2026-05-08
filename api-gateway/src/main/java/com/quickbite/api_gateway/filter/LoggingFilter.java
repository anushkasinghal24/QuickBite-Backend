//package com.quickbite.gateway.filter;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import reactor.core.publisher.Mono;
//
///**
// * LoggingFilter
// *
// * Logs every request passing through the Gateway.
// * Useful for debugging routing issues during development.
// *
// * Logs:
// *   → Request: method, path, X-User-Id (if present)
// *   → Response: HTTP status code
// *   → Timing: how long the request took (ms)
// */
//@Configuration
//@Slf4j
//public class LoggingFilter {
//
//    @Bean
//    @Order(2)  // Run after JwtAuthenticationFilter (which is Order 1 = HIGHEST_PRECEDENCE)
//    public GlobalFilter loggingFilter() {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//            long startTime = System.currentTimeMillis();
//
//            String userId = request.getHeaders().getFirst("X-User-Id");
//            String role   = request.getHeaders().getFirst("X-User-Role");
//
//            log.info("→ Gateway Request: [{} {}] | UserId: {} | Role: {}",
//                    request.getMethod(),
//                    request.getURI().getPath(),
//                    userId != null ? userId : "GUEST",
//                    role   != null ? role   : "NONE"
//            );
//
//            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//                ServerHttpResponse response = exchange.getResponse();
//                long duration = System.currentTimeMillis() - startTime;
//
//                log.info("← Gateway Response: [{} {}] | Status: {} | Time: {}ms",
//                        request.getMethod(),
//                        request.getURI().getPath(),
//                        response.getStatusCode(),
//                        duration
//                );
//            }));
//        };
//    }
//}

package com.quickbite.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return 2; // after JWT filter
    }

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String userId = request.getHeaders().getFirst("X-User-Id");
        String role   = request.getHeaders().getFirst("X-User-Role");

        log.info("→ Gateway Request: [{} {}] | UserId: {} | Role: {}",
                request.getMethod(),
                request.getURI().getPath(),
                userId != null ? userId : "GUEST",
                role   != null ? role   : "NONE"
        );

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;

            log.info("← Gateway Response: [{} {}] | Status: {} | Time: {}ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    response.getStatusCode(),
                    duration
            );
        }));
    }
}
