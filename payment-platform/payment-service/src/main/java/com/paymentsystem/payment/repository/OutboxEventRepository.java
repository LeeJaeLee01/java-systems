package com.paymentsystem.payment.repository;

import com.paymentsystem.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	@Query(value = """
		SELECT *
		FROM outbox_events
		WHERE (
		    status = 'PENDING'
		    AND retry_count < :maxRetries
		    AND (next_retry_at IS NULL OR next_retry_at <= :now)
		  )
		  OR (
		    status = 'PROCESSING'
		    AND claimed_at IS NOT NULL
		    AND claimed_at <= :staleBefore
		  )
		ORDER BY created_at ASC
		LIMIT :limit
		FOR UPDATE SKIP LOCKED
		""", nativeQuery = true)
	List<OutboxEvent> findPendingForDispatch(
		@Param("maxRetries") int maxRetries,
		@Param("now") Instant now,
		@Param("staleBefore") Instant staleBefore,
		@Param("limit") int limit
	);

}
