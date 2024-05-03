package com.rmendes.service.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface LLMAviation {
	
	
	@SystemMessage("You are an assistant that receives a collection of NTSB incident reports and will derive key information from it")
	@UserMessage("You will receive a vector database with the relevant information. Don't return anything.")
	TokenStream prepare();
	
	TokenStream chat(String message);

}
