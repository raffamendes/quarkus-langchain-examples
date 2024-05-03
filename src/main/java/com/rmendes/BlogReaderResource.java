package com.rmendes;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jboss.resteasy.reactive.RestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rmendes.crawler.BlogCrawler;
import com.rmendes.service.llm.LLMAviation;
import com.rmendes.service.llm.LLMService;
import com.rmendes.utils.RequestSplitter;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class BlogReaderResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlogReaderResource.class);

	private LLMService llmService;
	
	private LLMAviation llmAviation;

	@Inject
	private BlogCrawler crawler;

	@Inject
	private RequestSplitter splitter;

	private StreamingChatLanguageModel languageModel;

	private static final String modelUrl = "http://localhost:11434";

	private static final String modelName = "llama2";

	private void instatiateModelConnection() {
		System.out.println("Init-----");
		this.languageModel = OllamaStreamingChatModel.builder().baseUrl(modelUrl).modelName(modelName)
				.timeout(Duration.ofHours(1)).build();
		System.out.println(this.languageModel);
		this.llmService = AiServices.builder(LLMService.class).streamingChatLanguageModel(this.languageModel)
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10)).build();
	}

	@Path("/read")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	public String read(String url) {
		if (llmService == null) {
			instatiateModelConnection();
		}

		String content = crawler.crawl(url);

		LOGGER.info("\uD83D\uDD1C Preparing analysis of {}", url);
		
		llmService.prepare();

		List<String> split = splitter.split(content);

		for (int i = 0; i < split.size(); i++) {
			llmService.sendBody(split.get(i));
			LOGGER.info("\uD83E\uDDD0 Analyzing article... Part {} out of {}.", (i + 1), split.size());
		}

		LOGGER.info("\uD83D\uDCDD Preparing response...");

		TokenStream tokenStream = llmService.sumUp();
		CompletableFuture<Void> future = new CompletableFuture<>();
		tokenStream.onNext(System.out::print).onComplete(x -> {
			System.out.println(x.toString());
			future.complete(null);
		}).onError(Throwable::printStackTrace).start();

		LOGGER.info("✅ Response for {} ready", url);

		return "";
	}
	
	@Path("/aviation-incidents")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	public String getAviationIncidents(@RestForm String prompt) {
		List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/rmendes/KG-RAG-datasets/ntsb-aviation-incident-accident-reports/data/v1/docs");
		InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
		EmbeddingStoreIngestor.ingest(documents, store);
		this.llmAviation = AiServices.builder(LLMAviation.class)
				.streamingChatLanguageModel(OllamaStreamingChatModel.builder().baseUrl(modelUrl).modelName(modelName).timeout(Duration.ofHours(1)).build())
				.contentRetriever(EmbeddingStoreContentRetriever.from(store))
				.build();
		
		llmAviation.prepare();
		System.out.println(prompt);
		TokenStream tokenStream = llmAviation.chat(prompt);
		CompletableFuture<Void> future = new CompletableFuture<>();
		tokenStream.onNext(System.out::print).onComplete(x -> {
			System.out.println(x.toString());
			future.complete(null);
		}).onError(Throwable::printStackTrace).start();
		return "";
		
	}
}
