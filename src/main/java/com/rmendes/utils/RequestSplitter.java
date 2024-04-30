package com.rmendes.utils;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequestSplitter {

	private static final int MAX_CHARS = 2000;

	public List<String> split(String text) {
		List<String> pieces = new ArrayList<>();
		if (text != null && !text.isEmpty() && MAX_CHARS > 0) {
			int length = text.length();

			if (length <= MAX_CHARS) {
				return List.of(text);
			}

			int startIndex = 0;
			int endIndex = MAX_CHARS;

			while (startIndex < length) {
				String piece = text.substring(startIndex, endIndex);
				pieces.add(piece);
				startIndex = endIndex;
				endIndex = Math.min(startIndex + MAX_CHARS, length);
			}
		}

		return pieces;
	}
}
