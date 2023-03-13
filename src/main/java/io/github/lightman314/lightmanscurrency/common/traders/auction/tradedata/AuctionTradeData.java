package io.github.lightman314.lightmanscurrency.common.traders.auction.tradedata;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.common.commands.CommandLCAdmin;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationSaveData;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseBidNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseBuyerNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseCancelNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseSellerNobidNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.auction.AuctionHouseSellerNotification;
import io.github.lightman314.lightmanscurrency.common.player.PlayerReference;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.auction.AuctionHouseTrader;
import io.github.lightman314.lightmanscurrency.common.traders.auction.AuctionPlayerStorage;
import io.github.lightman314.lightmanscurrency.common.traders.auction.PersistentAuctionData;
import io.github.lightman314.lightmanscurrency.common.traders.auction.tradedata.client.AuctionTradeButtonRenderer;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.client.TradeRenderManager;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.comparison.TradeComparisonResult;
import io.github.lightman314.lightmanscurrency.common.events.AuctionHouseEvent.AuctionEvent.AuctionCompletedEvent;
import io.github.lightman314.lightmanscurrency.common.events.AuctionHouseEvent.AuctionEvent.CancelAuctionEvent;
import io.github.lightman314.lightmanscurrency.common.menus.TraderStorageMenu.IClientMessage;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.trades_basic.BasicTradeEditTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.util.FileUtil;
import io.github.lightman314.lightmanscurrency.util.TimeUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;

public class AuctionTradeData extends TradeData {

	public static long GetMinimumDuration() {
		if(Config.SERVER.minAuctionDuration.get() > 0)
			return TimeUtil.DURATION_DAY * Config.SERVER.minAuctionDuration.get();
		return TimeUtil.DURATION_HOUR;
	}
	public static long GetDefaultDuration() {
		if(Config.SERVER.minAuctionDuration.get() > 0)
			return TimeUtil.DURATION_DAY * Config.SERVER.minAuctionDuration.get();
		return TimeUtil.DURATION_DAY;
	}

	public boolean hasBid() { return this.lastBidPlayer != null; }

	private boolean cancelled;

	private String persistentID = "";
	public boolean isPersistentID(String id) { return this.persistentID.equals(id); }

	CoinValue lastBidAmount = new CoinValue();
	public CoinValue getLastBidAmount() { return this.lastBidAmount; }
	PlayerReference lastBidPlayer = null;
	public PlayerReference getLastBidPlayer() { return this.lastBidPlayer; }

	public void setStartingBid(CoinValue amount) {
		if(this.isActive())
			return;
		this.lastBidAmount = amount.copy();
	}

	CoinValue minBidDifference = new CoinValue(1);
	public CoinValue getMinBidDifference() { return this.minBidDifference; }
	public void setMinBidDifferent(CoinValue amount) {
		if(this.isActive())
			return;
		this.minBidDifference = amount.copy();
		if(this.minBidDifference.getRawValue() <= 0)
			this.minBidDifference = new CoinValue(1);
	}
	PlayerReference tradeOwner;
	public PlayerReference getOwner() { return this.tradeOwner; }
	public boolean isOwner(PlayerEntity player) {
		return (this.tradeOwner != null && this.tradeOwner.is(player)) || CommandLCAdmin.isAdminPlayer(player);
	}

	long startTime = 0;
	long duration = 0;
	public void setDuration(long duration) {
		if(this.isActive())
			return;
		this.duration = Math.max(GetMinimumDuration(), duration);
	}

	List<ItemStack> auctionItems = new ArrayList<>();
	public List<ItemStack> getAuctionItems() { return this.auctionItems; }
	public void setAuctionItems(IInventory auctionItems) {
		if(this.isActive())
			return;
		this.auctionItems.clear();
		for(int i = 0; i < auctionItems.getContainerSize(); ++i)
		{
			ItemStack stack = auctionItems.getItem(i);
			if(!stack.isEmpty())
				this.auctionItems.add(stack.copy());
		}
	}

	public AuctionTradeData(PlayerEntity owner) { super(false); this.tradeOwner = PlayerReference.of(owner); this.setDuration(GetDefaultDuration()); }

	public AuctionTradeData(CompoundNBT compound) { super(false); this.loadFromNBT(compound); }

	/**
	 * Used to create an auction trade from persistent auction data
	 */
	public AuctionTradeData(PersistentAuctionData data) {
		super(false);
		this.persistentID = data.id;
		this.setDuration(data.duration);
		this.auctionItems = data.getAuctionItems();
		this.setStartingBid(data.getStartingBid());
		this.setMinBidDifferent(data.getMinimumBidDifference());
	}

	public boolean isActive() { return this.startTime != 0 && !this.cancelled; }

