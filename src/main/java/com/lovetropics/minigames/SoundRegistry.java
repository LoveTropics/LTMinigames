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

	public static final Holder<SoundEvent> generic_no_1 = register("escape_race.generic.no.1");
	public static final Holder<SoundEvent> generic_no_2 = register("escape_race.generic.no.2");
	public static final Holder<SoundEvent> generic_no_3 = register("escape_race.generic.no.3");
	public static final Holder<SoundEvent> generic_no_4 = register("escape_race.generic.no.4");
	public static final Holder<SoundEvent> generic_yes_1 = register("escape_race.generic.yes.1");
	public static final Holder<SoundEvent> generic_yes_2 = register("escape_race.generic.yes.2");
	public static final Holder<SoundEvent> generic_yes_3 = register("escape_race.generic.yes.3");
	public static final Holder<SoundEvent> generic_yes_4 = register("escape_race.generic.yes.4");
	public static final Holder<SoundEvent> room_1_generic = register("escape_race.room_1.generic");
	public static final Holder<SoundEvent> room_1_intro_1 = register("escape_race.room_1.intro.1");
	public static final Holder<SoundEvent> room_5_4 = register("escape_race.room_5.4");
	public static final Holder<SoundEvent> warehouse_2 = register("escape_race.warehouse.2");
	public static final Holder<SoundEvent> warehouse_3 = register("escape_race.warehouse.3");
	public static final Holder<SoundEvent> warehouse_4 = register("escape_race.warehouse.4");
	public static final Holder<SoundEvent> warehouse_5 = register("escape_race.warehouse.5");
	public static final Holder<SoundEvent> warehouse_6 = register("escape_race.warehouse.6");
	public static final Holder<SoundEvent> warehouse_7 = register("escape_race.warehouse.7");
	public static final Holder<SoundEvent> warehouse_8 = register("escape_race.warehouse.8");
	public static final Holder<SoundEvent> warehouse_9 = register("escape_race.warehouse.9");
	public static final Holder<SoundEvent> warehouse_10 = register("escape_race.warehouse.10");
	public static final Holder<SoundEvent> warehouse_11 = register("escape_race.warehouse.11");
	public static final Holder<SoundEvent> warehouse_12 = register("escape_race.warehouse.12");
	public static final Holder<SoundEvent> warehouse_13 = register("escape_race.warehouse.13");
	public static final Holder<SoundEvent> warehouse_14 = register("escape_race.warehouse.14");
	public static final Holder<SoundEvent> warehouse_15 = register("escape_race.warehouse.15");
	public static final Holder<SoundEvent> warehouse_16 = register("escape_race.warehouse.16");
	public static final Holder<SoundEvent> warehouse_17 = register("escape_race.warehouse.17");

	public static final Holder<SoundEvent> terry_trash_sound_1 = register("escape_race.terry_trash.sound_1");
	public static final Holder<SoundEvent> terry_trash_sound_2 = register("escape_race.terry_trash.sound_2");
	public static final Holder<SoundEvent> terry_trash_sound_3 = register("escape_race.terry_trash.sound_3");
	public static final Holder<SoundEvent> terry_trash_sound_4 = register("escape_race.terry_trash.sound_4");
	public static final Holder<SoundEvent> terry_trash_sound_5 = register("escape_race.terry_trash.sound_5");
	public static final Holder<SoundEvent> terry_trash_sound_6 = register("escape_race.terry_trash.sound_6");

	public static final Holder<SoundEvent> escape_race_room_5_sound_1 = register("escape_race.room_5.sound_1");
	public static final Holder<SoundEvent> escape_race_room_5_sound_2 = register("escape_race.room_5.sound_2");
	public static final Holder<SoundEvent> escape_race_room_5_sound_3 = register("escape_race.room_5.sound_3");
	public static final Holder<SoundEvent> escape_race_room_5_sound_4 = register("escape_race.room_5.sound_4");
	public static final Holder<SoundEvent> escape_race_room_5_sound_5 = register("escape_race.room_5.sound_5");
	public static final Holder<SoundEvent> escape_race_room_5_sound_6 = register("escape_race.room_5.sound_6");
	public static final Holder<SoundEvent> escape_race_room_5_izzy_sound_1 = register("escape_race.room_5.izzy.sound_1");
	public static final Holder<SoundEvent> escape_race_room_5_izzy_sound_2 = register("escape_race.room_5.izzy.sound_2");

	public static final Holder<SoundEvent> escape_race_billy_sound_0 = register("escape_race.bill_shoebill.00_hmm_i_wonder");
	public static final Holder<SoundEvent> escape_race_billy_sound_1 = register("escape_race.bill_shoebill.01_oh_hello_there");
	public static final Holder<SoundEvent> escape_race_billy_sound_2 = register("escape_race.bill_shoebill.02_oh_sorry");
	public static final Holder<SoundEvent> escape_race_billy_sound_3 = register("escape_race.bill_shoebill.03_i_can_t_possibly");
	public static final Holder<SoundEvent> escape_race_billy_sound_4 = register("escape_race.bill_shoebill.04_anyway_find_me");
	public static final Holder<SoundEvent> escape_race_billy_sound_5 = register("escape_race.bill_shoebill.05_these_are_far_too_plane");
	public static final Holder<SoundEvent> escape_race_billy_sound_6 = register("escape_race.bill_shoebill.06_ouchies");
	public static final Holder<SoundEvent> escape_race_billy_sound_7 = register("escape_race.bill_shoebill.07_these_are_too_sparkly");
	public static final Holder<SoundEvent> escape_race_billy_sound_8 = register("escape_race.bill_shoebill.08_scaly_shoes_");
	public static final Holder<SoundEvent> escape_race_billy_sound_9 = register("escape_race.bill_shoebill.09_hmm_no_these");
	public static final Holder<SoundEvent> escape_race_billy_sound_10 = register("escape_race.bill_shoebill.10_nuh_uh_uh");
	public static final Holder<SoundEvent> escape_race_billy_sound_11 = register("escape_race.bill_shoebill.11_huh_what_s_this");
	public static final Holder<SoundEvent> escape_race_billy_sound_12 = register("escape_race.bill_shoebill.12_now_these");

	public static final Holder<SoundEvent> escape_race_room_7_race_room_2 = register("escape_race.room_7.2");
	public static final Holder<SoundEvent> escape_race_room_7_race_room_3 = register("escape_race.room_7.3");
	public static final Holder<SoundEvent> escape_race_room_7_race_room_4 = register("escape_race.room_7.4");

	public static final Holder<SoundEvent> escape_race_final_1 = register("escape_race.final.1");
	public static final Holder<SoundEvent> escape_race_final_2 = register("escape_race.final.2");
	public static final Holder<SoundEvent> escape_race_final_3 = register("escape_race.final.3");
	public static final Holder<SoundEvent> escape_race_final_4 = register("escape_race.final.4");
	public static final Holder<SoundEvent> escape_race_final_5 = register("escape_race.final.5");
	public static final Holder<SoundEvent> escape_race_final_6 = register("escape_race.final.6");
	public static final Holder<SoundEvent> escape_race_final_7 = register("escape_race.final.7");
	public static final Holder<SoundEvent> escape_race_room_1_win = register("escape_race.room_1.win");
	public static final Holder<SoundEvent> escape_race_room_3_intro_1 = register("escape_race.room_3.intro.1");
	public static final Holder<SoundEvent> escape_race_room_3_intro_2 = register("escape_race.room_3.intro.2");
	public static final Holder<SoundEvent> escape_race_room_4_1 = register("escape_race.room_4.1");
	public static final Holder<SoundEvent> escape_race_room_4_2 = register("escape_race.room_4.2");
	public static final Holder<SoundEvent> escape_race_room_4_3 = register("escape_race.room_4.3");
	public static final Holder<SoundEvent> escape_race_room_4_4 = register("escape_race.room_4.4");
	public static final Holder<SoundEvent> escape_race_room_7_1 = register("escape_race.room_7.1");
	public static final Holder<SoundEvent> escape_race_room_7_5 = register("escape_race.room_7.5");
	public static final Holder<SoundEvent> escape_race_room_7_6 = register("escape_race.room_7.6");
	public static final Holder<SoundEvent> escape_race_room_7_7 = register("escape_race.room_7.7");
	public static final Holder<SoundEvent> escape_race_room_7_8 = register("escape_race.room_7.8");
	public static final Holder<SoundEvent> escape_race_room_7_9 = register("escape_race.room_7.9");
	public static final Holder<SoundEvent> escape_race_room_7_10 = register("escape_race.room_7.10");

	public static final Holder<SoundEvent> UPSET_STOMACH_FART = register("upset_stomach.fart");

	private static Holder<SoundEvent> register(String name) {
		return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(LoveTropics.location(name)));
	}
}
