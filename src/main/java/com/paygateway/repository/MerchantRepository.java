package com.paygateway.repository;

import com.paygateway.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);

    Optional<Merchant> findByApiKey(String apiKey);

    boolean existsByEmail(String email);
}
