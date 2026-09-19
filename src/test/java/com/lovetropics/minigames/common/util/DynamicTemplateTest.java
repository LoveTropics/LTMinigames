package com.lovetropics.minigames.common.util;

import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamicTemplateTest {
	private static void assertSubstitutionAndExtraction(DynamicTemplate template, Object parameters, Object result) {
		assertEquals(result, template.substitute(JavaOps.INSTANCE, parameters));
		assertEquals(parameters, template.extract(JavaOps.INSTANCE, result));
	}

	@Test
	public void substitutionInMap() {
		DynamicTemplate template = DynamicTemplate.parse(JavaOps.INSTANCE, Map.of(
				"foo", "$1",
				"bar", Map.of(
						"baz", "$2"
				)
		));
		assertSubstitutionAndExtraction(template,
				Map.of(
						"1", "hello",
						"2", "there"
				),
				Map.of(
						"foo", "hello",
						"bar", Map.of(
								"baz", "there"
						)
				)
		);
	}

	@Test
	public void missingParameterInMap() {
		DynamicTemplate template = DynamicTemplate.parse(JavaOps.INSTANCE,
				Map.of(
						"foo", "$1",
						"bar", "$2"
				)
		);
		assertSubstitutionAndExtraction(template,
				Map.of(
						"1", "hello"
				),
				Map.of(
						"foo", "hello"
				)
		);
	}

	@Test
	public void stringTemplate() {
		DynamicTemplate template = DynamicTemplate.parse(JavaOps.INSTANCE, Map.of(
				"with_prefix", Map.of(
						"$", Map.of(
								"path", "$1",
								"prefix", "prefix_"
						)
				),
				"with_suffix", Map.of(
						"$", Map.of(
								"path", "$1",
								"suffix", "_suffix"
						)
				)
		));
		assertSubstitutionAndExtraction(template,
				Map.of(
						"1", "value"
				),
				Map.of(
						"with_prefix", "prefix_value",
						"with_suffix", "value_suffix"
				)
		);
	}
}
