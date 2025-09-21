package com.lovetropics.minigames.client.lobby.screen.game_config;

import com.lovetropics.minigames.client.screen.LayoutGui;
import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.client.screen.flex.Layout;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.SimpleConfigData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Collections;
import java.util.List;

public abstract class SimpleConfigWidget extends LayoutGui implements IConfigWidget {

	protected final SimpleConfigData config;
	private final AbstractWidget control;

	public static SimpleConfigWidget from(LayoutTree ltree, SimpleConfigData data) {
		return switch (data.type()) {
			case BOOLEAN -> new BooleanConfigWidget(ltree, data);
			case NUMBER -> new NumericConfigWidget(ltree, data);
			case STRING -> new StringConfigWidget(ltree, data);
			case ENUM -> new EnumConfigWidget(ltree, data);
			default ->
					throw new IllegalArgumentException("Invalid config type " + data.type() + " for simple config widget");
		};
	}

	protected SimpleConfigWidget(LayoutTree ltree, SimpleConfigData config) {
		super();
		this.config = config;
		control = createControl(ltree.definiteChild(-1, getHeight()).pop());
		mainLayout = ltree.pop();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return Collections.singletonList(control);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		control.render(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	protected abstract AbstractWidget createControl(Layout ltree);

	@Override
	public int getHeight() {
		return 20;
	}

	private static final class BooleanConfigWidget extends SimpleConfigWidget {

		BooleanConfigWidget(LayoutTree ltree, SimpleConfigData config) {
			super(ltree, config);
		}

		@Override
		protected AbstractWidget createControl(Layout mainLayout) {
			// TODO communicate changes to config object
			return new BooleanButton(mainLayout, (Boolean) config.value());
		}
	}

	private static final class NumericConfigWidget extends SimpleConfigWidget {

		NumericConfigWidget(LayoutTree ltree, SimpleConfigData config) {
			super(ltree, config);
		}

		@Override
		protected AbstractWidget createControl(Layout mainLayout) {
			// TODO communicate changes to config object
			return Util.make(new EditBox(Minecraft.getInstance().font, mainLayout.background().left(), mainLayout.background().top(), mainLayout.background().width(), mainLayout.background().height(), Component.literal("")), w -> {
				w.setValue(config.value().toString());
				w.setFilter(NumberUtils::isCreatable);
			});
		}
	}

	private static final class StringConfigWidget extends SimpleConfigWidget {

		StringConfigWidget(LayoutTree ltree, SimpleConfigData config) {
			super(ltree, config);
		}

		@Override
		protected AbstractWidget createControl(Layout mainLayout) {
			// TODO communicate changes to config object
			return Util.make(new EditBox(Minecraft.getInstance().font, mainLayout.background().left(), mainLayout.background().top(), mainLayout.background().width(), mainLayout.background().height(), CommonComponents.EMPTY), w -> {
				w.setValue(config.value().toString());
			});
		}
	}

	private static final class EnumConfigWidget extends SimpleConfigWidget {

		EnumConfigWidget(LayoutTree ltree, SimpleConfigData config) {
			super(ltree, config);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		protected AbstractWidget createControl(Layout mainLayout) {
			// TODO communicate changes to config object
			return createButton(mainLayout, (Enum) config.value());
		}

		private <E extends Enum<E>> EnumButton<E> createButton(Layout layout, E def) {
			return new EnumButton<>(layout, def);
		}
	}
}
