package com.example.spring_ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import com.example.spring_ai.config.PricingProperties.ModelPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link PricingProperties#cost(String, int, int)} 的成本核算单元测试。
 */
class PricingPropertiesTest {

	private PricingProperties pricing;

	@BeforeEach
	void setUp() {
		this.pricing = new PricingProperties();
		ModelPrice price = new ModelPrice();
		price.setInput(2.0);
		price.setOutput(8.0);
		this.pricing.setModels(Map.of("deepseek-chat", price));
	}

	@Test
	void costComputesFormula() {
		// (2.0 * 100 + 8.0 * 50) / 1_000_000 = 0.0006
		assertThat(this.pricing.cost("deepseek-chat", 100, 50)).isEqualTo(0.0006, within(1e-9));
	}

	@Test
	void costIsZeroForUnconfiguredModel() {
		assertThat(this.pricing.cost("unknown-model", 100, 50)).isZero();
	}

	@Test
	void costIsZeroForZeroTokens() {
		assertThat(this.pricing.cost("deepseek-chat", 0, 0)).isZero();
	}

}
