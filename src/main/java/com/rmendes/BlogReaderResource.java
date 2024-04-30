package com.rmendes;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rmendes.crawler.BlogCrawler;
import com.rmendes.service.llm.LLMService;
import com.rmendes.utils.RequestSplitter;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class BlogReaderResource {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BlogReaderResource.class);
	
	private final LLMService llmService;
	
	private final BlogCrawler crawler;
	
	private final RequestSplitter splitter;
	
	private final StreamingChatLanguageModel languageModel;
	
	private static final String modelUrl = "http://localhost:11434";
	
	private static final String modelName = "llama2";
	
	@Inject
	public BlogReaderResource(BlogCrawler crawler, RequestSplitter splitter) {
		this.languageModel = OllamaStreamingChatModel.builder()
				.baseUrl(modelUrl)
				.modelName(modelName)
				.timeout(Duration.ofHours(1))
				.build();
		this.llmService = AiServices.builder(LLMService.class)
				.streamingChatLanguageModel(this.languageModel)
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
				.build();
		this.crawler = crawler;
		this.splitter = splitter;
	}
	
	 
	@Path("/read")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String read(String url) {
        // Read the HTML from the specified URL
        String content = crawler.crawl(url);

        LOGGER.info("\uD83D\uDD1C Preparing analysis of {}", url);

        // Prepare the model
        llmService.prepare();

        // Split the HTML into small pieces
        List<String> split = splitter.split(content);

        // Send each piece of HTML to the LLM
        for (int i = 0; i < split.size(); i++) {
            llmService.sendBody(split.get(i));
            LOGGER.info("\uD83E\uDDD0 Analyzing article... Part {} out of {}.", (i + 1), split.size());
        }

        LOGGER.info("\uD83D\uDCDD Preparing response...");

        // Ask the model to sum up the article
        TokenStream tokenStream = llmService.sumUp();
        CompletableFuture<Void> future = new CompletableFuture<>();
        tokenStream.onNext(System.out::print)
        .onComplete(x -> {
        	System.out.println();
        	future.complete(null);
        }).onError(Throwable::printStackTrace).start();	

        LOGGER.info("✅ Response for {} ready", url);

        // Return the result to the user
        return "";
    }
}
