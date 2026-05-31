package com.paymentsystem.payment.repository;

import com.paymentsystem.payment.domain.ProcessedStripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEvent, java.util.UUID> {

	boolean existsByStripeEventId(String stripeEventId);

}
