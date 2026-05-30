package com.paygateway.service;

import com.paygateway.dto.auth.ApiKeyResponse;
import com.paygateway.dto.auth.MerchantResponse;
import com.paygateway.entity.Merchant;
import com.paygateway.exception.ResourceNotFoundException;
import com.paygateway.repository.MerchantRepository;
import com.paygateway.util.ApiCredentialGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final ApiCredentialGenerator credentialGenerator;

    @Transactional(readOnly = true)
    public Merchant getById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + id));
    }

    @Transactional(readOnly = true)
    public MerchantResponse getProfile(UUID id) {
        return MerchantResponse.from(getById(id));
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getApiCredentials(UUID id) {
        Merchant merchant = getById(id);
        return new ApiKeyResponse(merchant.getApiKey(), merchant.getApiSecret());
    }

    @Transactional
    public ApiKeyResponse regenerateApiCredentials(UUID id) {
        Merchant merchant = getById(id);
        merchant.setApiKey(credentialGenerator.generateApiKey());
        merchant.setApiSecret(credentialGenerator.generateApiSecret());
        merchantRepository.save(merchant);
        log.info("Regenerated API credentials for merchant {}", id);
        return new ApiKeyResponse(merchant.getApiKey(), merchant.getApiSecret());
    }
}
