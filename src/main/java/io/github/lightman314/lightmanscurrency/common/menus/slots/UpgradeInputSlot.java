package io.github.lightman314.lightmanscurrency.common.menus.slots;

import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.items.UpgradeItem;
import io.github.lightman314.lightmanscurrency.common.upgrades.UpgradeType.IUpgradeable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UpgradeInputSlot extends SimpleSlot{

	public static final ResourceLocation EMPTY_UPGRADE_SLOT = new ResourceLocation(LightmansCurrency.MODID, "item/empty_upgrade_slot");
	
	private final IUpgradeable machine;
	private final Runnable onModified;

	public UpgradeInputSlot(Container inventory, int index, int x, int y, IUpgradeable machine) { this(inventory, index, x, y, machine, (Runnable)() -> {}); }

	@Deprecated(since = "2.1.1.4")
	public UpgradeInputSlot(Container inventory, int index, int x, int y, IUpgradeable machine, IMessage onModified) { this(inventory, index, x, y, machine, (Runnable)onModified::accept); }

	public UpgradeInputSlot(Container inventory, int index, int x, int y, IUpgradeable machine, Runnable onModified)
	{
		super(inventory, index, x, y);
		this.machine = machine;
		this.onModified = onModified;
	}
	
	@Override
	public boolean mayPlace(@NotNull ItemStack stack)
	{
		if(stack.getItem() instanceof UpgradeItem item)
			return machine.allowUpgrade(item);
		return false;
	}
	
	@Override
	public int getMaxStackSize() { return 1; }
	
	@Override
	public void setChanged() {
		super.setChanged();
		if(this.onModified != null)
			this.onModified.run();
	}

	@Deprecated(since = "2.1.1.4")
	public interface IMessage {  void accept(); }
	
	@Override
	public Pair<ResourceLocation,ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_UPGRADE_SLOT);
	}
	
}
