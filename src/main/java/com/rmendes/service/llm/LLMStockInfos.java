package com.rmendes.service.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface LLMStockInfos {
	
	
	@SystemMessage("You are an assistant that receives a information about a company financial performance and will answer key questions regarding it")
	@UserMessage("You will receive additional information. Don't return anything.")
	TokenStream prepare();
	
	
	TokenStream chat(String message);

}
