package io.github.lightman314.lightmanscurrency.integration.alexsmobs;

import io.github.lightman314.lightmanscurrency.common.events.DroplistConfigEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Mostly just adds their mobs to the default coin drop configs.
 */
@Mod.EventBusSubscriber
public class LCAlexsMobs {

    public static boolean isLoaded() { return ModList.get().isLoaded("alexsmobs"); }

    @SubscribeEvent
    public static void AddEntityLoot(DroplistConfigEvent.Entity event)
    {
        event.setDefaultNamespace("alexsmobs");
        switch (event.getTier())
        {
            case T1 -> {
                event.addEntry("cosmic_cod");
                event.addEntry("fly");
                event.addEntry("raccoon");
                event.addEntry("stradpole");
            }
            case T2 -> {
                event.addEntry("bone_serpent");
                event.addEntry("anaconda");
                event.addEntry("froststalker");
                event.addEntry("rattlesnake");
                event.addEntry("rockey_roller");
                event.addEntry("skreecher");
                event.addEntry("soul_vulture");
                event.addEntry("tarantula_hawk");
            }
            case T3 -> {
                event.addEntry("crimson_mosquito");
                event.addEntry("dropbear");
                event.addEntry("guster");
                event.addEntry("skelewag");
                event.addEntry("tusklin");
            }
            case T4 -> {
                event.addEntry("enderiophage");
                event.addEntry("farseer");
                event.addEntry("murmur");
                event.addEntry("straddler");
            }
            case T6 -> event.addEntry("mimicube");
            case BOSS_T6 -> event.addEntry("void_worm");
        }
        event.resetDefaultNamespace();
    }

}