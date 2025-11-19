package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextMap;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TemplatedText(Component template) {
	public static final Codec<TemplatedText> CODEC = ComponentSerialization.CODEC.xmap(TemplatedText::new, TemplatedText::template);

	private static final Pattern PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");

	public Component apply(ContextMap context) {
		Map<String, Component> values = new Object2ObjectArrayMap<>();
		addValuesFromContext(context, values);
		return apply(values);
	}

	private static void addValuesFromContext(ContextMap context, Map<String, Component> values) {
		Optional.ofNullable(context.getOptional(GameActionContextKeys.PACKAGE_SENDER)).ifPresent(name ->
				values.put("sender", Component.literal(name))
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.KILLER)).ifPresent(player ->
				values.put("killer", player.getDisplayName())
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.KILLED)).ifPresent(player ->
				values.put("killed", player.getDisplayName())
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.TARGET)).ifPresent(entity ->
				values.put("target", entity.getDisplayName())
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.COUNT)).ifPresent(count ->
				values.put("count", Component.literal(String.valueOf(count)))
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.ITEM)).ifPresent(item ->
				values.put("item", item.getHoverName())
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.TEAM)).ifPresent(team ->
				values.put("team", team.config().styledName())
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.NAME)).ifPresent(name ->
				values.put("name", name)
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.WINNER)).ifPresent(name ->
				values.put("winner", name)
		);
		Optional.ofNullable(context.getOptional(GameActionContextKeys.CHANGE)).ifPresent(delta ->
				values.put("change", Component.literal(String.valueOf(delta)))
		);
	}

	public Component apply(Map<String, Component> values) {
		if (values.isEmpty()) {
			return template;
		}

		// TODO: Precompute this
		MutableComponent result = Component.literal("");
		template.visit((FormattedText.StyledContentConsumer<Unit>) (style, text) -> {
			int leftoverIndex = 0;
			Matcher matcher = PATTERN.matcher(text);
			while (matcher.find()) {
				String group = matcher.group();
				Component value = values.get(group.substring(1, group.length() - 1));
				if (value != null) {
					if (matcher.start() > leftoverIndex) {
						result.append(Component.literal(text.substring(leftoverIndex, matcher.start())).withStyle(style));
					}
					Style mergedStyle = value.getStyle().applyTo(style);
					result.append(value.copy().setStyle(mergedStyle));
					leftoverIndex = matcher.end();
				}
			}
			if (leftoverIndex < text.length()) {
				result.append(Component.literal(text.substring(leftoverIndex)).withStyle(style));
			}
			return Optional.empty();
		}, Style.EMPTY);
		return result;
	}
}
