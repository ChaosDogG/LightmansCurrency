package io.github.lightman314.lightmanscurrency.common.menus.slots.ticket;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.core.variants.Color;
import io.github.lightman314.lightmanscurrency.common.menus.slots.easy.EasyMultiBGSlot;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TicketModifierSlot extends EasyMultiBGSlot {

    public static final ResourceLocation EMPTY_DYE_SLOT =  new ResourceLocation(LightmansCurrency.MODID, "item/empty_dye_slot");

    public TicketModifierSlot(IInventory inventory, int index, int x, int y) { super(inventory, index, x, y); }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return stack.getItem() == ModItems.TICKET_MASTER.get() || InventoryUtil.ItemHasTag(stack, new ResourceLocation("forge","dyes"));
    }

    @Override
    protected List<Pair<ResourceLocation, ResourceLocation>> getPossibleNoItemIcons() {
        return ImmutableList.of(Pair.of(InventoryMenu.BLOCK_ATLAS,TicketSlot.EMPTY_TICKET_SLOT), Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_DYE_SLOT));
    }

    @Nullable
    public static Color getColorFromDye(ItemStack stack) {
        if(stack.isEmpty())
            return null;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("white")))
            return Color.WHITE;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("orange")))
            return Color.ORANGE;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("magenta")))
            return Color.MAGENTA;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("light_blue")))
            return Color.LIGHT_BLUE;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("yellow")))
            return Color.YELLOW;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("lime")))
            return Color.LIME;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("pink")))
            return Color.PINK;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("gray")))
            return Color.GRAY;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("light_gray")))
            return Color.LIGHT_GRAY;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("cyan")))
            return Color.CYAN;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("purple")))
            return Color.PURPLE;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("blue")))
            return Color.BLUE;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("brown")))
            return Color.BROWN;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("green")))
            return Color.GREEN;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("red")))
            return Color.RED;
        if(InventoryUtil.ItemHasTag(stack, getDyeTag("black")))
            return Color.BLACK;
        return null;
    }

    private static ResourceLocation getDyeTag(String color) { return new ResourceLocation("forge", "dyes/" + color); }

}