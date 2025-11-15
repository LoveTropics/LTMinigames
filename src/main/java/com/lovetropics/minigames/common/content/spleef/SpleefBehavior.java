package com.lovetropics.minigames.common.content.spleef;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GlobalGameWidgets;
import com.lovetropics.minigames.common.util.world.BlockPlacer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpleefBehavior implements IGameBehavior {

	private IGamePhase game;

	public static final MapCodec<SpleefBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("forced_progression_seconds", 17).forGetter(c -> c.forcedProgressionSeconds),
			Codec.INT.optionalFieldOf("floors", 0).forGetter(c -> c.floors),
			Codec.INT.optionalFieldOf("break_interval", 40).forGetter(c -> c.breakInterval),
			Codec.INT.optionalFieldOf("break_count", 6).forGetter(c -> c.breakCount),
			BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("floor_material", Blocks.OBSIDIAN).forGetter(c -> c.floorMaterial),
			BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("floor_breaking_material", Blocks.PURPLE_CONCRETE).forGetter(c -> c.floorBreakingMaterial),
			Codec.STRING.optionalFieldOf("flavor_text", "volcano").forGetter(c -> c.flavourText)
	).apply(i, SpleefBehavior::new));

	private final int forcedProgressionSeconds;
	private final int breakInterval;
	private final int breakCount;
	private final int floors;
	private final String flavourText;
	private GlobalGameWidgets widgets;

	private GameBossBar bossBar;

	private final BlockBox[] floorRegions;

	// Eliminate players if they enter this area.
	private BlockBox deathRegion;

	private final Block floorMaterial;
	private final Block floorBreakingMaterial;

	/**
	 * To make it easier for mapmakers, the levels will start at 1 but in code it'll be 0.
	 */
	private int currentFloor = 0;

	private int progressionTimer = 0;

	private final Style DARK_YELLOW = Style.EMPTY.withColor(TextColor.fromRgb(0x77A12F));

	// If people jump into the lava now, don't show the eliminated screen
	private boolean gameOver = false;

	// Handy for checking who won if multiple people were eliminated at the same time.
	private List<ServerPlayer> lastTickPlayers = Collections.emptyList();

	private boolean blockSinglePlayerWin = false;

	public SpleefBehavior(int forcedProgressionSeconds, int floors, int breakInterval, int breakCount, Block floorMaterial, Block floorBreakingMaterial, String flavourText) {
		this.forcedProgressionSeconds = forcedProgressionSeconds;
		this.floors = floors;
		floorRegions = new BlockBox[floors];
		this.breakInterval = breakInterval;
		this.breakCount = breakCount;
		this.floorMaterial = floorMaterial;
		this.floorBreakingMaterial = floorBreakingMaterial;
		this.flavourText = flavourText;
		gameOver = false;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		widgets = GlobalGameWidgets.registerTo(game, events);

		for (int floor = 0; floor < floors; floor++) {
			floorRegions[floor] = game.mapRegions().getOrThrow("floor" + (floor + 1));
		}

		deathRegion = game.mapRegions().getOrThrow("death");

		Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xACC12F)).withBold(true);
		bossBar = widgets.openBossBar(MinigameTexts.SPLEEF_TITLE_PREPARE.copy().withStyle(style), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

		for (var floor : floorRegions) {
			BlockPlacer.replace(game.level(), floor, floorMaterial, BlockPlacer.Mode.REPLACE, Blocks.WHITE_STAINED_GLASS);
		}

		events.listen(GamePhaseEvents.START, initiator -> {

			// Shouldn't affect a typical game but has a bug in dev with 1 player.
			lastTickPlayers = this.game.participants().stream().toList();

			if (this.game.participants().size() == 1) {
				blockSinglePlayerWin = true;
			}

			int tickDelay = 5 * 20;
			countdownMessage(10, 0x176D85, 0.25f, tickDelay);

			tickDelay += 5 * 20;
			countdownMessage(5, 0x176D85, 0.5f, tickDelay);

			tickDelay += 20;
			countdownMessage(4, 0x307B6F, 0.75f, tickDelay);

			tickDelay += 20;
			countdownMessage(3, 0x48885B, 1f, tickDelay);

			tickDelay += 20;
			countdownMessage(2, 0x5F9446, 1.25f, tickDelay);

			tickDelay += 20;
			countdownMessage(1, 0x77A12F, 1.5f, tickDelay);

			tickDelay += 20;
			game.scheduler().runAfterTicks(tickDelay, this::startGame);
		});

		events.listen(GamePhaseEvents.TICK, this::checkPlayerDeath);
		events.listen(GamePhaseEvents.TICK, this::checkForWinner);

		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		killPlayer(player);
		return TriState.FALSE;
	}

	/**
	 * This is after the countdown has finished.
	 */
	private void startGame() {
		progressionTimer = forcedProgressionSeconds;
		game.scheduler().runPeriodic(0, 20, this::updateForcedProgression);

		game.allPlayers().playSound(SoundEvents.ANVIL_PLACE, SoundSource.MASTER, Integer.MAX_VALUE, 1);
		game.allPlayers().playSound(SoundEvents.BASALT_BREAK, SoundSource.MASTER, Integer.MAX_VALUE, 1);
	}

	private void checkForWinner() {
		if (gameOver) {
			return;
		}
		var participants = game.participants();

		if (participants.size() <= 1) {

			if (game.participants().size() == 1 && !blockSinglePlayerWin) {
				winTitle(participants.stream().toList());
				var displayName = participants.iterator().next().getDisplayName();
				var style = displayName.getStyle();
				if (style.getColor() == null) {
					style = style.withColor(ChatFormatting.WHITE);
				}
				game.allPlayers().playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, Integer.MAX_VALUE, 0.8f);
				announceWinner(Component.translatable(getFlavourTextKey("win"), displayName.copy().withStyle(style)));
			} else if (game.participants().isEmpty()) {
				// Join the winners with a comma from the last tick though add an & for the last join.
				var winners = lastTickPlayers.stream().toList();
				MutableComponent winnerText = Component.literal("");

				for (int i = 0; i < winners.size(); i++) {
					winTitle(winners);
					var displayName = winners.get(i).getDisplayName();
					var style = displayName.getStyle();
					if (style.getColor() == null) {
						style = style.withColor(ChatFormatting.WHITE);
					}
					winnerText.append(winners.get(i).getDisplayName().copy().withStyle(style));
					if (i < winners.size() - 2) {
						winnerText.append(", ");
					} else if (i == winners.size() - 2) {
						winnerText.append(" & ");
					}
				}

				game.allPlayers().playSound(SoundEvents.RAID_HORN.value(), SoundSource.MASTER, Integer.MAX_VALUE, 0.75f);
				announceWinner(Component.translatable(getFlavourTextKey("winners"), winnerText));
			}
		} else {
			lastTickPlayers = participants.stream().toList();
		}
	}

	private void winTitle(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			title(player, MinigameTexts.WINNER_TITLE.copy().withStyle(ChatFormatting.GREEN),
					MinigameTexts.WINNER_SUBTITLE.copy(),
					20, 20 * 3, 20);
		}
	}

	private void checkPlayerDeath() {
		List<ServerPlayer> playersToKill = new ArrayList<>();
		for (ServerPlayer player : game.participants()) {
			if (deathRegion.intersects(player.getBoundingBox())) {
				playersToKill.add(player);
			}
		}
		playersToKill.forEach(this::killPlayer);
	}

	/**
	 * Trigger the end game timer as well as show the winner.
	 */
	private void announceWinner(MutableComponent winner) {
		gameOver = true;

		game.scheduler().clearAllEvents();

		specialMessage(Component.literal("★").withStyle(ChatFormatting.GOLD),
				winner.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xACC12F))));

		bossBar.close();

		game.scheduler().runAfterSeconds(8, () -> {
			game.requestStop(GameStopReason.finished());
		});
	}

	private void killPlayer(ServerPlayer player) {
		if (!gameOver) {
			specialMessage(Component.literal("☠").withStyle(ChatFormatting.RED), player.getDisplayName().copy().withStyle(ChatFormatting.DARK_GRAY));
			title(player, MinigameTexts.SPLEEF_ELIMINATED.copy().withStyle(ChatFormatting.RED),
					Component.translatable(getFlavourTextKey("eliminated")).withStyle(ChatFormatting.YELLOW),
					20, 20 * 3, 20);
			player.playNotifySound(SoundEvents.ANVIL_PLACE, SoundSource.MASTER, Integer.MAX_VALUE, 1);
		}
		game.setPlayerRole(player, PlayerRole.SPECTATOR);
	}

	private void specialMessage(Component sender, Component message) {
		game.allPlayers().sendMessage(Component.literal("[").withStyle(ChatFormatting.GRAY)
				.append(sender).append("]: ")
				.append(message), false);
	}

	private String getFlavourTextKey(String key) {
		return "ltminigames.minigame.spleef.flavor." + flavourText + "." + key;
	}

	private void startFloor() {
		progressionTimer = forcedProgressionSeconds;
	}

	private void updateForcedProgression() {
		if (currentFloor >= floors) {
			return;
		}
		if (progressionTimer <= 0) {
			progressionTimer = forcedProgressionSeconds;
			spleefMessage(Component.translatable(getFlavourTextKey("forced_progression"), Component.translatable("ltminigames.minigame.position." + (currentFloor + 1))).withStyle(ChatFormatting.YELLOW));
			BlockPlacer.replace(game.level(), floorRegions[currentFloor], floorBreakingMaterial, BlockPlacer.Mode.REPLACE, floorMaterial, game.scheduler(),
					(pos) -> (game.level().random.nextInt(breakCount) * breakInterval),
					(pos) -> {
						game.scheduler().runAfterTicks(15 + game.level().random.nextInt(10), () ->
								game.level().destroyBlock(pos, false)
						);
					});
			currentFloor++;
		}

		switch (progressionTimer) {
			case 10 ->
					spleefMessage(Component.translatable(getFlavourTextKey("layer_countdown"), Component.literal(Integer.toString(progressionTimer)).withStyle(DARK_YELLOW)).withStyle(ChatFormatting.YELLOW));
			case 5, 4, 3, 2, 1 -> {
				spleefMessage(Component.literal(Integer.toString(progressionTimer)).withStyle(DARK_YELLOW), false);
				game.allPlayers().playSound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, Integer.MAX_VALUE, 1.75f - (progressionTimer * 0.25f));
			}
		}

		updateForcedProgressionBossbar();
		progressionTimer--;
	}

	private void updateForcedProgressionBossbar() {
		Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xACC12F)).withBold(true);
		bossBar.setTitle(MinigameTexts.SPLEEF_TITLE_FORCED_PROGRESSION.copy().withStyle(style)
				.append(Component.literal(Integer.toString(progressionTimer)).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))));
	}

	private void countdownMessage(int seconds, int color, float soundPitch, int delayTicks) {
		game.scheduler().runAfterTicks(delayTicks, () -> {
			title(MinigameTexts.SPLEEF_COUNTDOWN_TITLE.apply(
									Component.literal(Integer.toString(seconds)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
							.withStyle(ChatFormatting.GRAY),
					MinigameTexts.SPLEEF_COUNTDOWN_SUBTITLE.copy().withStyle(ChatFormatting.YELLOW),
					5, 20, seconds == 1 ? 0 : 5);
			game.allPlayers().playSound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.MASTER, Integer.MAX_VALUE, soundPitch);
		});
	}

	private void spleefMessage(Component message) {
		spleefMessage(message, true);
	}

	private void spleefMessage(Component message, boolean showGameName) {
		var fullMessage = Component.literal("").append(Component.literal(">>> ").withStyle(ChatFormatting.GRAY));
		if (showGameName) {
			fullMessage = fullMessage.append(MinigameTexts.SPLEEF).append(Component.literal(" ❯ "));
		}
		fullMessage = fullMessage.append(message);
		game.allPlayers().sendMessage(fullMessage.withStyle(ChatFormatting.YELLOW), false);
	}

	private void title(Component title, Component subtitle, int pFadeIn, int pStay, int pFadeOut) {
		PlayerSet players = game.allPlayers();
		title(players, title, subtitle, pFadeIn, pStay, pFadeOut);
	}

	private void title(PlayerIterable players, Component title, Component subtitle, int pFadeIn, int pStay, int pFadeOut) {
		players.sendPacket(new ClientboundSetTitlesAnimationPacket(pFadeIn, pStay, pFadeOut));
		players.sendPacket(new ClientboundSetTitleTextPacket(title));
		players.sendPacket(new ClientboundSetSubtitleTextPacket(subtitle));
	}

	private void title(ServerPlayer player, Component title, Component subtitle, int pFadeIn, int pStay, int pFadeOut) {
		PlayerSet players = game.allPlayers();
		player.connection.send(new ClientboundSetTitlesAnimationPacket(pFadeIn, pStay, pFadeOut));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
	}
}
