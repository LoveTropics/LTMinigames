package com.lovetropics.minigames.client.screen.flex;

import java.util.ArrayList;
import java.util.List;

public final class Flex {
	LengthRange width = LengthRange.ANY;
	LengthRange height = LengthRange.ANY;

	Box padding = new Box(0, 0, 0, 0);
	Box margin = new Box(0, 0, 0, 0);

	Axis axis = Axis.Y;
	Align.Main alignMain = Align.Main.START;
	Align.Cross alignCross = Align.Cross.START;

	float grow = 0.0F;

	final List<Flex> children = new ArrayList<>();

	public Flex row() {
		return axis(Axis.X);
	}

	public Flex column() {
		return axis(Axis.Y);
	}

	public Flex axis(Axis axis) {
		this.axis = axis;
		return this;
	}

	public Flex size(int w, int h) {
		return size(w, h, Unit.PX);
	}

	public Flex size(float w, float h, Unit unit) {
		return width(w, unit).height(h, unit);
	}

	public Flex width(int w) {
		return width(w, Unit.PX);
	}

	public Flex width(float w, Unit unit) {
		width = LengthRange.of(unit.width(this, w));
		return this;
	}

	public Flex height(int h) {
		return height(h, Unit.PX);
	}

	public Flex height(float h, Unit unit) {
		height = LengthRange.of(unit.height(this, h));
		return this;
	}

	public Flex minSize(int w, int h) {
		return minSize(w, h, Unit.PX);
	}

	public Flex minSize(float w, float h, Unit unit) {
		return minWidth(w, unit).minHeight(h, unit);
	}

	public Flex minWidth(int w) {
		return minWidth(w, Unit.PX);
	}

	public Flex minWidth(float w, Unit unit) {
		width = width.min(unit.width(this, w));
		return this;
	}

	public Flex minHeight(int h) {
		return minHeight(h, Unit.PX);
	}

	public Flex minHeight(float h, Unit unit) {
		height = height.min(unit.height(this, h));
		return this;
	}

	public Flex maxSize(int w, int h) {
		return maxSize(w, h, Unit.PX);
	}

	public Flex maxSize(float w, float h, Unit unit) {
		return maxWidth(w, unit).maxHeight(h, unit);
	}

	public Flex maxWidth(int w) {
		return maxWidth(w, Unit.PX);
	}

	public Flex maxWidth(float w, Unit unit) {
		width = width.max(unit.width(this, w));
		return this;
	}

	public Flex maxHeight(int h) {
		return maxHeight(h, Unit.PX);
	}

	public Flex maxHeight(float h, Unit unit) {
		height = height.max(unit.height(this, h));
		return this;
	}

	public Flex padding(int left, int top, int right, int bottom) {
		padding = new Box(left, top, right, bottom);
		return this;
	}

	public Flex padding(int x, int y) {
		return padding(x, y, x, y);
	}

	public Flex padding(int padding) {
		return padding(padding, padding);
	}

	public Flex paddingLeft(int padding) {
		this.padding = this.padding.left(padding);
		return this;
	}

	public Flex paddingTop(int padding) {
		this.padding = this.padding.top(padding);
		return this;
	}

	public Flex paddingRight(int padding) {
		this.padding = this.padding.right(padding);
		return this;
	}

	public Flex paddingBottom(int padding) {
		this.padding = this.padding.bottom(padding);
		return this;
	}

	public Flex margin(int left, int top, int right, int bottom) {
		margin = new Box(left, top, right, bottom);
		return this;
	}

	public Flex margin(int x, int y) {
		return margin(x, y, x, y);
	}

	public Flex margin(int margin) {
		return margin(margin, margin);
	}

	public Flex marginLeft(int margin) {
		this.margin = this.margin.left(margin);
		return this;
	}

	public Flex marginTop(int margin) {
		this.margin = this.margin.top(margin);
		return this;
	}

	public Flex marginRight(int margin) {
		this.margin = this.margin.right(margin);
		return this;
	}

	public Flex marginBottom(int margin) {
		this.margin = this.margin.bottom(margin);
		return this;
	}

	public Flex alignMain(Align.Main align) {
		alignMain = align;
		return this;
	}

	public Flex alignCross(Align.Cross align) {
		alignCross = align;
		return this;
	}

	public Flex grow(float grow) {
		this.grow = grow;
		return this;
	}

	public Flex child() {
		Flex child = new Flex();
		children.add(child);
		return child;
	}

	interface Length {
		Length ZERO = value(0);
		Length FILL = parent -> parent;

		static Length value(int value) {
			return parent -> value;
		}

		static Length max(Length left, Length right) {
			return parent -> {
				int resolvedLeft = left.resolve(parent);
				int resolvedRight = right.resolve(parent);
				return Math.max(resolvedLeft, resolvedRight);
			};
		}

		int resolve(int parent);

		default Length add(Length amount) {
			return parent -> {
				int resolvedAmount = amount.resolve(parent);
				return resolve(parent) + resolvedAmount;
			};
		}

		default Length add(int amount) {
			return add(value(amount));
		}
	}

	static final class LengthRange {
		static final LengthRange ANY = new LengthRange(Length.value(0), Length.FILL);

		final Length min;
		final Length max;

		LengthRange(Length min, Length max) {
			this.min = min;
			this.max = max;
		}

		static LengthRange of(Length size) {
			return new LengthRange(size, size);
		}

		LengthRange add(Length amount) {
			return new LengthRange(min.add(amount), max.add(amount));
		}

		LengthRange add(int amount) {
			return add(Length.value(amount));
		}

		LengthRange min(Length min) {
			return new LengthRange(min, max);
		}

		LengthRange max(Length max) {
			return new LengthRange(min, max);
		}
	}

	public interface Unit {
		Unit PX = (flex, axis, value) -> Length.value(Math.round(value));
		Unit CONTENT_PERCENT = (flex, axis, percent) -> parent -> {
			return Math.round(percent * parent);
		};
		Unit BORDER_PERCENT = (flex, axis, percent) -> parent -> {
			int padding = flex.padding.borderAlong(axis) + flex.margin.borderAlong(axis);
			return Math.round(percent * parent) - padding;
		};
		Unit PERCENT = BORDER_PERCENT;

		Length create(Flex flex, Axis axis, float value);

		default Length width(Flex flex, float width) {
			return create(flex, Axis.X, width);
		}

		default Length height(Flex flex, float height) {
			return create(flex, Axis.Y, height);
		}
	}
}
