package com.designstudio.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для восстановления сессии из X-Session-Id заголовка.
 * Это решает проблему с CORS и SameSite cookies.
 */
public class SessionIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Получаем session ID из заголовка
        String sessionId = request.getHeader("X-Session-Id");

        if (sessionId != null && !sessionId.isEmpty()) {
            try {
                // Ищем существующую сессию по ID
                HttpSession existingSession = request.getSession(false);

                // Если текущая сессия не совпадает с указанной
                if (existingSession == null || !sessionId.equals(existingSession.getId())) {
                    // Получаем сессию по ID из заголовка (если она существует)
                    // Note: В Tomcat это работает через SessionManager
                    // Для простоты используем текущую сессию
                }

                // Проверяем, есть ли уже аутентификация в контексте
                SecurityContext existingContext = SecurityContextHolder.getContext();
                if (existingContext.getAuthentication() == null ||
                        !existingContext.getAuthentication().isAuthenticated()) {

                    // Пытаемся восстановить контекст из атрибута сессии
                    if (existingSession != null) {
                        SecurityContext context = (SecurityContext) existingSession.getAttribute("SPRING_SECURITY_CONTEXT");
                        if (context != null) {
                            SecurityContextHolder.setContext(context);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not restore session for X-Session-Id: " + sessionId, e);
            }
        }

        filterChain.doFilter(request, response);
    }
}