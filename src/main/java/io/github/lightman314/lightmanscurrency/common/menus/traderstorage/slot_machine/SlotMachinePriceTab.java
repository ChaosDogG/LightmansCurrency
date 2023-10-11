package io.github.lightman314.lightmanscurrency.common.menus.traderstorage.slot_machine;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.slot_machine.SlotMachinePriceClientTab;
import io.github.lightman314.lightmanscurrency.common.menus.TraderStorageMenu;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.common.traders.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.common.traders.slot_machine.SlotMachineTraderData;
import io.github.lightman314.lightmanscurrency.network.packet.LazyPacketData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

public class SlotMachinePriceTab extends TraderStorageTab {


    public SlotMachinePriceTab(TraderStorageMenu menu) { super(menu); }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Object createClientTab(Object screen) { return new SlotMachinePriceClientTab(screen, this); }

    @Override
    public boolean canOpen(Player player) { return this.menu.hasPermission(Permissions.EDIT_TRADES); }

    @Override
    public void onTabOpen() { }

    @Override
    public void onTabClose() { }

    @Override
    public void addStorageMenuSlots(Function<Slot, Slot> addSlot) { }

    public void SetPrice(CoinValue newPrice)
    {
        if(this.menu.hasPermission(Permissions.EDIT_TRADES) && this.menu.getTrader() instanceof SlotMachineTraderData trader)
        {
            trader.setPrice(newPrice);
            if(this.menu.isClient())
                this.menu.SendMessage(LazyPacketData.simpleTag("SetPrice", newPrice.save()));
        }
    }

    @Override
    public void receiveMessage(LazyPacketData message) {
        if(message.contains("SetPrice"))
        {
            this.SetPrice(CoinValue.load(message.getNBT("SetPrice")));
        }
    }

}
