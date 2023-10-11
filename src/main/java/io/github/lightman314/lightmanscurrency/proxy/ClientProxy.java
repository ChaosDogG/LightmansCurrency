package io.github.lightman314.lightmanscurrency.proxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.data.*;
import io.github.lightman314.lightmanscurrency.client.gui.screen.*;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.*;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ItemEditWidget;
import io.github.lightman314.lightmanscurrency.client.renderer.LCItemRenderer;
import io.github.lightman314.lightmanscurrency.client.renderer.blockentity.*;
import io.github.lightman314.lightmanscurrency.client.renderer.blockentity.book.BookRenderer;
import io.github.lightman314.lightmanscurrency.client.renderer.blockentity.book.renderers.EnchantedBookRenderer;
import io.github.lightman314.lightmanscurrency.client.renderer.blockentity.book.renderers.NormalBookRenderer;
import io.github.lightman314.lightmanscurrency.common.bank.reference.BankReference;
import io.github.lightman314.lightmanscurrency.common.blockentity.CoinChestBlockEntity;
import io.github.lightman314.lightmanscurrency.common.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.common.core.*;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationData;
import io.github.lightman314.lightmanscurrency.common.player.LCAdminMode;
import io.github.lightman314.lightmanscurrency.common.playertrading.ClientPlayerTrade;
import io.github.lightman314.lightmanscurrency.common.events.NotificationEvent;
import io.github.lightman314.lightmanscurrency.common.items.CoinBlockItem;
import io.github.lightman314.lightmanscurrency.common.items.CoinItem;
import io.github.lightman314.lightmanscurrency.common.menus.PlayerTradeMenu;
import io.github.lightman314.lightmanscurrency.common.money.CoinData;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import io.github.lightman314.lightmanscurrency.integration.curios.client.LCCuriosClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;

public class ClientProxy extends CommonProxy{

	boolean openTeamManager = false;
	boolean openNotifications = false;
	private long timeOffset = 0;

	private final Supplier<CoinChestBlockEntity> coinChestBE = Suppliers.memoize(() -> new CoinChestBlockEntity(BlockPos.ZERO, ModBlocks.COIN_CHEST.get().defaultBlockState()));

	@Override
	public void setupClient() {
    	
    	//Register Screens
    	MenuScreens.register(ModMenus.ATM.get(), ATMScreen::new);
    	MenuScreens.register(ModMenus.MINT.get(), MintScreen::new);

		MenuScreens.register(ModMenus.NETWORK_TERMINAL.get(), NetworkTerminalScreen::new);
    	MenuScreens.register(ModMenus.TRADER.get(), TraderScreen::new);
    	MenuScreens.register(ModMenus.TRADER_BLOCK.get(), TraderScreen::new);
    	MenuScreens.register(ModMenus.TRADER_NETWORK_ALL.get(), TraderScreen::new);

    	MenuScreens.register(ModMenus.TRADER_STORAGE.get(), TraderStorageScreen::new);

    	MenuScreens.register(ModMenus.SLOT_MACHINE.get(), SlotMachineScreen::new);

    	MenuScreens.register(ModMenus.WALLET.get(), WalletScreen::new);
    	MenuScreens.register(ModMenus.WALLET_BANK.get(), WalletBankScreen::new);
    	MenuScreens.register(ModMenus.TICKET_MACHINE.get(), TicketMachineScreen::new);
    	
    	MenuScreens.register(ModMenus.TRADER_INTERFACE.get(), TraderInterfaceScreen::new);
    	
    	MenuScreens.register(ModMenus.TRADER_RECOVERY.get(), EjectionRecoveryScreen::new);

		MenuScreens.register(ModMenus.PLAYER_TRADE.get(), PlayerTradeScreen::new);

		MenuScreens.register(ModMenus.COIN_CHEST.get(), CoinChestScreen::new);

		MenuScreens.register(ModMenus.TAX_COLLECTOR.get(), TaxCollectorScreen::new);
    	
    	//Register Tile Entity Renderers
    	BlockEntityRenderers.register(ModBlockEntities.ITEM_TRADER.get(), ItemTraderBlockEntityRenderer::new);
    	BlockEntityRenderers.register(ModBlockEntities.FREEZER_TRADER.get(), FreezerTraderBlockEntityRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.SLOT_MACHINE_TRADER.get(), SlotMachineBlockEntityRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.BOOK_TRADER.get(), BookTraderBlockEntityRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.AUCTION_STAND.get(), AuctionStandBlockEntityRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.COIN_CHEST.get(), CoinChestRenderer::new);
		//BlockEntityRenderers.register(ModBlockEntities.TAX_BLOCK.get(), TaxBlockRenderer::new);

		//Setup Item Edit blacklists
		ItemEditWidget.BlacklistCreativeTabs(CreativeModeTabs.HOTBAR, CreativeModeTabs.INVENTORY, CreativeModeTabs.SEARCH, CreativeModeTabs.OP_BLOCKS);
		ItemEditWidget.BlacklistItem(ModItems.TICKET);
		ItemEditWidget.BlacklistItem(ModItems.TICKET_MASTER);
		//Add written book to Item Edit item list (for purchase/barter possibilities with NBT enforcement turned off)
		ItemEditWidget.AddExtraItemAfter(new ItemStack(Items.WRITTEN_BOOK), Items.WRITABLE_BOOK);

		//Setup Book Renderers
		BookRenderer.register(NormalBookRenderer.GENERATOR);
		BookRenderer.register(EnchantedBookRenderer.GENERATOR);

		//Setup custom item renderers
		LCItemRenderer.registerBlockEntitySource(this::checkForCoinChest);

		//Register Curios Render Layers
		if(LightmansCurrency.isCuriosLoaded())
			LCCuriosClient.registerRenderLayers();

	}

