package com.designstudio.infrastructure.filter;

import com.designstudio.domain.entity.Visit;
import com.designstudio.domain.repository.VisitRepository;
import com.designstudio.infrastructure.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class VisitLoggingFilter extends OncePerRequestFilter {

    private final VisitRepository visitRepository;

    public VisitLoggingFilter(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof CustomUserDetails) {

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            HttpSession session = request.getSession();

            if (session.getAttribute("visitId") == null) {
                Visit visit = new Visit();
                visit.setUser(userDetails.getUser());
                visit.setVisitTime(LocalDateTime.now());
                visit.setDurationSeconds(0);
                visit.setSessionId(session.getId());
                visit = visitRepository.save(visit);
                session.setAttribute("visitId", visit.getId());
            }
        }

        filterChain.doFilter(request, response);
    }
}