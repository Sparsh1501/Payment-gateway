package com.paygateway.repository;

import com.paygateway.entity.PaymentIntent;
import com.paygateway.entity.enums.PaymentStatus;
import com.paygateway.repository.projection.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    Optional<PaymentIntent> findByIdAndMerchantId(UUID id, UUID merchantId);

    Page<PaymentIntent> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<PaymentIntent> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Optional<PaymentIntent> findByProviderPaymentId(String providerPaymentId);

    long countByMerchantIdAndStatus(UUID merchantId, PaymentStatus status);

    @Query("""
            SELECT p.status AS status, COUNT(p) AS count
            FROM PaymentIntent p
            WHERE p.merchantId = :merchantId
              AND p.createdAt BETWEEN :from AND :to
            GROUP BY p.status
            """)
    List<StatusCount> countByStatus(@Param("merchantId") UUID merchantId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);
}
