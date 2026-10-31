package org.lovetropics.games.common.core.game.persistent.behavior.crab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.instances.action.RunCommandsAction;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;

import java.util.function.Supplier;

public class CrabGolfWinBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfWinBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC).fieldOf("firework").forGetter(b -> b.nbt),
			ItemStackTemplate.CODEC.fieldOf("reward").forGetter(b -> b.reward),
			RunCommandsAction.COMMAND_CODEC.fieldOf("on_start").forGetter(b -> b.onStart),
			RunCommandsAction.COMMAND_CODEC.fieldOf("on_end").forGetter(b -> b.onEnd)
	).apply(instance, CrabGolfWinBehavior::new));

	private static final EntitySpawnRequest SPAWN_REQUEST = new EntitySpawnRequest(EntitySpawnReason.COMMAND, false);

	private final CompoundTag nbt;
	private final ItemStackTemplate reward;
	private final String onStart;
	private final String onEnd;

	public CrabGolfWinBehavior(CompoundTag nbt, ItemStackTemplate reward, String onStart, String onEnd) {
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
				player.addItem(reward.create());
			}
			boolean highScore = data.submitNewScore(hole, player, score);
			if (highScore) {
				// Got high score for this hole?
				player.addItem(reward.create());
			}

			if (highScore) {
				game.invoker(CrabGolfEvents.HIGH_SCORE).onWin(hole, player, score);
			}

			if (!onEnd.isEmpty()) {
				Commands commands = game.level().getServer().getCommands();
				CommandSourceStack targetSource = getCommandSourceStack(game, player);
				commands.performPrefixedCommand(targetSource, onEnd);
			}

			game.level().playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.getFirst().value(), SoundSource.BLOCKS, 1F, 1.0F);

			CompoundTag nbt = this.nbt.copy();
			nbt.putString("id", "minecraft:firework_rocket");

			Vec3 pos = player.position().add(0, 3, 0);

			Entity entity = EntityType.loadEntityRecursive(nbt, player.level(), SPAWN_REQUEST, (e) -> {
				e.snapTo(pos.x, pos.y, pos.z, e.getYRot(), e.getXRot());
				return e;
			});

			player.level().tryAddFreshEntityWithPassengers(entity);
		});
	}

	private static @NotNull CommandSourceStack getCommandSourceStack(PersistentGame game, ServerPlayer player) {
		CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, game.level(), LevelBasedPermissionSet.OWNER, "crabgolf", Component.literal("crabgolf"), game.level().getServer(), null);

		CommandSourceStack targetSource = source.withEntity(player).withPosition(player.position());
		return targetSource;
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_WIN;
	}
}
