package com.lovetropics.minigames.common.content.survive_the_tide.item;

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
        super(properties.maxDamage(180));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("Prevents heatwaves from slowing you down.").mergeStyle(TextFormatting.GOLD));
        tooltip.add(new StringTextComponent(""));
        tooltip.add(new StringTextComponent("Active when held in main hand or off-hand.").mergeStyle(TextFormatting.AQUA));
    }
}
