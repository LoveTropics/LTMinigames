package com.lovetropics.minigames.client.screen.flex;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public final class FlexSolver {
	private final Box screen;

	public FlexSolver(Box screen) {
		this.screen = screen;
	}

	public Results apply(Flex root) {
		Results results = new Results();

		FlexSolve rootSolve = results.flexSolve(root);
		rootSolve.outerSize = rootSolve.innerSize = screen.size();
		rootSolve.layout = layout(root, screen);

		updateInnerSizes(results, root, rootSolve);
		solveSizes(results, root, rootSolve);
		solveLayouts(results, root, rootSolve);

		return results;
	}

	private void updateInnerSizes(Results results, Flex flex, FlexSolve solve) {
		Flex.Length innerWidth = Flex.Length.ZERO;
		Flex.Length innerHeight = Flex.Length.ZERO;

		for (Flex child : flex.children) {
			FlexSolve childSolve = results.flexSolve(child);
			updateInnerSizes(results, child, childSolve);

			Flex.Length childOuterWidth = child.width.min.add(totalBorderX(child));
			Flex.Length childOuterHeight = child.height.min.add(totalBorderY(child));

			if (flex.axis == Axis.X) {
				innerWidth = innerWidth.add(childOuterWidth);
				innerHeight = Flex.Length.max(innerHeight, childOuterHeight);
			} else {
				innerWidth = Flex.Length.max(innerWidth, childOuterWidth);
				innerHeight = innerHeight.add(childOuterHeight);
			}
		}

		solve.innerWidth = innerWidth;
		solve.innerHeight = innerHeight;
	}

	private void solveSizes(Results results, Flex flex, FlexSolve solve) {
		float totalGrow = 0.0F;
		for (Flex child : flex.children) {
			FlexSolve childSolve = results.flexSolve(child);
			childSolve.innerSize = solveInnerSize(solve.innerSize, child, childSolve);
			childSolve.outerSize = outerSize(child, childSolve.innerSize);
			totalGrow += child.grow;
		}

		if (totalGrow > 0.0F) {
			solveGrow(results, flex, solve, totalGrow);
		}

		for (Flex child : flex.children) {
			solveSizes(results, child, results.flexSolve(child));
		}
	}

	private void solveGrow(Results results, Flex flex, FlexSolve solve, float totalGrow) {
		int remainingSize = remainingSizeForGrow(results, flex, solve);
		if (remainingSize <= 0) {
			return;
		}

		for (Flex child : flex.children) {
			if (child.grow <= 0.0F) {
				continue;
			}

			int growSize = Math.round(remainingSize * (child.grow / totalGrow));
			remainingSize -= growSize;
			totalGrow -= child.grow;

			FlexSolve childSolve = results.flexSolve(child);
			childSolve.applyGrow(flex.axis, growSize);
		}
	}

	private int remainingSizeForGrow(Results results, Flex flex, FlexSolve solve) {
		int remainingSize = solve.innerSize.along(flex.axis);
		for (Flex child : flex.children) {
			FlexSolve childSolve = results.flexSolve(child);
			remainingSize -= childSolve.outerSize.along(flex.axis);
		}
		return remainingSize;
	}

	private void solveLayouts(Results results, Flex flex, FlexSolve solve) {
		Axis axis = flex.axis;

		Box inner = solve.layout.content();
		Box.Interval innerMain = inner.along(axis);
		Box.Interval innerCross = inner.along(axis.cross());

		for (Flex child : flex.children) {
			FlexSolve childSolve = results.flexSolve(child);

			Box.Interval childMain = innerMain.applyMainAlign(child.alignMain, childSolve.outerSize.along(axis));
			Box.Interval childCross = innerCross.applyCrossAlign(child.alignCross, childSolve.outerSize.along(axis.cross()));

			childSolve.layout = layout(child, Box.combine(axis, childMain, childCross));

			innerMain = innerMain.subtract(child.alignMain, childMain.size());

			solveLayouts(results, child, childSolve);
		}
	}

	private Box.Size solveInnerSize(Box.Size parent, Flex flex, FlexSolve solve) {
		int minInnerWidth = flex.width.min.resolve(parent.width());
		int minInnerHeight = flex.height.min.resolve(parent.height());
		int maxInnerWidth = flex.width.max.resolve(parent.width());
		int maxInnerHeight = flex.height.max.resolve(parent.height());

		int innerWidth = solve.innerWidth.resolve(minInnerWidth);
		int innerHeight = solve.innerHeight.resolve(minInnerHeight);

		return new Box.Size(
				Mth.clamp(innerWidth, minInnerWidth, maxInnerWidth),
				Mth.clamp(innerHeight, minInnerHeight, maxInnerHeight)
		);
	}

	private static Box.Size outerSize(Flex flex, Box.Size size) {
		return size.grow(flex.margin).grow(flex.padding);
	}

	private static Layout layout(Flex flex, Box margin) {
		Box padding = margin.contract(flex.margin);
		Box content = padding.contract(flex.padding);
		return new Layout(content, padding, margin);
	}

	static int totalBorderX(Flex flex) {
		return flex.padding.borderX() + flex.margin.borderX();
	}

	static int totalBorderY(Flex flex) {
		return flex.padding.borderY() + flex.margin.borderY();
	}

	public static final class Results {
		private final Map<Flex, FlexSolve> flexSolves = new Reference2ObjectOpenHashMap<>();

		public Layout layout(Flex flex) {
			return Objects.requireNonNull(layoutOrNull(flex), "given flex has not been solved");
		}

		@Nullable
		private Layout layoutOrNull(Flex flex) {
			FlexSolve solve = flexSolves.get(flex);
			return solve != null ? solve.layout : null;
		}

		private FlexSolve flexSolve(Flex flex) {
			return flexSolves.computeIfAbsent(flex, f -> new FlexSolve());
		}
	}

	static final class FlexSolve {
		Flex.Length innerWidth = Flex.Length.ZERO;
		Flex.Length innerHeight = Flex.Length.ZERO;

		Box.Size outerSize;
		Box.Size innerSize;
		Layout layout;

		void applyGrow(Axis axis, int size) {
			outerSize = outerSize.grow(axis, size);
			innerSize = innerSize.grow(axis, size);
		}
	}
}