	private BlockEntity checkForCoinChest(Block block)
	{
		if(block == ModBlocks.COIN_CHEST.get())
			return coinChestBE.get();
		return null;
	}

	@Override
	public void clearClientTraders() { ClientTraderData.ClearTraders(); }
	
	@Override
	public void updateTrader(CompoundTag compound) { ClientTraderData.UpdateTrader(compound); }
	
	@Override
	public void removeTrader(long traderID) { ClientTraderData.RemoveTrader(traderID); }
	
	public void clearTeams() { ClientTeamData.ClearTeams(); }
	
	public void updateTeam(CompoundTag compound) { ClientTeamData.UpdateTeam(compound); }
	
	@Override
	public void removeTeam(long teamID) { ClientTeamData.RemoveTeam(teamID); }
	
	@Override
	public void initializeBankAccounts(CompoundTag compound)
	{
		if(compound.contains("BankAccounts", Tag.TAG_LIST))
		{
			Map<UUID,BankAccount> bank = new HashMap<>();
			ListTag bankList = compound.getList("BankAccounts", Tag.TAG_COMPOUND);
			for(int i = 0; i < bankList.size(); ++i)
			{
				CompoundTag tag = bankList.getCompound(i);
				UUID id = tag.getUUID("Player");
				BankAccount bankAccount = new BankAccount(tag);
				bank.put(id,bankAccount);
			}
			ClientBankData.InitBankAccounts(bank);
		}
	}
	
	@Override
	public void updateBankAccount(CompoundTag compound)
	{
		ClientBankData.UpdateBankAccount(compound);
	}
	
	@Override
	public void receiveEmergencyEjectionData(CompoundTag compound)
	{
		ClientEjectionData.UpdateEjectionData(compound);
	}
	
	@Override
	public void updateNotifications(NotificationData data)
	{
		ClientNotificationData.UpdateNotifications(data);
	}
	
	@Override
	public void receiveNotification(Notification notification)
	{
		
		Minecraft mc = Minecraft.getInstance();
		assert mc.player != null;
		if(MinecraftForge.EVENT_BUS.post(new NotificationEvent.NotificationReceivedOnClient(mc.player.getUUID(), ClientNotificationData.GetNotifications(), notification)))
			return;
		
		if(Config.CLIENT.pushNotificationsToChat.get()) //Post the notification to chat
			mc.gui.getChat().addMessage(notification.getChatMessage());
		
	}
	
	@Override
	public void receiveSelectedBankAccount(BankReference selectedAccount) { ClientBankData.UpdateLastSelectedAccount(selectedAccount); }

	@Override
	public void updateTaxEntries(CompoundTag compound) { ClientTaxData.UpdateEntry(compound); }

	@Override
	public void removeTaxEntry(long id) { ClientTaxData.RemoveEntry(id); }
	
	@Override
	public void openNotificationScreen() { this.openNotifications = true; }
	
	@Override
	public void openTeamManager() { this.openTeamManager = true; }
	
	@Override
	public void createTeamResponse(long teamID)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.screen instanceof TeamManagerScreen screen)
			screen.setActiveTeam(teamID);
	}
	
	@Override
	public long getTimeDesync()
	{
		return timeOffset;
	}
	
	@Override
	public void setTimeDesync(long serverTime)
	{
		this.timeOffset = serverTime - System.currentTimeMillis();
		//Round the time offset to the nearest second
		this.timeOffset = (timeOffset / 1000) * 1000;
		if(this.timeOffset < 10000) //Ignore offset if less than 10s, as it's likely due to ping
			this.timeOffset = 0;
	}
	
	@Override
	public void loadAdminPlayers(List<UUID> serverAdminList) { LCAdminMode.loadAdminPlayers(serverAdminList); }
	
	@SubscribeEvent
	public void openScreenOnRenderTick(RenderTickEvent event)
	{
		if(event.phase == TickEvent.Phase.START)
		{
			if(this.openTeamManager)
			{
				this.openTeamManager = false;
				Minecraft.getInstance().setScreen(new TeamManagerScreen());
			}
			else if(this.openNotifications)
			{
				this.openNotifications = false;
				//Open easy notification screen
				Minecraft.getInstance().setScreen(new NotificationScreen());
			}
		}
	}
	
	@SubscribeEvent
	//Add coin value tooltips to non CoinItem coins.
	public void onItemTooltip(ItemTooltipEvent event) {
		Item item = event.getItemStack().getItem();
		CoinData coinData = MoneyUtil.getData(item);
		if(coinData != null && !(item instanceof CoinItem || item instanceof CoinBlockItem))
		{
			CoinItem.addCoinTooltips(event.getItemStack(), event.getToolTip());
		}
	}
	
	@Override
	public void playCoinSound() {
		if(Config.CLIENT.moneyMendingClink.get())
		{
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.COINS_CLINKING.get(), 1f, 0.4f));
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) { ItemEditWidget.ConfirmItemListLoaded(); }

	@Override
	@Nonnull
	public Level safeGetDummyLevel() throws Exception {
		Level level = this.getDummyLevelFromServer();
		if(level == null)
			level = Minecraft.getInstance().level;
		if(level != null)
			return level;
		throw new Exception("Could not get dummy level from client, as there is no active level!");
	}

	@Override
	public void loadPlayerTrade(ClientPlayerTrade trade) {
		Minecraft mc = Minecraft.getInstance();
		if(mc.player.containerMenu instanceof PlayerTradeMenu menu)
			menu.reloadTrade(trade);
	}
	
}