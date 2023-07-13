package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderinterface;

import java.util.List;

import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderInterfaceScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TradeButtonArea;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.menus.traderinterface.TraderInterfaceClientTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderinterface.TraderInterfaceTab;
import io.github.lightman314.lightmanscurrency.common.menus.traderinterface.base.TradeSelectTab;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

public class TradeSelectClientTab extends TraderInterfaceClientTab<TradeSelectTab> {

	public TradeSelectClientTab(TraderInterfaceScreen screen, TradeSelectTab commonTab) { super(screen, commonTab); }

	@Nonnull
	@Override
	public IconData getIcon() { return IconData.of(ModItems.TRADING_CORE); }

	@Override
	public MutableComponent getTooltip() { return EasyText.translatable("tooltip.lightmanscurrency.interface.trade"); }

	@Override
	public boolean tabButtonVisible() { return this.commonTab.canOpen(this.menu.player); }
	
	TradeButtonArea tradeDisplay;
	
	@Override
	public void initialize(ScreenArea screenArea, boolean firstOpen) {
		
		this.tradeDisplay = this.addChild(new TradeButtonArea(this.menu.getBE()::getTrader, trader -> this.menu.getBE().getTradeContext(), this.screen.getGuiLeft() + 3, this.screen.getGuiTop() + 17, this.screen.getXSize() - 6, 100, this::SelectTrade, TradeButtonArea.FILTER_VALID));
		this.tradeDisplay.setSelectionDefinition(this::isTradeSelected);
		this.tradeDisplay.withTitle(this.screen.getCorner().offset(8,6), this.screen.getXSize() - 16, false);
		
	}

	@Override
	public void renderBG(@Nonnull EasyGuiGraphics gui) { }
	
	@Override
	public void tick() {
		if(!this.commonTab.canOpen(this.menu.player))
			this.screen.changeTab(TraderInterfaceTab.TAB_INFO);
	}
	
	private boolean isTradeSelected(TraderData trader, TradeData trade) {
		return this.menu.getBE().getTrueTrade() == trade;
	}
	
	private int getTradeIndex(TraderData trader, TradeData trade) {
		List<? extends TradeData> trades = trader.getTradeData();
		if(trades != null)
			return trades.indexOf(trade);
		return -1;
	}
	
	private void SelectTrade(TraderData trader, TradeData trade) {
		
		this.commonTab.setTradeIndex(this.getTradeIndex(trader, trade));
		
	}

}
