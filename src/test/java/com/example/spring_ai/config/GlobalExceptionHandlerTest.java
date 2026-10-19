package com.example.spring_ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.spring_ai.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * {@link GlobalExceptionHandler} 把异常映射为 {@link ErrorResponse} 的单元测试。
 */
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		this.handler = new GlobalExceptionHandler();
	}

	@Test
	void illegalArgumentMapsTo400() {
		ResponseEntity<ErrorResponse> resp = this.handler.handleIllegalArgument(new IllegalArgumentException("不支持的模型提供方: x"));

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody().code()).isEqualTo(400);
		assertThat(resp.getBody().message()).isEqualTo("不支持的模型提供方: x");
	}

	@Test
	void validationMapsFirstFieldErrorTo400() {
		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("chatRequest", "message", "message 不能为空");
		when(ex.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

		ResponseEntity<ErrorResponse> resp = this.handler.handleValidation(ex);

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(resp.getBody().message()).isEqualTo("message: message 不能为空");
	}

	@Test
	void validationWithoutFieldErrorsUsesFallbackMessage() {
		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);
		when(ex.getBindingResult()).thenReturn(bindingResult);
		when(bindingResult.getFieldErrors()).thenReturn(List.of());

		ResponseEntity<ErrorResponse> resp = this.handler.handleValidation(ex);

		assertThat(resp.getBody().message()).isEqualTo("参数校验失败");
	}

	@Test
	void genericMapsTo500() {
		ResponseEntity<ErrorResponse> resp = this.handler.handleGeneric(new RuntimeException("boom"));

		assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(resp.getBody().code()).isEqualTo(500);
		assertThat(resp.getBody().message()).isEqualTo("服务器内部错误");
	}

}
