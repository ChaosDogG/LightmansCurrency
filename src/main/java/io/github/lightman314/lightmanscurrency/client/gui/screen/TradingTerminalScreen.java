package io.github.lightman314.lightmanscurrency.client.gui.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.ClientTradingOffice;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.IconButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.UniversalTraderButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.common.universal_traders.data.UniversalTraderData;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.universal_trader.MessageOpenTrades2;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

public class TradingTerminalScreen extends Screen{
	
	private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(LightmansCurrency.MODID, "textures/gui/trader_selection.png");
	private static final Comparator<UniversalTraderData> TRADER_SORTER = new TraderSorter();
	
	private int xSize = 176;
	private int ySize = 187;
	
	Player player;
	
	private EditBox searchField;
	private static int page = 0;
	
	Button buttonNextPage;
	Button buttonPreviousPage;
	List<UniversalTraderButton> traderButtons;
	
	private List<UniversalTraderData> traderList(){
		List<UniversalTraderData> traderList = ClientTradingOffice.getTraderList();
		traderList.sort(TRADER_SORTER);
		return traderList;
	}
	private List<UniversalTraderData> filteredTraderList = new ArrayList<>();
	
	public TradingTerminalScreen(Player player)
	{
		super(new TranslatableComponent("block.lightmanscurrency.terminal"));
		this.player = player;
	}
	
	@Override
	protected void init()
	{
		
		super.init();
		
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		
		this.searchField = this.addRenderableWidget(new EditBox(this.font, guiLeft + 28, guiTop + 6, 101, 9, new TranslatableComponent("gui.lightmanscurrency.terminal.search")));
		this.searchField.setBordered(false);;
		this.searchField.setMaxLength(32);
		this.searchField.setTextColor(0xFFFFFF);
		
		this.buttonPreviousPage = this.addRenderableWidget(new IconButton(guiLeft - 6, guiTop + 18, this::PreviousPage, this.font, IconData.of(GUI_TEXTURE, this.xSize, 0)));
		this.buttonNextPage = this.addRenderableWidget(new IconButton(guiLeft + this.xSize - 14, guiTop + 18, this::NextPage, this.font, IconData.of(GUI_TEXTURE, this.xSize + 16, 0)));
		
		this.initTraderButtons(guiLeft, guiTop);
		
		page = MathUtil.clamp(page, 0, this.pageLimit());
		
		this.tick();
		
		this.updateTraderList();
		
	}
	
	@Override
	public boolean isPauseScreen() { return false; }
	
	private void initTraderButtons(int guiLeft, int guiTop)
	{
		this.traderButtons = new ArrayList<>();
		for(int y = 0; y < 5; y++)
		{
			UniversalTraderButton newButton = this.addRenderableWidget(new UniversalTraderButton(guiLeft + 15, guiTop + 18 + (y * UniversalTraderButton.HEIGHT), this::OpenTrader, this.font));
			this.traderButtons.add(newButton);
		}
	}
	
	@Override
	public void tick()
	{
		super.tick();
		this.searchField.tick();
		if(this.buttonPreviousPage != null)
		this.buttonPreviousPage.visible = this.pageLimit() > 0;
		this.buttonPreviousPage.active = page > 0;
		this.buttonNextPage.visible = this.pageLimit() > 0;
		this.buttonNextPage.active = page < this.pageLimit();
	}
	
	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
	{
		if(this.minecraft == null)
			this.minecraft = Minecraft.getInstance();
		
		this.renderBackground(poseStack);
		
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GUI_TEXTURE);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		
		int startX = (this.width - this.xSize) / 2;
		int startY = (this.height - this.ySize) / 2;
		//Render the background
		this.blit(poseStack, startX, startY, 0, 0, this.xSize, this.ySize);
		
		super.render(poseStack, mouseX, mouseY, partialTicks);
		
	}
	
	@Override
	public boolean charTyped(char c, int code)
	{
		String s = this.searchField.getValue();
		if(this.searchField.charTyped(c, code))
		{
			if(!Objects.equals(s, this.searchField.getValue()))
			{
				this.updateTraderList();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean keyPressed(int key, int scanCode, int mods)
	{
		String s = this.searchField.getValue();
		if(this.searchField.keyPressed(key, scanCode, mods))
		{
			if(!Objects.equals(s,  this.searchField.getValue()))
			{
				this.updateTraderList();
			}
			return true;
		}
		return this.searchField.isFocused() && this.searchField.isVisible() && key != GLFW_KEY_ESCAPE || super.keyPressed(key, scanCode, mods);
	}
	
	private void PreviousPage(Button button)
	{
		if(page > 0)
		{
			page--;
			this.updateTraderButtons();
		}
	}
	
	private void NextPage(Button button)
	{
		if(page < this.pageLimit())
		{
			page++;
			this.updateTraderButtons();
		}
	}
	
	private void OpenTrader(Button button)
	{
		int index = getTraderIndex(button);
		if(index >= 0 && index < this.filteredTraderList.size())
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenTrades2(this.filteredTraderList.get(index).getTraderID()));
		}
	}
	
	private int getTraderIndex(Button button)
	{
		if(!traderButtons.contains(button))
			return -1;
		int index = traderButtons.indexOf(button);
		index += page * this.traderButtons();
		return index;
	}
	
	private int pageLimit()
	{
		return (this.filteredTraderList.size() - 1) / this.traderButtons();
	}
	
	private int traderButtons()
	{
		return this.traderButtons.size();
	}
	
	
	private void updateTraderList()
	{
		//Filtering of results moved to the TradingOffice.filterTraders
		this.filteredTraderList = TradingOffice.filterTraders(this.searchField.getValue(), this.traderList());
		this.updateTraderButtons();
		//Limit the page
		if(page > pageLimit())
			page = pageLimit();
	}
	
	private void updateTraderButtons()
	{
		int startIndex = page * this.traderButtons();
		for(int i = 0; i < this.traderButtons.size(); i++)
		{
			if(startIndex + i < this.filteredTraderList.size())
				this.traderButtons.get(i).SetData(this.filteredTraderList.get(startIndex + i));
			else
				this.traderButtons.get(i).SetData(null);
		}
	}
	
	private static class TraderSorter implements Comparator<UniversalTraderData>
	{

		@Override
		public int compare(UniversalTraderData a, UniversalTraderData b) {
			
			//(lowercase since lowercase letters apparently get sorted after uppercase letters)
			//Sort by trader name
			int sort = a.getName().getString().toLowerCase().compareTo(b.getName().getString().toLowerCase());
			//Sort by owner name if trader name is equal
			if(sort == 0)
				sort = a.getCoreSettings().getOwner().lastKnownName().toLowerCase().compareTo(b.getCoreSettings().getOwner().lastKnownName().toLowerCase());
			return sort;
		}
		
	}

}