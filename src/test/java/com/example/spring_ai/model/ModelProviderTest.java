package com.example.spring_ai.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * {@link ModelProvider#resolve(String)} 的路由逻辑单元测试。
 */
class ModelProviderTest {

	@Test
	void resolveNullDefaultsToDeepseek() {
		assertThat(ModelProvider.resolve(null)).isEqualTo(ModelProvider.DEEPSEEK);
	}

	@Test
	void resolveBlankDefaultsToDeepseek() {
		assertThat(ModelProvider.resolve("   ")).isEqualTo(ModelProvider.DEEPSEEK);
		assertThat(ModelProvider.resolve("")).isEqualTo(ModelProvider.DEEPSEEK);
	}

	@Test
	void resolveDeepseekCaseInsensitive() {
		assertThat(ModelProvider.resolve("deepseek")).isEqualTo(ModelProvider.DEEPSEEK);
		assertThat(ModelProvider.resolve("DeepSeek")).isEqualTo(ModelProvider.DEEPSEEK);
	}

	@Test
	void resolveGlmCaseInsensitive() {
		assertThat(ModelProvider.resolve("glm")).isEqualTo(ModelProvider.GLM);
		assertThat(ModelProvider.resolve("GLM")).isEqualTo(ModelProvider.GLM);
	}

	@Test
	void resolveUnknownThrows() {
		assertThatThrownBy(() -> ModelProvider.resolve("openai"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("不支持的模型提供方");
	}

}
