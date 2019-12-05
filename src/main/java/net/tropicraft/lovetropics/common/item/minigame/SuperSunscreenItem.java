package net.tropicraft.lovetropics.common.item.minigame;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class SuperSunscreenItem extends Item {
    public SuperSunscreenItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Prevents heatwaves from slowing you down.").applyTextStyle(TextFormatting.GOLD));
        tooltip.add(new StringTextComponent(""));
        tooltip.add(new StringTextComponent("Active when held in off-hand.").applyTextStyle(TextFormatting.AQUA));
    }
}
