package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.auction;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.CoinValueInput;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TimeInputWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyAddonHelper;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyTextButton;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.client.util.TextRenderUtil;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.player.LCAdminMode;
import io.github.lightman314.lightmanscurrency.common.traders.auction.tradedata.AuctionTradeData;
import io.github.lightman314.lightmanscurrency.common.menus.TraderMenu;
import io.github.lightman314.lightmanscurrency.common.menus.slots.SimpleSlot;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.auction.AuctionCreateTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.network.message.persistentdata.CPacketCreatePersistentAuction;
import io.github.lightman314.lightmanscurrency.network.packet.LazyPacketData;
import io.github.lightman314.lightmanscurrency.util.TimeUtil;
import io.github.lightman314.lightmanscurrency.util.TimeUtil.TimeData;
import io.github.lightman314.lightmanscurrency.util.TimeUtil.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

public class AuctionCreateClientTab extends TraderStorageClientTab<AuctionCreateTab> {

	public static final long CLOSE_DELAY = TimeUtil.DURATION_SECOND * 5;
	
	public AuctionCreateClientTab(Object screen, AuctionCreateTab commonTab) { super(screen, commonTab); }
	
	@Nonnull
	@Override
	public IconData getIcon() { return IconAndButtonUtil.ICON_PLUS; }
	
	@Override
	public MutableComponent getTooltip() { return EasyText.translatable("tooltip.lightmanscurrency.auction.create"); }
	
	@Override
	public boolean blockInventoryClosing() { return LCAdminMode.isAdminPlayer(this.screen.getMenu().player); }
	
	AuctionTradeData pendingAuction;
	
	TradeButton tradeDisplay;
	
	CoinValueInput priceSelect;
	EasyButton buttonTogglePriceMode;
	boolean startingBidMode = true;

	EasyButton buttonSubmitAuction;
	
	boolean locked = false;
	long successTime = 0;

	EasyButton buttonSubmitPersistentAuction;
	EditBox persistentAuctionIDInput;
	
	TimeInputWidget timeInput;
	
	@Override
	public void initialize(ScreenArea screenArea, boolean firstOpen) {

		if(firstOpen)
		{
			this.pendingAuction = new AuctionTradeData(this.menu.player);
			this.locked = false;
			this.successTime = 0;
			this.startingBidMode = true;
			this.commonTab.getAuctionItems().addListener(c -> this.UpdateAuctionItems());
		}
		
		this.tradeDisplay = this.addChild(new TradeButton(this.menu::getContext, () -> this.pendingAuction, b -> {}));
		this.tradeDisplay.setPosition(screenArea.pos.offset(15, 5));
		
		this.priceSelect = this.addChild(new CoinValueInput(screenArea.pos.offset(screenArea.width / 2 - CoinValueInput.DISPLAY_WIDTH / 2, 34), EasyText.empty(), CoinValue.EMPTY, this.getFont(), this::onPriceChanged));
		this.priceSelect.drawBG = this.priceSelect.allowFreeToggle = false;
		
		this.buttonTogglePriceMode = this.addChild(new EasyTextButton(screenArea.pos.offset(114, 5), screenArea.width - 119, 20, EasyText.translatable("button.lightmanscurrency.auction.toggleprice.startingbid"), b -> this.TogglePriceTarget()));
		
		//Duration Input
		this.timeInput = this.addChild(new TimeInputWidget(screenArea.pos.offset(80, 112), 10, TimeUnit.DAY, TimeUnit.HOUR, this::updateDuration));
		this.timeInput.minDuration = Math.max(Config.SERVER.minAuctionDuration.get() * TimeUtil.DURATION_DAY, TimeUtil.DURATION_HOUR);
		this.timeInput.maxDuration = Math.max(Config.SERVER.maxAuctionDuration.get(), Config.SERVER.minAuctionDuration.get()) * TimeUtil.DURATION_DAY;
		this.timeInput.setTime(this.timeInput.minDuration);
		
		//Submit Button
		this.buttonSubmitAuction = this.addChild(new EasyTextButton(screenArea.pos.offset(40,- 20), screenArea.width - 80, 20, EasyText.translatable("button.lightmanscurrency.auction.create"), b -> this.submitAuction()));
		this.buttonSubmitAuction.active = false;
		
		this.buttonSubmitPersistentAuction = this.addChild(new IconButton(screenArea.pos.offset(screenArea.width - 20, -20), this::submitPersistentAuction, IconAndButtonUtil.ICON_PERSISTENT_DATA)
				.withAddons(EasyAddonHelper.tooltip(IconAndButtonUtil.TOOLTIP_PERSISTENT_AUCTION)));
		this.buttonSubmitPersistentAuction.visible = LCAdminMode.isAdminPlayer(this.screen.getMenu().player);
		this.buttonSubmitPersistentAuction.active = false;
		
		int idWidth = this.getFont().width(EasyText.translatable("gui.lightmanscurrency.settings.persistent.id"));
		this.persistentAuctionIDInput = this.addChild((new EditBox(this.getFont(), screenArea.x + idWidth + 2, screenArea.y - 40, screenArea.width - idWidth - 2, 18, EasyText.empty())));
		this.persistentAuctionIDInput.visible = LCAdminMode.isAdminPlayer(this.screen.getMenu().player);
		
	}
	
