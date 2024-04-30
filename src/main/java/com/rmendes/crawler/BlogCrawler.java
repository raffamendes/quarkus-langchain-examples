package com.rmendes.crawler;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BlogCrawler {
	
	public String crawl(String url) {
		Document doc;
		try {
			doc = Jsoup.connect(url).get();
		}catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return doc.body().getElementsByClass("rhdc-blog-post--body").first().html();
	}

}
