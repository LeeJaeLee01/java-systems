package com.paymentsystem.notification.sender;

import com.paymentsystem.common.event.PaymentCompletedEvent;
import com.paymentsystem.notification.config.SesProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.ses.enabled", havingValue = "true")
public class SesEmailSender implements EmailSender {

	private final SesProperties sesProperties;

	private final SesClient sesClient;

	public SesEmailSender(SesProperties sesProperties) {
		this.sesProperties = sesProperties;
		this.sesClient = SesClient.builder()
			.region(Region.of(sesProperties.getRegion()))
			.build();
	}

	@Override
	public void sendPaymentCompleted(PaymentCompletedEvent event) {
		String recipient = resolveRecipient(event);
		String subject = "Payment completed — " + event.paymentId();
		String bodyText = """
			Your payment of %s has been processed successfully.
			Payment ID: %s
			Status: %s
			""".formatted(event.amount(), event.paymentId(), event.status());

		SendEmailRequest request = SendEmailRequest.builder()
			.source(sesProperties.getFromEmail())
			.destination(Destination.builder().toAddresses(recipient).build())
			.message(Message.builder()
				.subject(Content.builder().data(subject).charset("UTF-8").build())
				.body(Body.builder()
					.text(Content.builder().data(bodyText).charset("UTF-8").build())
					.build())
				.build())
			.build();

		sesClient.sendEmail(request);
		log.info("SES email sent for payment {} to {}", event.paymentId(), recipient);
	}

	private String resolveRecipient(PaymentCompletedEvent event) {
		return event.userId() + "@paymentsystem.local";
	}

}
