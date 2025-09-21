package com.lovetropics.minigames.client.screen.flex;

import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

public final class Box {

	private final int left;
	private final int top;
	private final int right;
	private final int bottom;

	public Box() {
		this(0, 0, 0, 0);
	}

	public Box(int left, int top, int right, int bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	public Box(Size size) {
		this(0, 0, size.width(), size.height());
	}

	public Box(Screen screen) {
		this(0, 0, screen.width, screen.height);
	}

	public Box grow(int amount) {
		return grow(amount, amount);
	}

	public Box grow(int x, int y) {
		return grow(x, y, x, y);
	}

	public Box grow(int left, int top, int right, int bottom) {
		return new Box(
				this.left - left,
				this.top - top,
				this.right + right,
				this.bottom + bottom
		);
	}

	public Box contract(Box border) {
		return grow(-border.left(), -border.top(), -border.right(), -border.bottom());
	}

	public int left() {
		return left;
	}

	public int top() {
		return top;
	}

	public int right() {
		return right;
	}

	public int bottom() {
		return bottom;
	}

	public int width() {
		return right - left;
	}

	public int height() {
		return bottom - top;
	}

	public Size size() {
		return new Size(width(), height());
	}

	public int centerX() {
		return (left + right) / 2;
	}

	public int centerY() {
		return (top + bottom) / 2;
	}

	public int borderX() {
		return left + right;
	}

	public int borderY() {
		return top + bottom;
	}

	public Box left(int left) {
		return new Box(left, top, right, bottom);
	}

	public Box top(int top) {
		return new Box(left, top, right, bottom);
	}

	public Box right(int right) {
		return new Box(left, top, right, bottom);
	}

	public Box bottom(int bottom) {
		return new Box(left, top, right, bottom);
	}

	public Box union(Box other) {
		return new Box(Math.min(left, other.left), Math.min(top, other.top), Math.max(right, other.right), Math.max(bottom, other.bottom));
	}

	public Box intersect(Box other) {
		Box intersect = new Box(Math.max(left, other.left), Math.max(top, other.top), Math.min(right, other.right), Math.min(bottom, other.bottom));
		if (intersect.width() <= 0 || intersect.height() <= 0) {
			return new Box();
		}
		return intersect;
	}

	public Box withDimensions(int width, int height) {
		return new Box(left, top, width >= 0 ? left + width : right, height >= 0 ? top + height : bottom);
	}

	public Box shift(int x, int y) {
		return new Box(left + x, top + y, right + x, bottom + y);
	}

	static Box combine(Axis mainAxis, Interval main, Interval cross) {
		if (mainAxis == Axis.X) {
			return new Box(main.start, cross.start, main.end, cross.end);
		} else {
			return new Box(cross.start, main.start, cross.end, main.end);
		}
	}

	int borderAlong(Axis axis) {
		return axis == Axis.X ? borderX() : borderY();
	}

	Interval along(Axis axis) {
		return axis == Axis.X ? new Interval(left, right) : new Interval(top, bottom);
	}

	public boolean contains(double x, double y) {
		return x >= left && y >= top && x < right && y < bottom;
	}

	public boolean intersects(Box other) {
		return right() > other.left() && bottom() > other.top() && left() < other.right() && top() < other.bottom();
	}

	@Override
	public String toString() {
		return "Box [" + left + "," + top + " -> " + right + "," + bottom + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(bottom, left, right, top);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Box other = (Box) obj;
		return bottom == other.bottom && left == other.left && right == other.right && top == other.top;
	}

	static final class Size {
		private final int width;
		private final int height;

		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public Size grow(int amount) {
			return grow(amount, amount);
		}

		public Size grow(int x, int y) {
			return new Size(width + x, height + y);
		}

		public Size grow(Axis axis, int amount) {
			return axis == Axis.X ? grow(amount, 0) : grow(0, amount);
		}

		public Size grow(Box border) {
			return grow(border.borderX(), border.borderY());
		}

		public int width() {
			return width;
		}

		public int height() {
			return height;
		}

		public int along(Axis axis) {
			return switch (axis) {
				case X -> width;
				case Y -> height;
			};
		}
	}

	static final class Interval {
		final int start;
		final int end;

		Interval(int start, int end) {
			this.start = start;
			this.end = end;
		}

		Interval subtract(Align.Main align, int size) {
			return switch (align) {
				case START -> new Interval(start + size, end);
				case END -> new Interval(start, end - size);
			};
		}

		int size() {
			return end - start;
		}

		Interval applyMainAlign(Align.Main align, int size) {
			return switch (align) {
				case START -> new Interval(start, start + size);
				case END -> new Interval(end - size, end);
			};
		}

		Interval applyCrossAlign(Align.Cross align, int size) {
			return switch (align) {
				case START -> new Interval(start, start + size);
				case CENTER -> {
					int start = (this.start + end - size) / 2;
					yield new Interval(start, start + size);
				}
				case END -> new Interval(end - size, end);
			};
		}
	}
}
