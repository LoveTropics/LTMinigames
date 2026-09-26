package org.lovetropics.games.common.core.game.weather.tornado;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import org.lovetropics.games.common.util.EntityTemplate;

public record NadoEntitySpawnSettings(EntityTemplate template, int spawnRate, float damageAmount) {

    public static final float DEFAULT_DAMAGE_AMOUNT = 1.5f;

    public static final Codec<NadoEntitySpawnSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityTemplate.CODEC.fieldOf("entity").forGetter(NadoEntitySpawnSettings::template),
            Codec.INT.optionalFieldOf("spawn_rate", SharedConstants.TICKS_PER_SECOND).forGetter(NadoEntitySpawnSettings::spawnRate),
            Codec.FLOAT.optionalFieldOf("damage_amount", NadoEntitySpawnSettings.DEFAULT_DAMAGE_AMOUNT).forGetter(NadoEntitySpawnSettings::damageAmount)

    ).apply(i, NadoEntitySpawnSettings::new));

    public static NadoEntitySpawnSettings of(EntityTemplate template) {
        return new NadoEntitySpawnSettings(template, SharedConstants.TICKS_PER_SECOND, DEFAULT_DAMAGE_AMOUNT);
    }

}
