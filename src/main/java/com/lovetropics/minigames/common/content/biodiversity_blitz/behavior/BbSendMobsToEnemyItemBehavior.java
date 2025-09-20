package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner.BbEntityTypes;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class BbSendMobsToEnemyItemBehavior implements IGameBehavior {
    public static final MapCodec<BbSendMobsToEnemyItemBehavior> CODEC = Codecs.ITEMS.fieldOf("item")
            .xmap(BbSendMobsToEnemyItemBehavior::new, b -> b.items);

    private final HolderSet<Item> items;

    public BbSendMobsToEnemyItemBehavior(HolderSet<Item> items) {
        this.items = items;
    }

    private Multimap<Plot, Entity> sentEnemies = HashMultimap.create();

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

        events.listen(BbEvents.MODIFY_WAVE_MODS, (entities, random, world, plot, waveIndex) -> entities.addAll(sentEnemies.removeAll(plot)));
        events.listen(GamePhaseEvents.START, () -> sentEnemies = Multimaps.synchronizedMultimap(Multimaps.newListMultimap(new HashMap<>(), LinkedList::new)));
        events.listen(GamePhaseEvents.STOP, reason -> sentEnemies.clear());

        final var plots = game.state().getOrThrow(PlotsState.KEY);
        events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> {
            final var item = player.getItemInHand(hand);
            return tryUseMobItem(player, item, plots, teams) ? InteractionResult.CONSUME : InteractionResult.PASS;
        });

        events.listen(GamePlayerEvents.ATTACK, (player, target) -> tryUseMobItem(player, player.getMainHandItem(), plots, teams) ? TriState.TRUE : TriState.DEFAULT);
    }

    private boolean tryUseMobItem(ServerPlayer player, ItemStack item, PlotsState plots, TeamState teams) {
		if (!items.contains(item.getItemHolder())) {
			return false;
		}

		final var playerPlot = plots.getPlotFor(player);

        Map<BbEntityTypes, Integer> entities = item.get(BiodiversityBlitz.ENEMIES_TO_SEND);
        if (entities != null) {
            plots.stream().filter(p -> p != playerPlot)
                    .forEach(targetPlot -> {
                        final Component playerName = player.getName().copy().withStyle(ChatFormatting.AQUA);
                        teams.getPlayersForTeam(targetPlot.team).sendMessage(BiodiversityBlitzTexts.SENT_MOBS_MESSAGE.apply(playerName, buildMessage(entities)));

                        sentEnemies.putAll(targetPlot, entities.entrySet().stream()
                                .flatMap(entry -> repeat(() -> entry.getKey().create(player.level(), targetPlot), entry.getValue()))
                                .toList());
                    });
        }

		item.shrink(1);
		return true;
	}

    public Component buildMessage(Map<BbEntityTypes, Integer> entities) {
        MutableComponent component = Component.empty();
        final var itr = entities.entrySet().iterator();
        while (itr.hasNext()) {
            final var next = itr.next();
            component.append(String.valueOf(next.getValue())).append("x ").append(next.getKey().getName().withStyle(ChatFormatting.GOLD));

            if (itr.hasNext()) {
                component = component.append(", ");
            }
        }

        return component;
    }

    private static <T> Stream<T> repeat(Supplier<T> value, int amount) {
        final var builder = Stream.<T>builder();
        for (int i = 0; i < amount; i++) {
            builder.accept(value.get());
        }
        return builder.build();
    }

    @EventBusSubscriber(Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        static void appendTooltips(final RenderTooltipEvent.GatherComponents event) {
            Map<BbEntityTypes, Integer> entities = event.getItemStack().get(BiodiversityBlitz.ENEMIES_TO_SEND);
            if (entities != null) {
                entities.forEach((entity, count) ->
                        event.getTooltipElements().add(Either.left(BiodiversityBlitzTexts.sendMobsTooltip(entity, count))));
            }
        }
    }
}
