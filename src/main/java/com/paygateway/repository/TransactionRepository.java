package com.paygateway.repository;

import com.paygateway.entity.Transaction;
import com.paygateway.repository.projection.CurrencyVolume;
import com.paygateway.repository.projection.DailyRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.paymentIntentId IN (
                SELECT p.id FROM PaymentIntent p WHERE p.merchantId = :merchantId
            )
            AND t.type = com.paygateway.entity.enums.TransactionType.PAYMENT
            AND t.createdAt BETWEEN :from AND :to
            """)
    BigDecimal totalVolume(@Param("merchantId") UUID merchantId,
                           @Param("from") Instant from,
                           @Param("to") Instant to);

    @Query("""
            SELECT CAST(t.createdAt AS date) AS day, COALESCE(SUM(t.amount), 0) AS revenue
            FROM Transaction t
            WHERE t.paymentIntentId IN (
                SELECT p.id FROM PaymentIntent p WHERE p.merchantId = :merchantId
            )
            AND t.type = com.paygateway.entity.enums.TransactionType.PAYMENT
            AND t.createdAt BETWEEN :from AND :to
            GROUP BY CAST(t.createdAt AS date)
            ORDER BY CAST(t.createdAt AS date)
            """)
    List<DailyRevenue> revenueByDay(@Param("merchantId") UUID merchantId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    @Query("""
            SELECT t.currency AS currency, COALESCE(SUM(t.amount), 0) AS volume
            FROM Transaction t
            WHERE t.paymentIntentId IN (
                SELECT p.id FROM PaymentIntent p WHERE p.merchantId = :merchantId
            )
            AND t.type = com.paygateway.entity.enums.TransactionType.PAYMENT
            AND t.createdAt BETWEEN :from AND :to
            GROUP BY t.currency
            ORDER BY SUM(t.amount) DESC
            """)
    List<CurrencyVolume> volumeByCurrency(@Param("merchantId") UUID merchantId,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);
}
