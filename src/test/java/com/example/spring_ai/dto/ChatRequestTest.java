package com.example.spring_ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link ChatRequest} 的 {@code @NotBlank} 校验单元测试。
 */
class ChatRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	void nullMessageFailsValidation() {
		assertThat(validator.validate(new ChatRequest(null, null, null, null))).isNotEmpty();
	}

	@Test
	void blankMessageFailsValidation() {
		assertThat(validator.validate(new ChatRequest("   ", null, null, null))).isNotEmpty();
	}

	@Test
	void validMessagePassesValidation() {
		assertThat(validator.validate(new ChatRequest("hi", "sys", "glm", "c1"))).isEmpty();
	}

}
