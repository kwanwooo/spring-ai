package com.example.spring_ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link RagIngestRequest} 的 {@code @NotBlank} 校验单元测试。
 */
class RagIngestRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	void nullTextFailsValidation() {
		assertThat(validator.validate(new RagIngestRequest(null, null))).isNotEmpty();
	}

	@Test
	void blankTextFailsValidation() {
		assertThat(validator.validate(new RagIngestRequest("  ", null))).isNotEmpty();
	}

	@Test
	void validTextPassesValidation() {
		assertThat(validator.validate(new RagIngestRequest("some text", null))).isEmpty();
	}

}
