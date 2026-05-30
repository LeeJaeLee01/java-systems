package com.paymentsystem.notification.repository;

import com.paymentsystem.notification.domain.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

	boolean existsByMessageId(UUID messageId);

}
