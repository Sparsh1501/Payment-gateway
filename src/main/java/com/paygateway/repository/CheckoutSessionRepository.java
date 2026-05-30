package com.paygateway.repository;

import com.paygateway.entity.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
}
