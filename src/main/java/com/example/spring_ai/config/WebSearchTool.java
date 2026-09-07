package com.example.spring_ai.config;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WebSearchTool {
    @Tool(description = "search for baidu or bing search engine")
    public String searchWeb(@ToolParam(description = "") String query) {
        return "搜索结果" + query + "如下：";
    }
}
