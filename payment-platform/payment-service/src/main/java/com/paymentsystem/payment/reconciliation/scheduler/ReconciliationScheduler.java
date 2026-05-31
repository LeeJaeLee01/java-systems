package com.paymentsystem.payment.reconciliation.scheduler;

import com.paymentsystem.payment.reconciliation.batch.ReconciliationTasklet;
import com.paymentsystem.payment.reconciliation.config.ReconciliationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliationScheduler {

	private final JobLauncher jobLauncher;
	private final Job stripeReconciliationJob;
	private final ReconciliationProperties reconciliationProperties;

	@Scheduled(cron = "${payment.reconciliation.cron:0 0 2 * * *}", zone = "UTC")
	public void runDailyReconciliation() {
		LocalDate runDate = LocalDate.now(ZoneOffset.UTC).minusDays(reconciliationProperties.getLookbackDays());
		log.info("Launching scheduled Stripe reconciliation for {}", runDate);

		try {
			jobLauncher.run(
				stripeReconciliationJob,
				new JobParametersBuilder()
					.addString(ReconciliationTasklet.RUN_DATE_PARAM, runDate.toString())
					.addLong("launchTime", System.currentTimeMillis())
					.toJobParameters()
			);
		}
		catch (Exception ex) {
			log.error("Scheduled reconciliation failed for {}", runDate, ex);
		}
	}

}