	@Override
	public boolean isValid() {
		if(this.cancelled)
			return false;
		if(this.auctionItems.size() == 0)
			return false;
		if(this.isActive() && this.hasExpired(TimeUtil.getCurrentTime()))
			return false;
		if(this.minBidDifference.getRawValue() <= 0)
			return false;
		return this.lastBidAmount.getRawValue() > 0;
	}

	public void startTimer() {
		if(!this.isActive())
			this.startTime = TimeUtil.getCurrentTime();
	}

	public long getRemainingTime(long currentTime) {
		if(!this.isActive())
			return this.duration;
		return Math.max(0, this.startTime + this.duration - currentTime);
	}

	public boolean hasExpired(long time) {
		if(this.isActive())
			return time >= this.startTime + this.duration;
		return false;
	}

	public boolean tryMakeBid(AuctionHouseTrader trader, PlayerEntity player, CoinValue amount) {
		if(!validateBidAmount(amount))
			return false;

		PlayerReference oldBidder = this.lastBidPlayer;

		if(this.lastBidPlayer != null)
		{
			//Refund the money to the previous bidder
			AuctionPlayerStorage storage = trader.getStorage(this.lastBidPlayer);
			storage.giveMoney(this.lastBidAmount);
			trader.markStorageDirty();
		}

		this.lastBidPlayer = PlayerReference.of(player);
		this.lastBidAmount = amount.copy();

		//Send notification to the previous bidder letting them know they've been out-bid.
		if(oldBidder != null)
			NotificationSaveData.PushNotification(oldBidder.id, new AuctionHouseBidNotification(this));

		return true;
	}

	public boolean validateBidAmount(CoinValue amount) {
		CoinValue minAmount = this.getMinNextBid();
		return amount.getRawValue() >= minAmount.getRawValue();
	}

	public CoinValue getMinNextBid() {
		return new CoinValue(this.lastBidPlayer == null ? this.lastBidAmount.getRawValue() : this.lastBidAmount.getRawValue() + this.minBidDifference.getRawValue());
	}

	public void ExecuteTrade(AuctionHouseTrader trader) {
		if(this.cancelled)
			return;
		this.cancelled = true;

		//Throw auction completed event
		AuctionCompletedEvent event = new AuctionCompletedEvent(trader, this);
		MinecraftForge.EVENT_BUS.post(event);

		if(this.lastBidPlayer != null)
		{
			AuctionPlayerStorage buyerStorage = trader.getStorage(this.lastBidPlayer);
			List<ItemStack> rewards = event.getItems();
			//Reward the items to the last bidder
			for (ItemStack reward : rewards) buyerStorage.giveItem(reward);
			//Give the bid money to the trades owner
			if(this.tradeOwner != null)
			{
				AuctionPlayerStorage sellerStorage = trader.getStorage(this.tradeOwner);
				sellerStorage.giveMoney(event.getPayment());
			}

			//Post notification to the auction winner
			NotificationSaveData.PushNotification(this.lastBidPlayer.id, new AuctionHouseBuyerNotification(this));

			//Post notification to the auction owner
			if(this.tradeOwner != null)
				NotificationSaveData.PushNotification(this.tradeOwner.id, new AuctionHouseSellerNotification(this));
		}
		else
		{
			//Nobody bid on the item(s), return the items to the auction owner
			if(this.tradeOwner != null)
			{
				AuctionPlayerStorage sellerStorage = trader.getStorage(this.tradeOwner);
				List<ItemStack> items = event.getItems();
				for (ItemStack item : items) sellerStorage.giveItem(item);

				//Post notification to the auction owner
				NotificationSaveData.PushNotification(this.tradeOwner.id, new AuctionHouseSellerNobidNotification(this));

			}
		}
	}

	public void CancelTrade(AuctionHouseTrader trader, boolean giveToPlayer, PlayerEntity player)
	{
		if(this.cancelled)
			return;
		this.cancelled = true;

		if(this.lastBidPlayer != null)
		{
			//Give a refund to the last bidder
			AuctionPlayerStorage buyerStorage = trader.getStorage(this.lastBidPlayer);
			buyerStorage.giveMoney(this.lastBidAmount);

			//Send cancel notification
			NotificationSaveData.PushNotification(this.lastBidPlayer.id, new AuctionHouseCancelNotification(this));

		}
		//Return the items being sold to their owner
		if(giveToPlayer)
		{
			//Return items to the player who cancelled the trade
			for(ItemStack stack : this.auctionItems) ItemHandlerHelper.giveItemToPlayer(player, stack);
		}
		else
		{
			//Return items to the trader owners storage. Ignore the player
			if(this.tradeOwner != null)
			{
				AuctionPlayerStorage sellerStorage = trader.getStorage(this.tradeOwner);
				for(ItemStack stack : this.auctionItems) sellerStorage.giveItem(stack);
			}
		}

		CancelAuctionEvent event = new CancelAuctionEvent(trader, this, player);
		MinecraftForge.EVENT_BUS.post(event);

	}

