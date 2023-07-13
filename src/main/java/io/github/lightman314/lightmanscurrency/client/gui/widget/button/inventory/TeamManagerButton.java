package io.github.lightman314.lightmanscurrency.client.gui.widget.button.inventory;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.Sprite;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TeamManagerScreen;
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class TeamManagerButton extends InventoryButton {
	
	private static TeamManagerButton lastButton = null;
	
	public static final int SIZE = 9;

	public static final Sprite SPRITE = Sprite.SimpleSprite(TeamManagerScreen.GUI_TEXTURE, 200, 0, SIZE, SIZE);
	
	public static final ScreenPosition OFFSET = ScreenPosition.ZERO;

	
	public TeamManagerButton(AbstractContainerScreen<?> inventoryScreen) {
		super(inventoryScreen, button -> LightmansCurrency.PROXY.openTeamManager(), SPRITE);
		lastButton = this;
	}

	@Override
	protected ScreenPosition getPositionOffset(boolean isCreativeScreen) { return isCreativeScreen ? Config.CLIENT.notificationAndTeamButtonCreativePosition.get().offset(OFFSET) : Config.CLIENT.notificationAndTeamButtonPosition.get().offset(OFFSET); }
	
	public static void tryRenderTooltip(EasyGuiGraphics gui) {
		if(lastButton != null && lastButton.isMouseOver(gui.mousePos))
			gui.renderTooltip(EasyText.translatable("tooltip.button.team_manager"));
	}
	
}