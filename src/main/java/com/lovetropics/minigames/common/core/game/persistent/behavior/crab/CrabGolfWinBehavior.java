package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.RunCommandsAction;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CrabGolfWinBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfWinBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC).fieldOf("firework").forGetter(b -> b.nbt),
			ItemStack.CODEC.fieldOf("reward").forGetter(b -> b.reward),
			RunCommandsAction.COMMAND_CODEC.fieldOf("on_start").forGetter(b -> b.onStart),
			RunCommandsAction.COMMAND_CODEC.fieldOf("on_end").forGetter(b -> b.onEnd)
	).apply(instance, CrabGolfWinBehavior::new));

	private final CompoundTag nbt;
	private final ItemStack reward;
	private final String onStart;
	private final String onEnd;

	public CrabGolfWinBehavior(CompoundTag nbt, ItemStack reward, String onStart, String onEnd) {
		this.nbt = nbt;
		this.reward = reward;
		this.onStart = onStart;
		this.onEnd = onEnd;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(CrabGolfEvents.START_GAME, (hole, player) -> {
			if (!onStart.isEmpty()) {
				Commands commands = game.level().getServer().getCommands();
				CommandSourceStack targetSource = getCommandSourceStack(game, player);
				commands.performPrefixedCommand(targetSource, onStart);
			}
		});

		events.listen(CrabGolfEvents.WIN_GAME, (hole, player, score) -> {
			if (score == -1) {
				return;
			}

			GolfData data = GolfData.get(player.level());
			if (data.getHighScoreFor(hole, player) == -1) {
				// First time playing this hole?
				player.addItem(reward.copy());
			}
			boolean highScore = data.submitNewScore(hole, player, score);
			if (highScore) {
				// Got high score for this hole?
				player.addItem(reward.copy());
			}

			if (highScore) {
				game.invoker(CrabGolfEvents.HIGH_SCORE).onWin(hole, player, score);
			}

			if (!onEnd.isEmpty()) {
				Commands commands = game.level().getServer().getCommands();
				CommandSourceStack targetSource = getCommandSourceStack(game, player);
				commands.performPrefixedCommand(targetSource, onEnd);
			}

			game.level().playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.BLOCKS, 1F, 1.0F);

			CompoundTag nbt = this.nbt.copy();
			nbt.putString("id", "minecraft:firework_rocket");

			Vec3 pos = player.position().add(0, 3, 0);

			Entity entity = EntityType.loadEntityRecursive(nbt, player.level(), EntitySpawnReason.COMMAND, (e) -> {
				e.snapTo(pos.x, pos.y, pos.z, e.getYRot(), e.getXRot());
				return e;
			});

			player.level().tryAddFreshEntityWithPassengers(entity);
		});
	}

	private static @NotNull CommandSourceStack getCommandSourceStack(PersistentGame game, ServerPlayer player) {
		CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, game.level(), Commands.LEVEL_OWNERS, "crabgolf", Component.literal("crabgolf"), game.level().getServer(), null);

		CommandSourceStack targetSource = source.withEntity(player).withPosition(player.position());
		return targetSource;
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_WIN;
	}

	public static class GolfData extends SavedData {
		public static final Codec<GolfData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, Object::toString), HoleData.CODEC).fieldOf("data").forGetter(b -> b.data)
		).apply(instance, GolfData::new));

		private static final SavedDataType<GolfData> TYPE = new SavedDataType<>(
				LoveTropics.ID + "_crab_golf", GolfData::new, CODEC);

		private final Map<Integer, HoleData> data;

		private GolfData() {
			this(new HashMap<>());
		}

		private GolfData(Map<Integer, HoleData> data) {
			this.data = new HashMap<>(data);
		}

		public int getHighScore(int hole) {
			HoleData d = data.get(hole);
			if (d != null) {
				return d.highScore;
			}

			return -1;
		}

		public UUID getHighScoreUUIDFor(int hole) {
			HoleData d = data.get(hole);
			if (d != null) {
				List<UUID> res = new ArrayList<>();
				for (Map.Entry<UUID, Integer> e : d.playerHighScores.entrySet()) {
					if (e.getValue() == d.highScore) {
						res.add(e.getKey());
					}
				}

				Collections.shuffle(res);

				return res.isEmpty() ? null : res.getFirst();
			}

			return null;
		}

		public void clearHole(int hole) {
			data.remove(hole);
			setDirty();
		}

		public int getHighScoreFor(int hole, ServerPlayer player) {
			HoleData d = data.get(hole);
			if (d == null) {
				return -1;
			}

			Integer i = d.playerHighScores.get(player.getUUID());
			return i == null ? -1 : i;
		}

		// Returns whether this is a high score
		public boolean submitNewScore(int hole, ServerPlayer player, int score) {
			if (score == -1) {
				return false;
			}

			HoleData d = data.get(hole);
			if (d == null) {
				HoleData hdata = new HoleData(score, 1, Map.of(player.getUUID(), score));
				data.put(hole, hdata);
				setDirty();
				return true;
			}

			boolean betterEquals = d.highScore <= score;
			d.highScore = Math.min(d.highScore, score);
			d.totalPlays++;
			d.playerHighScores.merge(player.getUUID(), score, Math::min);
			setDirty();

			return betterEquals;
		}

		public static GolfData get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(TYPE);
		}

		private static class HoleData {
			public static final Codec<HoleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.INT.fieldOf("high_score").forGetter(b -> b.highScore),
					Codec.INT.fieldOf("total_play").forGetter(b -> b.totalPlays),
					Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Codec.INT)
							.fieldOf("player_scores").forGetter(b -> b.playerHighScores)
			).apply(instance, HoleData::new));
			private int highScore;
			private int totalPlays;
			private Map<UUID, Integer> playerHighScores;

			public HoleData(int highScore, int totalPlays, Map<UUID, Integer> playerHighScores) {
				this.highScore = highScore;
				this.totalPlays = totalPlays;
				this.playerHighScores = new HashMap<>(playerHighScores);
			}
		}
	}
}
