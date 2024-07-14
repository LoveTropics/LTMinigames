package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.core.dimension.RegistryEntryRemover;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(MappedRegistry.class)
public class SimpleRegistryMixin<T> implements RegistryEntryRemover<T> {
    @Shadow @Final private ObjectList<Holder.Reference<T>> byId;
    @Shadow @Final private Reference2IntMap<T> toId;
    @Shadow @Final private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow @Final private Map<T, Holder.Reference<T>> byValue;
    @Shadow @Final private Map<T, RegistrationInfo> registrationInfos;
    @Shadow private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

    @Override
    public boolean ltminigames$remove(T entry) {
        int rawId = this.toId.removeInt(entry);
        if (rawId == -1) {
            return false;
        }

        final Holder.Reference<T> reference = this.byId.remove(rawId);
        final ResourceKey<T> key = reference.key();

        this.byLocation.remove(key.location());
        this.byKey.remove(key);
        this.byValue.remove(entry);
        this.registrationInfos.remove(entry);
        if (this.unregisteredIntrusiveHolders != null) {
            this.unregisteredIntrusiveHolders.remove(entry);
        }

        // This is an extreme hack that only works because the network IDs aren't used for dimensions
        for (int id = rawId; id < this.byId.size(); id++) {
            this.toId.put(this.byId.get(id).value(), id);
        }

        return true;
    }

    @Override
    public boolean ltminigames$remove(ResourceLocation key) {
        Holder.Reference<T> entry = this.byLocation.get(key);
        return entry != null && this.ltminigames$remove(entry.value());
    }
}
