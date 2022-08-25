package com.scopelang;

import org.junit.Assert;
import org.junit.Test;

public class ScopeTest {
	@Test
	public void stringProcessing() {
		// "\t\n"
		String input = Utils.processLiteral("\"\\t\\n\"");

		Assert.assertFalse(input == "\t\n");
	}
}