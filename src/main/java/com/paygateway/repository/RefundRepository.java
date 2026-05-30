package com.paygateway.repository;

import com.paygateway.entity.Refund;
import com.paygateway.entity.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByIdAndMerchantId(UUID id, UUID merchantId);

    Page<Refund> findByMerchantId(UUID merchantId, Pageable pageable);

    List<Refund> findByPaymentIntentId(UUID paymentIntentId);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM Refund r
            WHERE r.paymentIntentId = :paymentIntentId
              AND r.status IN :statuses
            """)
    BigDecimal sumRefundedAmount(@Param("paymentIntentId") UUID paymentIntentId,
                                 @Param("statuses") List<RefundStatus> statuses);
}
