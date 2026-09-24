package com.example.spring_ai.model;

/**
 * 支持的模型提供方。
 */
public enum ModelProvider {

	DEEPSEEK, GLM;

	/**
	 * 解析请求中的 provider 字符串：deepseek / glm，为空或未知值时的行为与旧逻辑保持一致。
	 */
	public static ModelProvider resolve(String provider) {
		if (provider == null || provider.isBlank() || "deepseek".equalsIgnoreCase(provider)) {
			return DEEPSEEK;
		}
		if ("glm".equalsIgnoreCase(provider)) {
			return GLM;
		}
		throw new IllegalArgumentException("不支持的模型提供方: " + provider);
	}

}
