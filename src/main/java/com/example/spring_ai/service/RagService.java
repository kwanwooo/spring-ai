package com.example.spring_ai.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.spring_ai.model.ModelProvider;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RAG 服务：文本摄入（切分 → 向量化 → 入库持久化）与检索增强问答。
 */
@Service
public class RagService {

	private final SimpleVectorStore vectorStore;

	private final TokenTextSplitter splitter;

	private final File vectorStoreFile;

	private final QuestionAnswerAdvisor questionAnswerAdvisor;

	private final ChatService chatService;

	/** 已摄入文本的哈希，用于会话内去重（进程重启后失效）。 */
	private final Set<String> ingestedHashes = new HashSet<>();

	public RagService(SimpleVectorStore vectorStore, TokenTextSplitter splitter,
			@Value("${spring.ai.rag.vector-store-file:data/vectorstore.json}") String filePath,
			ChatService chatService) {
		this.vectorStore = vectorStore;
		this.splitter = splitter;
		this.vectorStoreFile = new File(filePath);
		this.chatService = chatService;
		this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
			.searchRequest(SearchRequest.builder().topK(4).similarityThreshold(0.5).build())
			.build();
	}

	/**
	 * 摄入一段文本：切分为分块后向量化入库，并持久化到本地文件。同步执行避免并发写文件竞态。
	 *
	 * @return 产生的分块数量；若该文本已摄入则返回 0
	 */
	public synchronized int ingest(String text, Map<String, Object> metadata) {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("text 不能为空");
		}
		String hash = sha256(text);
		if (!this.ingestedHashes.add(hash)) {
			return 0;
		}
		List<Document> chunks = this.splitter.split(new Document(text, metadata));
		this.vectorStore.add(chunks);
		this.vectorStore.save(this.vectorStoreFile);
		return chunks.size();
	}

	/**
	 * 检索增强问答：从向量库检索相关片段注入上下文后，再由指定模型作答。
	 */
	public String ask(ModelProvider provider, String conversationId, String message) {
		return this.chatService.ragChat(provider, conversationId, message, this.questionAnswerAdvisor);
	}

	private String sha256(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e) {
			return Integer.toHexString(text.hashCode());
		}
	}

}
