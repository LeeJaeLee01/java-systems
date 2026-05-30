package com.paymentsystem.fraud.repository;

import com.paymentsystem.fraud.domain.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
}
