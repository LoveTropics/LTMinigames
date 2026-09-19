package com.lovetropics.minigames.common.util;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DynamicTemplate {
	private final Node node;
	private final Set<ParameterPath> parameters;

	private DynamicTemplate(Node node, Set<ParameterPath> parameters) {
		this.node = node;
		this.parameters = parameters;
	}

	public static <T> DynamicTemplate parse(DynamicOps<T> ops, T template) {
		Node node = parseNode(ops, template);
		Set<ParameterPath> parameters = node.parameters().collect(Collectors.toSet());
		return new DynamicTemplate(node, parameters);
	}

	private static <T> Node parseNode(DynamicOps<T> ops, T value) {
		Optional<ParameterPath> reference = ops.getStringValue(value).result().map(DynamicTemplate::parseParameterReference);
		if (reference.isPresent()) {
			return new Substituted(reference.get());
		}

		Static<T> staticNode = new Static<>(new Dynamic<>(ops, value));

		Optional<MapLike<T>> map = ops.getMap(value).result();
		if (map.isPresent()) {
			T stringTemplateValue = map.get().get("$");
			if (stringTemplateValue != null) {
				StringTemplate stringTemplate = parseStringTemplate(ops, stringTemplateValue);
				if (stringTemplate != null) {
					return stringTemplate;
				}
			}

			MapNode<T> node = new MapNode<>(ops, map.get().entries().collect(Collectors.toMap(
					Pair::getFirst,
					entry -> parseNode(ops, entry.getSecond())
			)));
			return node.hasParameters() ? node : staticNode;
		}

		Optional<Stream<T>> list = ops.getStream(value).result();
		if (list.isPresent()) {
			ListNode node = new ListNode(list.get().map(entry -> parseNode(ops, entry)).toList());
			return node.hasParameters() ? node : staticNode;
		}

		return staticNode;
	}

	private static <T> @Nullable StringTemplate parseStringTemplate(DynamicOps<T> ops, T value) {
		ParameterPath path = ops.get(value, "path").flatMap(ops::getStringValue).result()
				.map(DynamicTemplate::parseParameterReference)
				.orElse(null);
		if (path == null) {
			return null;
		}
		String prefix = ops.get(value, "prefix").flatMap(ops::getStringValue).result().orElse("");
		String suffix = ops.get(value, "suffix").flatMap(ops::getStringValue).result().orElse("");
		return new StringTemplate(prefix, path, suffix);
	}

	private static @Nullable ParameterPath parseParameterReference(String key) {
		if (key.startsWith("$")) {
			return new ParameterPath(key.substring(1).split("\\."));
		} else {
			return null;
		}
	}

	public <U> U substitute(DynamicOps<U> ops, U parameters) {
		Optional<MapLike<U>> map = ops.getMap(parameters).result();
		if (map.isPresent()) {
			return substituteMap(ops, map.get());
		}
		return substituteWithResolver(ops, path -> null);
	}

	public <U> U substituteMap(DynamicOps<U> ops, MapLike<U> parameters) {
		return substituteWithResolver(ops, path -> resolveParameter(ops, parameters, path));
	}

	private <U> U substituteWithResolver(DynamicOps<U> ops, Function<ParameterPath, U> resolver) {
		U result = node.substitute(ops, resolver);
		return Objects.requireNonNullElseGet(result, ops::emptyMap);
	}

	private static <T> @Nullable T resolveParameter(DynamicOps<T> ops, MapLike<T> root, ParameterPath path) {
		if (path.segments.length == 0) {
			return null;
		}
		T value = null;
		MapLike<T> map = root;
		for (String key : path.segments) {
			if (map == null) {
				return null;
			}
			value = map.get(key);
			map = value != null ? ops.getMap(value).result().orElse(null) : null;
		}
		return value;
	}

	public Set<ParameterPath> parameters() {
		return parameters;
	}

	public <T> T extract(DynamicOps<T> ops, T substituted) {
		if (parameters.isEmpty()) {
			return ops.emptyMap();
		}

		Map<ParameterPath, T> values = new HashMap<>();
		node.extract(ops, substituted, values::put);

		T result = ops.emptyMap();
		for (Map.Entry<ParameterPath, T> entry : values.entrySet()) {
			ParameterPath path = entry.getKey();

			T map = result;

			List<T> stack = new ArrayList<>(path.segments.length);
			stack.add(map);
			for (int i = 0; i < path.segments.length - 1; i++) {
				map = ops.get(map, path.segments[i]).result().orElseGet(ops::emptyMap);
				stack.add(map);
			}

			T value = entry.getValue();
			for (int i = path.segments.length - 1; i >= 0; i--) {
				value = ops.set(stack.get(i), path.segments[i], value);
			}
			result = value;
		}

		return result;
	}

	private sealed interface Node {
		<U> @Nullable U substitute(DynamicOps<U> ops, Function<ParameterPath, @Nullable U> resolver);

		<U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer);

		Stream<ParameterPath> parameters();

		default boolean hasParameters() {
			return parameters().findAny().isPresent();
		}
	}

	private record MapNode<T>(DynamicOps<T> ops, Map<T, Node> nodes) implements Node {
		@Override
		public <U> U substitute(DynamicOps<U> ops, Function<ParameterPath, U> resolver) {
			return ops.createMap(nodes.entrySet().stream()
					.map(entry -> {
						U substituted = entry.getValue().substitute(ops, resolver);
						if (substituted != null) {
							U key = Dynamic.convert(this.ops, ops, entry.getKey());
							return Map.entry(key, substituted);
						}
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
			);
		}

		@Override
		public <U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer) {
			ops.getMap(value).result().ifPresent(map -> map.entries().forEach(entry -> {
				T key = Dynamic.convert(ops, this.ops, entry.getFirst());
				Node node = nodes.get(key);
				if (node != null) {
					node.extract(ops, entry.getSecond(), consumer);
				}
			}));
		}

		@Override
		public Stream<ParameterPath> parameters() {
			return nodes.values().stream().flatMap(Node::parameters);
		}
	}

	private record ListNode(List<Node> nodes) implements Node {
		@Override
		public <U> U substitute(DynamicOps<U> ops, Function<ParameterPath, U> resolver) {
			return ops.createList(nodes.stream().map(node -> node.substitute(ops, resolver)).filter(Objects::nonNull));
		}

		@Override
		public <U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer) {
			ops.getStream(value).result().ifPresent(elements ->
					Streams.zip(elements, nodes.stream(), Pair::of)
							.forEach(pair -> pair.getSecond().extract(ops, pair.getFirst(), consumer))
			);
		}

		@Override
		public Stream<ParameterPath> parameters() {
			return nodes.stream().flatMap(Node::parameters);
		}
	}

	private record Substituted(ParameterPath path) implements Node {
		@Override
		public <U> @Nullable U substitute(DynamicOps<U> ops, Function<ParameterPath, @Nullable U> resolver) {
			return resolver.apply(path);
		}

		@Override
		public <U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer) {
			consumer.accept(path, value);
		}

		@Override
		public Stream<ParameterPath> parameters() {
			return Stream.of(path);
		}
	}

	private record StringTemplate(String prefix, ParameterPath path, String suffix) implements Node {
		@Override
		public <U> @Nullable U substitute(DynamicOps<U> ops, Function<ParameterPath, @Nullable U> resolver) {
			U resolved = resolver.apply(path);
			if (resolved != null) {
				String string = ops.getStringValue(resolved).result().orElse(null);
				if (string != null) {
					return ops.createString(prefix + string + suffix);
				}
			}
			return null;
		}

		@Override
		public <U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer) {
			String string = ops.getStringValue(value).result().orElse(null);
			if (string != null && string.startsWith(prefix) && string.endsWith(suffix)) {
				String extracted = string.substring(prefix.length(), string.length() - suffix.length());
				consumer.accept(path, ops.createString(extracted));
			}
		}

		@Override
		public Stream<ParameterPath> parameters() {
			return Stream.of(path);
		}
	}

	private record Static<T>(Dynamic<T> dynamic) implements Node {
		@Override
		public <U> U substitute(DynamicOps<U> ops, Function<ParameterPath, U> resolver) {
			return dynamic.convert(ops).getValue();
		}

		@Override
		public <U> void extract(DynamicOps<U> ops, U value, BiConsumer<ParameterPath, U> consumer) {
		}

		@Override
		public Stream<ParameterPath> parameters() {
			return Stream.empty();
		}
	}

	public record ParameterPath(String[] segments) {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof ParameterPath parameter && Arrays.equals(segments, parameter.segments);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(segments);
		}

		@Override
		public String toString() {
			return String.join(".", segments);
		}
	}
}
