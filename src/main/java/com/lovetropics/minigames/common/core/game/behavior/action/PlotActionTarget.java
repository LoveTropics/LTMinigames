package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Lists;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public record PlotActionTarget(Target target) implements ActionTarget<Plot> {
    public static final Codec<PlotActionTarget> CODEC = RecordCodecBuilder.create(in -> in.group(
            Target.CODEC.optionalFieldOf("target", PlotActionTarget.Target.SOURCE).forGetter(PlotActionTarget::target)
    ).apply(in, PlotActionTarget::new));

    @Override
    public List<Plot> resolve(IGamePhase phase, Iterable<Plot> sources) {
        return target.resolve(phase, sources);
    }

    @Override
    public boolean apply(IGamePhase game, GameEventListeners listeners, GameActionContext actionContext, Iterable<Plot> sources) {
        boolean result = false;
        for (Plot target : target.resolve(game, sources)) {
            result |= listeners.invoker(GameActionEvents.APPLY_TO_PLOT).apply(actionContext, target);
        }
        return result;
    }

    @Override
    public Codec<? extends ActionTarget<Plot>> type() {
        return ActionTargetTypes.PLOT.get();
    }

    public enum Target implements StringRepresentable {
        NONE("none"),
        SOURCE("source"),
        ALL("all"),
        ;

        public static final Codec<Target> CODEC = MoreCodecs.stringVariants(values(), Target::getSerializedName);

        private final String name;

        Target(String name) {
            this.name = name;
        }

        public List<Plot> resolve(IGamePhase game, Iterable<Plot> sources) {
            // Copy the lists because we might otherwise get concurrent modification from whatever the actions do!
            return switch (this) {
                case NONE -> List.of();
                case SOURCE -> Lists.newArrayList(sources);
                case ALL -> Lists.newArrayList(game.getState().getOrThrow(PlotsState.KEY));
            };
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
