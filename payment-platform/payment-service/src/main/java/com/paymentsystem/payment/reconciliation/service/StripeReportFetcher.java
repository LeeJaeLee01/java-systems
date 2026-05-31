package com.paymentsystem.payment.reconciliation.service;

import com.paymentsystem.payment.reconciliation.dto.StripePaymentSnapshot;

import java.time.Instant;
import java.util.List;

public interface StripeReportFetcher {

	List<StripePaymentSnapshot> fetchPaymentIntents(Instant fromInclusive, Instant toExclusive);

}
