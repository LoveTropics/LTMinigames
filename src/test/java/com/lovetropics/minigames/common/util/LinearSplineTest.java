package com.lovetropics.minigames.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearSplineTest {
	@Test
	public void singlePoint() {
		LinearSpline spline = LinearSpline.builder()
				.point(0.0f, 1.0f)
				.build();

		assertEquals(1.0f, spline.get(0.0f));
		assertEquals(1.0f, spline.get(1.0f));
		assertEquals(1.0f, spline.get(-1.0f));
	}

	@Test
	public void twoPoints() {
		LinearSpline spline = LinearSpline.builder()
				.point(0.0f, 0.0f)
				.point(1.0f, 2.0f)
				.build();

		assertEquals(0.0f, spline.get(0.0f));
		assertEquals(0.5f, spline.get(0.25f));
		assertEquals(1.0f, spline.get(0.5f));
		assertEquals(1.5f, spline.get(0.75f));
		assertEquals(2.0f, spline.get(2.0f));

		assertEquals(0.0f, spline.get(-10.0f));
		assertEquals(2.0f, spline.get(10.0f));
	}

	@Test
	public void threePoints() {
		LinearSpline spline = LinearSpline.builder()
				.point(0.0f, 0.0f)
				.point(2.0f, -1.0f)
				.point(1.0f, 2.0f)
				.build();

		assertEquals(0.0f, spline.get(0.0f));
		assertEquals(1.0f, spline.get(0.5f));
		assertEquals(2.0f, spline.get(1.0f));
		assertEquals(0.5f, spline.get(1.5f));
		assertEquals(-1.0f, spline.get(2.0f));
	}
}
