package com.lovetropics.minigames.gametests;

import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.NoneActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.action.PlayerActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.GiveEffectAction;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.PlaySoundAction;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.RunCommandsAction;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.SendMessageAction;
import com.lovetropics.minigames.common.core.game.behavior.instances.trigger.GeneralEventsTrigger;
import com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase.StartGameTrigger;
import com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase.StopGameTrigger;
import com.lovetropics.minigames.common.core.game.datagen.BehaviorFactory;
import com.lovetropics.minigames.common.core.game.datagen.GameProvider;
import com.lovetropics.minigames.common.core.game.map.InlineMapProvider;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.lovetropics.minigames.gametests.api.GameTest;
import com.lovetropics.minigames.gametests.api.LTGameTestHelper;
import com.lovetropics.minigames.gametests.api.MinigameTest;
import com.lovetropics.minigames.gametests.api.RegisterMinigameTest;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RegisterMinigameTest
public class ActionTriggerTests implements MinigameTest {
	@Override
	public void generateGame(GameProvider.GameGenerator generator, BehaviorFactory behaviors, HolderLookup.Provider registries) {
		generator.builder(gameId("start"))
				.withPlayingPhase(new InlineMapProvider(Level.OVERWORLD), phaseBuilder -> phaseBuilder
						.withBehavior(new StartGameTrigger(behaviors.applyToAllPlayers(
								NoneActionTarget.INSTANCE,
								new SendMessageAction(new TemplatedText(Component.literal("hello world!")))
						)), new PlaySoundAction(SoundEvents.ALLAY_HURT, 0.5f, 0.5f, SoundSource.AMBIENT, false)));

		generator.builder(gameId("stop"))
				.withPlayingPhase(new InlineMapProvider(Level.OVERWORLD), phaseBuilder -> phaseBuilder
						.withBehavior(new StopGameTrigger(behaviors.applyToAllPlayers(
								NoneActionTarget.INSTANCE,
								new RunCommandsAction(List.of(), List.of("give @s minecraft:oak_planks 13"))
						), Optional.empty(), Optional.empty())));

		generator.builder(gameId("events"))
				.withPlayingPhase(new InlineMapProvider(Level.OVERWORLD), phaseBuilder -> phaseBuilder
						.withBehavior(new GeneralEventsTrigger(Map.of(
								"player_hurt", behaviors.actions(PlayerActionTarget.SOURCE, new GiveEffectAction(
										List.of(new MobEffectInstance(MobEffects.ABSORPTION, 23, 2))
								))
						))));
	}

	@GameTest
	public void testEventsTrigger(final LTGameTestHelper helper) {
		final var player = helper.playerBuilder()
				.isVulnerableTo(source -> source.is(DamageTypes.FELL_OUT_OF_WORLD))
				.build();

		final var lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("events"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(5)
				.thenExecute(() -> player.hurt(player.damageSources().fellOutOfWorld(), 1))
				.thenIdle(5)
				.thenExecute(() -> helper.assertTrue(player.getEffect(MobEffects.ABSORPTION) != null, "Effect could not be found on player!"))
				.thenExecute(() -> helper.assertTrue(player.getEffect(MobEffects.ABSORPTION).getAmplifier() == 2 && player.getEffect(MobEffects.ABSORPTION).getDuration() == 23 - 5, "Effect was not as expected!"))
				.thenSucceed();
	}

	@GameTest
	public void testStartTrigger(final LTGameTestHelper helper) {
		final var player = helper.playerBuilder()
				.packetFilter(packet -> packet instanceof ClientboundSystemChatPacket sc && sc.content().equals(Component.literal("hello world!")) || packet instanceof ClientboundSoundPacket it && it.getSound().value() == SoundEvents.ALLAY_HURT)
				.build();
		final var lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("start"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(20)
				.thenExecute(() -> helper.assertReceivedPacket(player, 0, ClientboundSystemChatPacket.class, it -> it.content().equals(Component.literal("hello world!"))))
				.thenExecute(() -> lobby.getActivePhase().invoker(GameActionEvents.APPLY_TO_PLAYER).apply(GameActionContext.EMPTY, player))
				.thenExecute(() -> helper.assertReceivedPacket(player, 1, ClientboundSoundPacket.class, it -> it.getSound().value() == SoundEvents.ALLAY_HURT && it.getVolume() == 0.5f && it.getPitch() == 0.5f))
				.thenSucceed();
	}

	@GameTest
	public void testStopTrigger(final LTGameTestHelper helper) {
		final var player = helper.createFakePlayer();
		final var lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("stop"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(20)
				.thenExecute(() -> lobby.getActivePhase().requestStop(GameStopReason.finished()))
				.thenExecute(() -> helper.assertPlayerInventoryContainsAt(player, 0, new ItemStack(Items.OAK_PLANKS, 13)))
				.thenSucceed();
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.fromNamespaceAndPath("lttest", "action_trigger_test");
	}
}
