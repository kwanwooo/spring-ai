package com.example.spring_ai.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 联网搜索工具：调用 DuckDuckGo Instant Answer API（免费、无需 key）返回摘要信息。
 */
@Component
public class WebSearchTool {

	private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

	private final RestClient restClient = RestClient.builder().baseUrl("https://api.duckduckgo.com").build();

	@Tool(description = "联网搜索，返回与查询相关的摘要信息；在需要最新信息或不确定的内容时使用")
	public String searchWeb(@ToolParam(description = "要搜索的关键词") String query) {
		try {
			Map<?, ?> resp = this.restClient.get()
				.uri(uri -> uri.path("/")
					.queryParam("q", query)
					.queryParam("format", "json")
					.queryParam("no_html", 1)
					.queryParam("skip_disambig", 1)
					.build())
				.retrieve()
				.body(Map.class);

			if (resp == null) {
				return "未找到与「" + query + "」相关的信息";
			}

			StringBuilder sb = new StringBuilder();
			Object abstractText = resp.get("AbstractText");
			Object abstractUrl = resp.get("AbstractURL");
			if (abstractText != null && !abstractText.toString().isBlank()) {
				sb.append(abstractText);
				if (abstractUrl != null && !abstractUrl.toString().isBlank()) {
					sb.append("（来源: ").append(abstractUrl).append("）");
				}
			}
			Object related = resp.get("RelatedTopics");
			if (related instanceof List<?> list) {
				int count = 0;
				for (Object item : list) {
					if (item instanceof Map<?, ?> topic) {
						Object text = topic.get("Text");
						Object url = topic.get("FirstURL");
						if (text != null && !text.toString().isBlank()) {
							sb.append("\n- ").append(text);
							if (url != null) {
								sb.append("（").append(url).append("）");
							}
							if (++count >= 3) {
								break;
							}
						}
					}
				}
			}
			return sb.length() == 0 ? "未找到与「" + query + "」相关的信息" : sb.toString();
		}
		catch (Exception e) {
			log.warn("联网搜索失败: {}", e.getMessage());
			return "联网搜索暂时不可用: " + e.getMessage();
		}
	}

}
