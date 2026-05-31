package com.paymentsystem.payment.reconciliation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment.reconciliation")
public class ReconciliationProperties {

	private boolean enabled = true;

	/** Cron for daily reconciliation (default 02:00 UTC). */
	private String cron = "0 0 2 * * *";

	/** Reconcile transactions from N days ago (1 = yesterday). */
	private int lookbackDays = 1;

	private String defaultCurrency = "usd";

}
