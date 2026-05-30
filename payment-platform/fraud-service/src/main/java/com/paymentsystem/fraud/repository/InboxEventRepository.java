package com.paymentsystem.fraud.repository;

import com.paymentsystem.fraud.domain.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

	boolean existsByMessageId(UUID messageId);

}
