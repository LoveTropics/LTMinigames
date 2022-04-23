package com.lovetropics.minigames.common.content.biodiversity_blitz.merchant;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.OptionalInt;

public final class BbMerchant implements Merchant {
    private final Player customer;
    private final MerchantOffers offers;

    public BbMerchant(Player player, MerchantOffers offers) {
        this.customer = player;
        this.offers = offers;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {

    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.customer;
    }

    @Override
    public MerchantOffers getOffers() {
        return this.offers;
    }

    @Override
    public void overrideOffers(@Nullable MerchantOffers offers) {

    }

    @Override
    public void notifyTrade(MerchantOffer offer) {

    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {

    }

    @Override
    public Level getLevel() {
        return this.customer.level;
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xpIn) {

    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public void openTradingScreen(Player player, Component displayName, int level) {
        OptionalInt container = player.openMenu(new SimpleMenuProvider(this::createContainer, displayName));
        if (container.isPresent()) {
            MerchantOffers offers = this.getOffers();
            if (!offers.isEmpty()) {
                player.sendMerchantOffers(container.getAsInt(), offers, level, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
            }
        }
    }

    private MerchantMenu createContainer(int id, Inventory playerInventory, Player player) {
        return new BbMerchantContainer(this, id, playerInventory);
    }
}
