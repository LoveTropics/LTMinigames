package org.lovetropics.games.common.core.game.weather.tornado;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.InterModComms;
import org.slf4j.Logger;

import java.util.Optional;

public record Tornado(
        Optional<PlayerController> playerController,
        boolean isBaby,
        boolean isFireNado,
        Optional<NadoEntitySpawnSettings> entitySpawnSettings,
        int startStage,
        int maxStage
) {

	private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<Tornado> CODEC = RecordCodecBuilder.create(i -> i.group(
            PlayerController.CODEC.optionalFieldOf("player_controller").forGetter(Tornado::playerController),
            Codec.BOOL.optionalFieldOf("baby", false).forGetter(Tornado::isBaby),
            Codec.BOOL.optionalFieldOf("fire", false).forGetter(Tornado::isFireNado),
            NadoEntitySpawnSettings.CODEC.optionalFieldOf("entity_spawn_settings").forGetter(Tornado::entitySpawnSettings),
            Codec.INT.optionalFieldOf("start_stage", 5).forGetter(Tornado::startStage), // This should be better than some magic numbers
            Codec.INT.optionalFieldOf("max_stage", 8).forGetter(Tornado::maxStage) // Again should be better than some magic numbers
    ).apply(i, Tornado::new));

	public void postAsIMCMessage(ServerLevel level) {
		Tornado.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), this)
				.ifError(error -> LOGGER.error("Failed to encode tornado for IMC message: {}", error))
				.ifSuccess(tag -> tag.asCompound().ifPresent(compoundTag -> {
					compoundTag.putString("dimension", level.dimension().identifier().toString());
					InterModComms.sendTo("weather2", "spawn_tornado", () -> compoundTag);
				}));
	}
}
