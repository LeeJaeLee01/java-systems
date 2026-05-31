package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.config.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true")
public class SnsSmsSender implements SmsSender {

	private final SmsProperties smsProperties;

	private final SnsClient snsClient;

	public SnsSmsSender(SmsProperties smsProperties) {
		this.smsProperties = smsProperties;
		this.snsClient = SnsClient.builder()
			.region(Region.of(smsProperties.getRegion()))
			.build();
	}

	@Override
	public void sendPaymentCompleted(PaymentCompletedEvent event) {
		String phoneNumber = resolvePhoneNumber(event);
		String message = "Payment of %s completed. Ref: %s".formatted(event.amount(), event.paymentId());

		snsClient.publish(PublishRequest.builder()
			.phoneNumber(phoneNumber)
			.message(message)
			.build());

		log.info("SNS SMS sent for payment {} to {}", event.paymentId(), phoneNumber);
	}

	private String resolvePhoneNumber(PaymentCompletedEvent event) {
		return smsProperties.getDefaultCountryCode() + event.userId().toString().replace("-", "").substring(0, 10);
	}

}
