package org.lovetropics.games.common.core.game.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;
import java.util.function.BiConsumer;

public class TranslationCollector {
	private final String prefix;
	private final Map<String, String> values = new Object2ObjectOpenHashMap<>();

	public TranslationCollector(String prefix) {
		this.prefix = prefix;
	}

	public MutableComponent add(String key, String value) {
		String fullKey = put(key, value);
		return Component.translatable(fullKey);
	}

	public Fun1 add1(String key, String value) {
		String fullKey = put(key, value);
		return arg -> Component.translatable(fullKey, arg);
	}

	public Fun2 add2(String key, String value) {
		String fullKey = put(key, value);
		return (a, b) -> Component.translatable(fullKey, a, b);
	}

	public Fun3 add3(String key, String value) {
		String fullKey = put(key, value);
		return (a, b, c) -> Component.translatable(fullKey, a, b, c);
	}

	public Fun4 add4(String key, String value) {
		String fullKey = put(key, value);
		return (a, b, c, d) -> Component.translatable(fullKey, a, b, c, d);
	}

	public Fun5 add5(String key, String value) {
		String fullKey = put(key, value);
		return (a, b, c, d, e) -> Component.translatable(fullKey, a, b, c, d, e);
	}

	private String put(String key, String value) {
		String fullKey = prefix + key;
		if (values.putIfAbsent(fullKey, value) != null) {
			throw new IllegalArgumentException("Duplicate translation for key: " + fullKey);
		}
		return fullKey;
	}

	public void forEach(BiConsumer<String, String> consumer) {
		values.forEach(consumer);
	}

	public interface Fun1 {
		MutableComponent apply(Object arg);

		default Fun1 withStyle(ChatFormatting color) {
			return a -> apply(a).withStyle(color);
		}
	}

	public interface Fun2 {
		MutableComponent apply(Object a, Object b);

		default Fun2 withStyle(ChatFormatting color) {
			return (a, b) -> apply(a, b).withStyle(color);
		}
	}

	public interface Fun3 {
		MutableComponent apply(Object a, Object b, Object c);

		default Fun3 withStyle(ChatFormatting color) {
			return (a, b, c) -> apply(a, b, c).withStyle(color);
		}
	}

	public interface Fun4 {
		MutableComponent apply(Object a, Object b, Object c, Object d);

		default Fun4 withStyle(ChatFormatting color) {
			return (a, b, c, d) -> apply(a, b, c, d).withStyle(color);
		}
	}

	public interface Fun5 {
		MutableComponent apply(Object a, Object b, Object c, Object d, Object e);

		default Fun5 withStyle(ChatFormatting color) {
			return (a, b, c, d, e) -> apply(a, b, c, d, e).withStyle(color);
		}
	}
}
