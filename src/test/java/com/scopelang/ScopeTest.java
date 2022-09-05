package com.scopelang;

import org.junit.*;

public class ScopeTest {
	@Test
	public void stringProcessing() {
		// "\t\n"
		String input = Utils.processLiteral("\"\\t\\n\"");

		Assert.assertFalse(input == "\t\n");
	}
}