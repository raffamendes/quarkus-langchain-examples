package com.rmendes.service.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import io.quarkus.runtime.annotations.RegisterForReflection;



public interface LLMService {

	@SystemMessage("You are an assistant that receives the body of an HTML page and sum up the article in that page. Add key takeaways to the end of the sum up.")
	@UserMessage("""
			    The body will be sent in parts in the next requests. Don't return anything.
			""")
	TokenStream prepare();

	@UserMessage("""
			    Here's the next part of the body page:
			    ```html
			    {html}
			    ```
			    Wait for the next parts. Don't answer anything else.
			""")
	TokenStream sendBody(String html);

	@UserMessage("""
			    That's it. You can sum up the article and add key takeaways to the end of the sum up.
			""")
	TokenStream sumUp();

}
