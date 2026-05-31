package com.paymentsystem.payment.reconciliation.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationJobConfig {

	public static final String JOB_NAME = "stripeReconciliationJob";

	@Bean
	public Job stripeReconciliationJob(JobRepository jobRepository, Step stripeReconciliationStep) {
		return new JobBuilder(JOB_NAME, jobRepository)
			.start(stripeReconciliationStep)
			.build();
	}

	@Bean
	public Step stripeReconciliationStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ReconciliationTasklet reconciliationTasklet
	) {
		return new StepBuilder("stripeReconciliationStep", jobRepository)
			.tasklet(reconciliationTasklet, transactionManager)
			.build();
	}

}