	@Override
	public CompoundNBT getAsNBT() {
		//Do not run super.getAsNBT() as we don't need to save the price or trade rules.
		CompoundNBT compound = new CompoundNBT();
		ListNBT itemList = new ListNBT();
		for (ItemStack auctionItem : this.auctionItems) {
			itemList.add(auctionItem.save(new CompoundNBT()));
		}
		compound.put("SellItems", itemList);
		this.lastBidAmount.save(compound, "LastBid");
		if(this.lastBidPlayer != null)
			compound.put("LastBidPlayer", this.lastBidPlayer.save());

		this.minBidDifference.save(compound, "MinBid");

		compound.putLong("StartTime", this.startTime);
		compound.putLong("Duration", this.duration);

		if(this.tradeOwner != null)
			compound.put("TradeOwner", this.tradeOwner.save());

		compound.putBoolean("Cancelled", this.cancelled);

		if(!this.persistentID.isEmpty())
			compound.putString("PersistentID", this.persistentID);

		return compound;
	}

	public JsonObject saveToJson(JsonObject json) {

		for(int i = 0; i < this.auctionItems.size(); ++i)
			json.add("Item" + (i + 1), FileUtil.convertItemStack(this.auctionItems.get(i)));

		json.addProperty("Duration", this.duration);

		json.add("StartingBid", this.lastBidAmount.toJson());

		json.add("MinimumBid", this.minBidDifference.toJson());

		return json;
	}

	@Override
	public void loadFromNBT(CompoundNBT compound) {
		//Do not run super.loadFromNBT() as we didn't save the default data in the first place
		ListNBT itemList = compound.getList("SellItems", Constants.NBT.TAG_COMPOUND);
		this.auctionItems.clear();
		for(int i = 0; i < itemList.size(); ++i)
		{
			ItemStack stack = ItemStack.of(itemList.getCompound(i));
			if(!stack.isEmpty())
				this.auctionItems.add(stack);
		}
		this.lastBidAmount.load(compound, "LastBid");
		if(compound.contains("LastBidPlayer"))
			this.lastBidPlayer = PlayerReference.load(compound.getCompound("LastBidPlayer"));
		else
			this.lastBidPlayer = null;

		this.minBidDifference.load(compound, "MinBid");

		this.startTime = compound.getLong("StartTime");
		this.duration = compound.getLong("Duration");

		if(compound.contains("TradeOwner", Constants.NBT.TAG_COMPOUND))
			this.tradeOwner = PlayerReference.load(compound.getCompound("TradeOwner"));

		this.cancelled = compound.getBoolean("Cancelled");

		if(compound.contains("PersistentID", Constants.NBT.TAG_STRING))
			this.persistentID = compound.getString("PersistentID");

	}

	@Override
	public TradeRenderManager<?> getButtonRenderer() { return new AuctionTradeButtonRenderer(this); }

	@Override
	public void onInputDisplayInteraction(BasicTradeEditTab tab, IClientMessage clientHandler, int index, int button, ItemStack heldItem) { this.openCancelAuctionTab(tab); }

	@Override
	public void onOutputDisplayInteraction(BasicTradeEditTab tab, IClientMessage clientHandler, int index, int button, ItemStack heldItem) { this.openCancelAuctionTab(tab); }

	@Override
	public void onInteraction(BasicTradeEditTab tab, IClientMessage clientHandler, int mouseX, int mouseY, int button, ItemStack heldItem) { this.openCancelAuctionTab(tab); }

	private void openCancelAuctionTab(BasicTradeEditTab tab) {

		TraderData t = tab.menu.getTrader();
		if(t instanceof AuctionHouseTrader)
		{
			AuctionHouseTrader trader = (AuctionHouseTrader)t;
			int tradeIndex = trader.getTradeIndex(this);
			if(tradeIndex < 0)
				return;

			CompoundNBT extraData = new CompoundNBT();
			extraData.putInt("TradeIndex", tradeIndex);
			tab.sendOpenTabMessage(TraderStorageTab.TAB_TRADE_ADVANCED, extraData);

		}

	}

	@Override
	public TradeDirection getTradeDirection() {
		return TradeDirection.NONE;
	}

	@Override
	public TradeComparisonResult compare(TradeData otherTrade) { return new TradeComparisonResult(); }

	@Override
	public boolean AcceptableDifferences(TradeComparisonResult result) { return false; }

	@Override
	public List<ITextComponent> GetDifferenceWarnings(TradeComparisonResult differences) { return new ArrayList<>(); }

}