package com.paymentsystem.payment.idempotency;

import com.paymentsystem.common.dto.ApiResponse;
import com.paymentsystem.common.idempotency.Idempotent;
import com.paymentsystem.payment.exception.IdempotencyInProgressException;
import com.paymentsystem.payment.exception.IdempotencyKeyMissingException;
import com.paymentsystem.payment.service.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

	@Mock
	private IdempotencyService idempotencyService;

	@Mock
	private ProceedingJoinPoint joinPoint;

	@Mock
	private MethodSignature methodSignature;

	@InjectMocks
	private IdempotencyAspect idempotencyAspect;

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void returnsCachedResponseWithoutExecutingHandler() throws Throwable {
		bindRequest("Idempotency-Key", "dup-key");
		ApiResponse<String> cached = ApiResponse.ok("cached");
		when(methodSignature.getReturnType()).thenReturn((Class) ApiResponse.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		when(idempotencyService.findCachedResponse("dup-key", ApiResponse.class)).thenReturn(Optional.of(cached));

		Object result = idempotencyAspect.enforceIdempotency(joinPoint, sampleAnnotation());

		assertThat(result).isEqualTo(cached);
		verify(joinPoint, never()).proceed();
	}

	@Test
	void rejectsMissingHeader() {
		bindRequest(null, null);

		assertThatThrownBy(() -> idempotencyAspect.enforceIdempotency(joinPoint, sampleAnnotation()))
			.isInstanceOf(IdempotencyKeyMissingException.class);
	}

	@Test
	void rejectsConcurrentDuplicateWhileProcessing() throws Throwable {
		bindRequest("X-Idempotency-Key", "busy-key");
		when(methodSignature.getReturnType()).thenReturn((Class) ApiResponse.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		when(idempotencyService.findCachedResponse("busy-key", ApiResponse.class)).thenReturn(Optional.empty());
		when(idempotencyService.tryAcquireProcessingLock("busy-key")).thenReturn(false);

		assertThatThrownBy(() -> idempotencyAspect.enforceIdempotency(joinPoint, sampleAnnotation()))
			.isInstanceOf(IdempotencyInProgressException.class);
	}

	private void bindRequest(String headerName, String headerValue) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (headerName != null && headerValue != null) {
			request.addHeader(headerName, headerValue);
		}
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	private Idempotent sampleAnnotation() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("sample");
		return method.getAnnotation(Idempotent.class);
	}

	private static class SampleController {

		@Idempotent
		ApiResponse<String> sample() {
			return ApiResponse.ok("live");
		}

	}

}
