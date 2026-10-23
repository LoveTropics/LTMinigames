package org.lovetropics.games.client.game.debug;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ScrambleDebugScreenClientState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Messes with the lines of the debug screen, as set by [ScrambleDebugScreenClientState].
///
/// The debug screen is made of blocks of lines that belong together, separated by empty lines - e.g. the targeted block,
/// with its name and state under it. Blocks are moved around as a whole, so that each of them still makes sense on its own.
///
/// The screen is scrambled the same way for as long as it stays open, and may be scrambled differently every time it is
/// opened again.
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class DebugScreenScrambler {
	private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
	private static final Pattern KEY_END = Pattern.compile("[\\d:]");
	private static final long SIDE_SALT = 0x5DEECE66DL;
	private static final long GARBLE_SALT = 0x9E3779B97F4A7C15L;
	private static final long FAKE_SALT = 0xC2B2AE3D27D4EB4FL;

	private static final String[] DIRECTIONS = {"north", "south", "west", "east"};
	private static final String[] TOWARDS = {"negative Z", "positive Z", "negative X", "positive X"};
	private static final String[] BIOMES = {
			"plains", "desert", "forest", "taiga", "swamp", "badlands", "jungle", "savanna", "snowy_plains", "mushroom_fields",
			"deep_ocean", "nether_wastes", "soul_sand_valley", "warped_forest", "basalt_deltas", "the_end", "deep_dark", "cherry_grove"
	};

	private static final RandomSource RANDOM = RandomSource.create();

	private static long seed = RANDOM.nextLong();
	private static boolean wasOverlayVisible;

	private DebugScreenScrambler() {
	}

	/// Every time the player opens the debug screen, it may get scrambled a new way
	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		boolean overlayVisible = Minecraft.getInstance().debugEntries.isOverlayVisible();
		if (overlayVisible && !wasOverlayVisible) {
			ScrambleDebugScreenClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.SCRAMBLE_DEBUG_SCREEN);
			if (state != null && RANDOM.nextFloat() < state.rescrambleChance()) {
				seed = RANDOM.nextLong();
			}
		}
		wasOverlayVisible = overlayVisible;
	}

	/// Scrambles the lines of both sides of the debug screen in place
	public static void scramble(List<String> left, List<String> right) {
		ScrambleDebugScreenClientState state = ClientGameStateManager.getOrNull(GameClientStateTypes.SCRAMBLE_DEBUG_SCREEN);
		if (state == null) {
			return;
		}

		List<Block> blocks = new ArrayList<>();
		splitIntoBlocks(left, false, blocks);
		splitIntoBlocks(right, true, blocks);
		for (int i = 0; i < state.fakeLines(); i++) {
			RandomSource random = RandomSource.create(HashCommon.mix(seed + i * FAKE_SALT));
			blocks.add(new Block(List.of(createFakeLine(random)), random.nextBoolean(), blocks.size(), random.nextLong()));
		}

		// Every block is placed by its key rather than by where it is, so that blocks coming and going don't move the others
		Comparator<Block> order = state.shuffle()
				? Comparator.comparingLong(block -> HashCommon.mix(seed ^ block.key))
				: Comparator.comparingInt(Block::index);
		List<Block> sorted = blocks.stream()
				.sorted(order.thenComparingInt(Block::index))
				.toList();

		left.clear();
		right.clear();
		for (Block block : sorted) {
			boolean onRight = state.swapSides() ? (HashCommon.mix(seed ^ block.key ^ SIDE_SALT) & 1) == 1 : block.right;
			List<String> side = onRight ? right : left;
			for (String line : block.lines) {
				side.add(state.garbleChance() > 0.0f ? garble(line, state.garbleChance()) : line);
			}
			side.add("");
		}
	}

	private static void splitIntoBlocks(List<String> lines, boolean right, List<Block> blocks) {
		List<String> block = new ArrayList<>();
		for (String line : lines) {
			if (line == null || line.isEmpty()) {
				addBlock(block, right, blocks);
				block = new ArrayList<>();
			} else {
				block.add(line);
			}
		}
		addBlock(block, right, blocks);
	}

	private static void addBlock(List<String> lines, boolean right, List<Block> blocks) {
		if (!lines.isEmpty()) {
			blocks.add(new Block(lines, right, blocks.size(), getKey(lines.getFirst()).hashCode()));
		}
	}

	/// What a line is about, without the values that change all the time - e.g. `XYZ` or `Targeted Block`
	private static String getKey(String line) {
		Matcher matcher = KEY_END.matcher(line);
		return matcher.find() ? line.substring(0, matcher.start()) : line;
	}

	/// Replaces numbers with random ones that look just as believable. Each kind of line is garbled the same way for
	/// as long as the screen is open, rather than changing every frame.
	private static String garble(String line, float chance) {
		RandomSource random = RandomSource.create(HashCommon.mix(seed ^ getKey(line).hashCode() ^ GARBLE_SALT));
		Matcher matcher = NUMBER.matcher(line);
		StringBuilder garbled = new StringBuilder();
		while (matcher.find()) {
			String number = random.nextFloat() < chance ? randomDigits(matcher.group(), random) : matcher.group();
			matcher.appendReplacement(garbled, number);
		}
		matcher.appendTail(garbled);
		return garbled.toString();
	}

	private static String randomDigits(String number, RandomSource random) {
		StringBuilder digits = new StringBuilder(number.length());
		for (int i = 0; i < number.length(); i++) {
			char c = number.charAt(i);
			if (Character.isDigit(c)) {
				// No leading zeros, which would give it away
				boolean leading = i == 0 && number.length() > 1 && Character.isDigit(number.charAt(1));
				digits.append((char) ('0' + (leading ? 1 + random.nextInt(9) : random.nextInt(10))));
			} else {
				digits.append(c);
			}
		}
		return digits.toString();
	}

	/// Makes up a line that looks like it belongs on the debug screen, and sometimes one that doesn't but would be nice to have
	private static String createFakeLine(RandomSource random) {
		int x = random.nextIntBetweenInclusive(-3000, 3000);
		int y = random.nextIntBetweenInclusive(-60, 120);
		int z = random.nextIntBetweenInclusive(-3000, 3000);
		int direction = random.nextInt(DIRECTIONS.length);
		return switch (random.nextInt(7)) {
			case 0 -> String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", x + random.nextDouble(), y + random.nextDouble(), z + random.nextDouble());
			case 1 -> String.format(Locale.ROOT, "Block: %d %d %d [%d %d %d]", x, y, z, x & 15, y & 15, z & 15);
			case 2 -> String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]", x >> 4, y >> 4, z >> 4, (x >> 4) & 31, (z >> 4) & 31, x >> 9, z >> 9);
			case 3 -> String.format(Locale.ROOT, "Facing: %s (Towards %s) (%.1f / %.1f)", DIRECTIONS[direction], TOWARDS[direction], random.nextFloat() * 360.0f - 180.0f, random.nextFloat() * 180.0f - 90.0f);
			case 4 -> "Biome: minecraft:" + BIOMES[random.nextInt(BIOMES.length)];
			case 5 -> String.format(Locale.ROOT, "Client Light: %d (%d sky, %d block)", random.nextInt(16), random.nextInt(16), random.nextInt(16));
			default -> String.format(Locale.ROOT, "Nearest Stronghold: %d %d", x * 2, z * 2);
		};
	}

	/// Lines that belong together on the debug screen
	///
	/// @param right whether the block was on the right side of the screen
	/// @param index where the block was among all blocks
	/// @param key   what the block is about, which decides where it ends up
	private record Block(List<String> lines, boolean right, int index, long key) {
	}
}
