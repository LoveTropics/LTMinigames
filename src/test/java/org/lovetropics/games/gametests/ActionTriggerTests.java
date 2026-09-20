package org.lovetropics.games.gametests;

import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.ActionTarget;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.instances.action.GiveEffectAction;
import org.lovetropics.games.common.core.game.behavior.instances.action.PlaySoundAction;
import org.lovetropics.games.common.core.game.behavior.instances.action.RunCommandsAction;
import org.lovetropics.games.common.core.game.behavior.instances.action.SendMessageAction;
import org.lovetropics.games.common.core.game.behavior.instances.trigger.GeneralEventsTrigger;
import org.lovetropics.games.common.core.game.behavior.instances.trigger.phase.StartGameTrigger;
import org.lovetropics.games.common.core.game.behavior.instances.trigger.phase.StopGameTrigger;
import org.lovetropics.games.common.core.game.datagen.BehaviorFactory;
import org.lovetropics.games.common.core.game.datagen.GameProvider;
import org.lovetropics.games.common.core.game.map.InlineMapProvider;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import org.lovetropics.games.gametests.api.GameTest;
import org.lovetropics.games.gametests.api.LTFakePlayer;
import org.lovetropics.games.gametests.api.LTGameTestHelper;
import org.lovetropics.games.gametests.api.MinigameTest;
import org.lovetropics.games.gametests.api.RegisterMinigameTest;
import org.lovetropics.games.gametests.api.TestGameLobby;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextMap;
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
								new SendMessageAction(new TemplatedText(Component.literal("hello world!")), false)
						)), new PlaySoundAction(SoundEvents.ALLAY_HURT, 0.5f, 0.5f, SoundSource.AMBIENT, false)));

		generator.builder(gameId("stop"))
				.withPlayingPhase(new InlineMapProvider(Level.OVERWORLD), phaseBuilder -> phaseBuilder
						.withBehavior(new StopGameTrigger(behaviors.applyToAllPlayers(
								new RunCommandsAction(List.of(), List.of("give @s minecraft:oak_planks 13"))
						), Optional.empty(), Optional.empty())));

		generator.builder(gameId("events"))
				.withPlayingPhase(new InlineMapProvider(Level.OVERWORLD), phaseBuilder -> phaseBuilder
						.withBehavior(new GeneralEventsTrigger(Map.of(
								"player_hurt", behaviors.actions(ActionTarget.PASS, new GiveEffectAction(
										List.of(new MobEffectInstance(MobEffects.ABSORPTION, 23, 2))
								))
						))));
	}

	@GameTest
	public void testEventsTrigger(LTGameTestHelper helper) {
		LTFakePlayer player = helper.playerBuilder()
				.isVulnerableTo(source -> source.is(DamageTypes.FELL_OUT_OF_WORLD))
				.build();

		TestGameLobby lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("events"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(5)
				.thenExecute(() -> player.hurtServer(helper.getLevel(), player.damageSources().fellOutOfWorld(), 1))
				.thenIdle(5)
				.thenExecute(() -> helper.assertTrue(player.getEffect(MobEffects.ABSORPTION) != null, "Effect could not be found on player!"))
				.thenExecute(() -> helper.assertTrue(player.getEffect(MobEffects.ABSORPTION).getAmplifier() == 2 && player.getEffect(MobEffects.ABSORPTION).getDuration() == 23 - 5, "Effect was not as expected!"))
				.thenSucceed();
	}

	@GameTest
	public void testStartTrigger(LTGameTestHelper helper) {
		LTFakePlayer player = helper.playerBuilder()
				.packetFilter(packet -> packet instanceof ClientboundSystemChatPacket sc && sc.content().equals(Component.literal("hello world!")) || packet instanceof ClientboundSoundPacket it && it.getSound().value() == SoundEvents.ALLAY_HURT)
				.build();
		TestGameLobby lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("start"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(20)
				.thenExecute(() -> helper.assertReceivedPacket(player, 0, ClientboundSystemChatPacket.class, it -> it.content().equals(Component.literal("hello world!"))))
				.thenExecute(() -> lobby.getTopPhase().invoker(GameActionEvents.APPLY).apply(ContextMap.EMPTY, ActionSubjects.ofPlayer(player)))
				.thenExecute(() -> helper.assertReceivedPacket(player, 1, ClientboundSoundPacket.class, it -> it.getSound().value() == SoundEvents.ALLAY_HURT && it.getVolume() == 0.5f && it.getPitch() == 0.5f))
				.thenSucceed();
	}

	@GameTest
	public void testStopTrigger(LTGameTestHelper helper) {
		LTFakePlayer player = helper.createFakePlayer();
		TestGameLobby lobby = helper.createGame(player, PlayerRole.PARTICIPANT);
		lobby.enqueue(gameId("stop"));

		helper.startSequence()
				.thenExecute(helper.startGame(lobby))
				.thenIdle(20)
				.thenExecute(() -> lobby.getTopPhase().requestStop(GameStopReason.finished()))
				.thenExecute(() -> helper.assertPlayerInventoryContainsAt(player, 0, new ItemStack(Items.OAK_PLANKS, 13)))
				.thenSucceed();
	}

	@Override
	public Identifier id() {
		return Identifier.fromNamespaceAndPath("lttest", "action_trigger_test");
	}
}
