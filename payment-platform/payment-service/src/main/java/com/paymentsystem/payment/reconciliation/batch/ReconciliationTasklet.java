package com.paymentsystem.payment.reconciliation.batch;

import com.paymentsystem.payment.reconciliation.service.ReconciliationJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReconciliationTasklet implements Tasklet {

	public static final String RUN_DATE_PARAM = "runDate";

	private final ReconciliationJobService reconciliationJobService;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		String runDateValue = chunkContext.getStepContext()
			.getJobParameters()
			.get(RUN_DATE_PARAM)
			.toString();
		reconciliationJobService.reconcile(LocalDate.parse(runDateValue));
		return RepeatStatus.FINISHED;
	}

}
