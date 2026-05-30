package com.paygateway.auth;

import com.paygateway.entity.Merchant;
import com.paygateway.entity.enums.MerchantStatus;
import com.paygateway.repository.MerchantRepository;
import com.paygateway.util.HmacUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying {@code X-API-Key} + {@code X-API-Secret} headers.
 * Runs before the JWT filter; both auth mechanisms are supported.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final MerchantRepository merchantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        String apiSecret = request.getHeader("X-API-Secret");

        if (StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            merchantRepository.findByApiKey(apiKey).ifPresent(merchant -> {
                if (merchant.getStatus() == MerchantStatus.ACTIVE
                        && HmacUtil.constantTimeEquals(merchant.getApiSecret(), apiSecret)) {
                    authenticate(request, merchant);
                } else {
                    log.warn("API key authentication failed for key prefix {}",
                            apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);
                }
            });
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Merchant merchant) {
        MerchantPrincipal principal = new MerchantPrincipal(merchant.getId(), merchant.getEmail());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