	@Override
	public void closeAction() { this.commonTab.getAuctionItems().removeListener(c -> this.UpdateAuctionItems()); }
	
	@Override
	public void renderBG(@Nonnull EasyGuiGraphics gui) {

		gui.resetColor();
		for(SimpleSlot slot : this.commonTab.getSlots())
		{
			//Render Slot BG's
			gui.blit(TraderScreen.GUI_TEXTURE, slot.x - 1, slot.y - 1, TraderScreen.WIDTH, 0, 18, 18);
		}
		
		//Item Slot label
		gui.drawString(EasyText.translatable("gui.lightmanscurrency.auction.auctionitems"), TraderMenu.SLOT_OFFSET + 7, 112, 0x404040);
		
		if(this.locked && this.successTime != 0)
			TextRenderUtil.drawCenteredText(gui, EasyText.translatable("gui.lightmanscurrency.auction.create.success").withStyle(ChatFormatting.BOLD), this.screen.getXSize() / 2, 34, 0x404040);
		
		if(LCAdminMode.isAdminPlayer(this.screen.getMenu().player))
			gui.drawString(EasyText.translatable("gui.lightmanscurrency.settings.persistent.id"), 0, -35, 0xFFFFFF);
		
	}
	
	@Override
	public void tick() {
		this.priceSelect.locked = this.locked;
		this.priceSelect.tick();
		if(this.locked && this.successTime != 0)
		{
			if(TimeUtil.compareTime(CLOSE_DELAY, this.successTime))
			{
				this.screen.changeTab(TraderStorageTab.TAB_TRADE_BASIC);
				return;
			}
		}
		if(this.locked)
		{
			this.buttonTogglePriceMode.active = this.buttonSubmitAuction.active = false;
		}
		else
		{
			this.buttonTogglePriceMode.active = true;
			
			this.buttonSubmitAuction.active = this.pendingAuction.isValid();
		}
		
		if(LCAdminMode.isAdminPlayer(this.screen.getMenu().player))
		{
			this.buttonSubmitPersistentAuction.visible = this.persistentAuctionIDInput.visible = !this.locked;
			this.buttonSubmitPersistentAuction.active = this.pendingAuction.isValid();
		}
		else
			this.buttonSubmitPersistentAuction.visible = this.persistentAuctionIDInput.visible = false;
		
	}
	
	private void UpdateAuctionItems() {
		this.pendingAuction.setAuctionItems(this.commonTab.getAuctionItems());
	}
	
	private void onPriceChanged(CoinValue newPrice) {
		if(this.startingBidMode)
			this.pendingAuction.setStartingBid(newPrice);
		else
			this.pendingAuction.setMinBidDifferent(newPrice);
	}
	
	private void TogglePriceTarget() {
		this.startingBidMode = !this.startingBidMode;
		this.buttonTogglePriceMode.setMessage(EasyText.translatable(this.startingBidMode ? "button.lightmanscurrency.auction.toggleprice.startingbid" : "button.lightmanscurrency.auction.toggleprice.mindeltabid"));
		if(this.startingBidMode)
			this.priceSelect.setCoinValue(this.pendingAuction.getLastBidAmount());
		else
			this.priceSelect.setCoinValue(this.pendingAuction.getMinBidDifference());
	}
	
	
	
	private void updateDuration(TimeData newTime) {
		this.pendingAuction.setDuration(newTime.miliseconds);
	}
	
	private void submitAuction() {
		//LightmansCurrency.LogInfo("Sending Auction to the server!\n" + this.pendingAuction.getAsNBT().getAsString());
		this.commonTab.createAuction(this.pendingAuction);
		this.locked = true;
		for(SimpleSlot slot : this.commonTab.getSlots())
			slot.locked = true;
	}
	
	private void submitPersistentAuction(EasyButton button) {
		new CPacketCreatePersistentAuction(this.pendingAuction.getAsNBT(), this.persistentAuctionIDInput.getValue()).send();
	}
	
	@Override
	public void receiveServerMessage(LazyPacketData message) {
		if(message.contains("AuctionCreated"))
		{
			//LightmansCurrency.LogInfo("Received create response message from the server.\nAuction Created: " + message.getBoolean("AuctionCreated"));
			if(message.getBoolean("AuctionCreated"))
				this.successTime = TimeUtil.getCurrentTime();
			else
			{
				this.locked = false;
				for(SimpleSlot slot : this.commonTab.getSlots())
					slot.locked = false;
			}
		}
	}
	
}
