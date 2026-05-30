package com.paygateway.auth;

import com.paygateway.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Convenience accessor for the authenticated {@link MerchantPrincipal}.
 */
public final class AuthContext {

    private AuthContext() {
    }

    public static MerchantPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MerchantPrincipal principal)) {
            throw new UnauthorizedException("No authenticated merchant in context");
        }
        return principal;
    }

    public static UUID currentMerchantId() {
        return currentPrincipal().merchantId();
    }
}
