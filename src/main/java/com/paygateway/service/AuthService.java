package com.paygateway.service;

import com.paygateway.auth.JwtService;
import com.paygateway.config.props.JwtProperties;
import com.paygateway.dto.auth.AuthResponse;
import com.paygateway.dto.auth.LoginRequest;
import com.paygateway.dto.auth.RegisterRequest;
import com.paygateway.dto.auth.RegisterResponse;
import com.paygateway.entity.Merchant;
import com.paygateway.entity.enums.MerchantStatus;
import com.paygateway.exception.BusinessException;
import com.paygateway.exception.UnauthorizedException;
import com.paygateway.repository.MerchantRepository;
import com.paygateway.util.ApiCredentialGenerator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiCredentialGenerator credentialGenerator;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (merchantRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_TAKEN", "A merchant with this email already exists");
        }
        Merchant merchant = Merchant.builder()
                .businessName(request.businessName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .apiKey(credentialGenerator.generateApiKey())
                .apiSecret(credentialGenerator.generateApiSecret())
                .status(MerchantStatus.ACTIVE)
                .build();
        merchant = merchantRepository.save(merchant);
        log.info("Registered merchant {} ({})", merchant.getId(), merchant.getEmail());
        return RegisterResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Merchant merchant = merchantRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), merchant.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new UnauthorizedException("Merchant account is " + merchant.getStatus());
        }
        return issueTokens(merchant.getId(), merchant.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        final Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw new UnauthorizedException("Provided token is not a refresh token");
        }
        UUID merchantId = UUID.fromString(claims.getSubject());
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new UnauthorizedException("Merchant no longer exists"));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new UnauthorizedException("Merchant account is " + merchant.getStatus());
        }
        return issueTokens(merchant.getId(), merchant.getEmail());
    }

    private AuthResponse issueTokens(UUID merchantId, String email) {
        String access = jwtService.generateAccessToken(merchantId, email);
        String refresh = jwtService.generateRefreshToken(merchantId, email);
        return new AuthResponse(merchantId, access, refresh, "Bearer",
                jwtProperties.accessTokenExpiry() / 1000);
    }
}
