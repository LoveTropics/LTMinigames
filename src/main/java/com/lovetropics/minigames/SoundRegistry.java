package com.lovetropics.minigames;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SoundRegistry {
	public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, LoveTropics.ID);

	public static final Holder<SoundEvent> SWAP_PLAYERS = register("swap_players");
	public static final Holder<SoundEvent> PACKAGE_RECEIVE = register("package_receive");
	public static final Holder<SoundEvent> SABOTAGE_RECEIVE = register("sabotage_receive");
	public static final Holder<SoundEvent> ACID_FLASH_FLOODING_IMMINENT = register("stt4.acid_flash_flooding_imminent");
	public static final Holder<SoundEvent> FLASH_FLOODING_IMMINENT = register("stt4.flash_flooding_imminent");
	public static final Holder<SoundEvent> LAST_SHUTTLE_DEPARTING = register("stt4.last_shuttle_departing");
	public static final Holder<SoundEvent> COINS = register("coins");
	public static final Holder<SoundEvent> CORRECT = register("correct");
	public static final Holder<SoundEvent> INCORRECT = register("incorrect");
	public static final Holder<SoundEvent> QUIET_EXPLOSION = register("quiet_explosion");

	public static final Holder<SoundEvent> TERRY_TRASH_WELCOME = register("escape_race.terry_trash.welcome");
	public static final Holder<SoundEvent> TERRY_TRASH_CODE_UNLOCK = register("escape_race.terry_trash.code_unlock");
	public static final Holder<SoundEvent> TERRY_TRASH_DIRTY_TRASH = register("escape_race.terry_trash.dirty_trash");
	public static final Holder<SoundEvent> TERRY_TRASH_FOOD_CONTAMINATION = register("escape_race.terry_trash.food_contamination");
	public static final Holder<SoundEvent> TERRY_TRASH_COCONUT_BOMBS = register("escape_race.terry_trash.coconut_bombs");
	public static final Holder<SoundEvent> TERRY_TRASH_CHEMICAL_CONTAMINATION = register("escape_race.terry_trash.chemical_contamination");
	public static final Holder<SoundEvent> TERRY_TRASH_TWO_MINUTES = register("escape_race.terry_trash.two_minutes");
	public static final Holder<SoundEvent> TERRY_TRASH_THIRTY_SECONDS = register("escape_race.terry_trash.thirty_seconds");
	public static final Holder<SoundEvent> TERRY_TRASH_FIVE_SECONDS = register("escape_race.terry_trash.five_seconds");

	private static Holder<SoundEvent> register(String name) {
		return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(LoveTropics.location(name)));
	}
}
