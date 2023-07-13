package io.github.lightman314.lightmanscurrency.client.gui.easy.interfaces;

import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Player;

public interface IEasyScreen {

    default int getGuiLeft() { return this.getArea().x; }
    default int getGuiTop() { return this.getArea().y; }
    default ScreenPosition getCorner() { return this.getArea().pos; }
    default int getXSize() { return this.getArea().width; }
    default int getYSize() { return this.getArea().height; }
    ScreenArea getArea();

    Font getFont();
    Player getPlayer();

    <W> W addChild(W child);
    void removeChild(Object child);

    default boolean blockInventoryClosing() { return false; }


}
