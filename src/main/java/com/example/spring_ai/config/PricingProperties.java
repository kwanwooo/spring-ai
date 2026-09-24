package com.example.spring_ai.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型成本单价配置，按模型名匹配，单位为「元 / 百万 token」。
 *
 * <p>配置示例（application.properties）：
 * <pre>
 * spring.ai.pricing.models.deepseek-chat.input=2.0
 * spring.ai.pricing.models.deepseek-chat.output=8.0
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "spring.ai.pricing")
public class PricingProperties {

	private static final Logger log = LoggerFactory.getLogger(PricingProperties.class);

	private final Set<String> warnedModels = ConcurrentHashMap.newKeySet();

	private Map<String, ModelPrice> models = new HashMap<>();

	public Map<String, ModelPrice> getModels() {
		return this.models;
	}

	public void setModels(Map<String, ModelPrice> models) {
		this.models = models;
	}

	/**
	 * 根据模型名与 token 数计算成本（元）。未配置单价的模型按 0 计，并警告一次。
	 */
	public double cost(String model, int promptTokens, int completionTokens) {
		ModelPrice price = this.models.get(model);
		if (price == null) {
			if (this.warnedModels.add(model)) {
				log.warn("未配置模型 {} 的单价，成本按 0 计算", model);
			}
			return 0.0;
		}
		return price.getInput() * promptTokens / 1_000_000.0 + price.getOutput() * completionTokens / 1_000_000.0;
	}

	public static class ModelPrice {

		private double input;

		private double output;

		public double getInput() {
			return this.input;
		}

		public void setInput(double input) {
			this.input = input;
		}

		public double getOutput() {
			return this.output;
		}

		public void setOutput(double output) {
			this.output = output;
		}

	}

}
