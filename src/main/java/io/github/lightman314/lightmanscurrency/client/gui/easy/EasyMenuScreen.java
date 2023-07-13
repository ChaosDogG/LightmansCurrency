package io.github.lightman314.lightmanscurrency.client.gui.easy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lightman314.lightmanscurrency.client.gui.easy.interfaces.*;
import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyWidgetWithChildren;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.easy.IEasyTickable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class EasyMenuScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements IEasyScreen {

    private final List<IPreRender> preRenders = new ArrayList<>();
    private final List<IEasyTickable> guiTickers = new ArrayList<>();
    private final List<ITooltipSource> tooltipSources = new ArrayList<>();
    private final List<IScrollListener> scrollListeners = new ArrayList<>();
    private final List<IMouseListener> mouseListeners = new ArrayList<>();


    @Override
    public Font getFont() { return this.font; }
    @Override
    public Player getPlayer() { return this.minecraft.player; }

    ScreenArea screenArea = ScreenArea.of(0, 0, 100, 100);

    protected EasyMenuScreen(T menu, Inventory inventory) { this(menu, inventory, EasyText.empty()); }
    protected EasyMenuScreen(T menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override
    public final ScreenArea getArea() { return this.screenArea; }
    public final int getGuiLeft() { return this.screenArea.x; }
    public final  int getGuiTop() { return this.screenArea.y; }
    public final  ScreenPosition getCorner() { return this.screenArea.pos; }
    public final  int getXSize() { return this.screenArea.width; }
    public final  int getYSize() { return this.screenArea.height; }

    protected final ScreenArea resize(int width, int height)
    {
        this.screenArea = this.screenArea.ofSize(width, height);
        this.recalculateCorner();
        this.leftPos = this.screenArea.x;
        this.topPos = this.screenArea.y;
        this.imageWidth = this.screenArea.width;
        this.imageHeight = this.screenArea.height;
        return this.screenArea;
    }

    protected void recalculateCorner() { this.screenArea = this.screenArea.atPosition(ScreenPosition.of((this.width - this.screenArea.width) / 2,(this.height - this.screenArea.height) / 2)); this.topPos = this.screenArea.y; this.leftPos = this.screenArea.x; }

    @Override
    protected final void init() {
        this.preRenders.clear();
        this.guiTickers.clear();
        this.tooltipSources.clear();
        this.scrollListeners.clear();
        this.mouseListeners.clear();
        this.recalculateCorner();
        this.initialize(this.screenArea);
    }



    protected abstract void initialize(ScreenArea screenArea);

    @Override
    public final void render(@Nonnull PoseStack pose, int mouseX, int mouseY, float partialTicks) {
        this.renderTick();
        EasyGuiGraphics gui = EasyGuiGraphics.create(pose, this.font, mouseX, mouseY, partialTicks).pushOffset(this.getCorner());
        //Trigger Pre-Render ticks
        for(IPreRender r : this.preRenders)
            r.preRender(gui);
        //Render background tint
        this.renderBackground(pose);
        //Render Background
        this.renderBG(gui);
        //Render Widgets, Slots, etc.
        super.render(pose, mouseX, mouseY, partialTicks);
        //Render After Widgets
        this.renderAfterWidgets(gui);
        //Render Tooltips
        this.renderTooltip(pose, mouseX, mouseY);
        EasyScreenHelper.RenderTooltips(gui, this.tooltipSources);
        //Render After Tooltips
        this.renderAfterTooltips(gui);
    }

    protected void renderTick() {}

    @Override
    protected final void renderBg(@Nonnull PoseStack pose, float partialTicks, int mouseX, int mouseY) { }
    protected abstract void renderBG(@Nonnull EasyGuiGraphics gui);

    //Don't render labels using the vanilla method
    @Override
    protected final void renderLabels(@Nonnull PoseStack pose, int mouseX, int mouseY) { }

    protected void renderAfterWidgets(@Nonnull EasyGuiGraphics gui) {}

    protected void renderAfterTooltips(@Nonnull EasyGuiGraphics gui) {}

    @Override
    public final <W> W addChild(W child) {
        if(child instanceof EasyWidgetWithChildren w)
        {
            w.pairWithScreen(this::addChild, this::removeChild);
            if(w.addChildrenBeforeThis())
                w.addChildren();
        }
        if(child instanceof Widget r && !this.renderables.contains(child))
            this.renderables.add(r);
        if(child instanceof GuiEventListener && child instanceof NarratableEntry)
            super.addWidget((GuiEventListener & NarratableEntry)child);
        IEasyTickable ticker = EasyScreenHelper.getWidgetTicker(child);
        if(ticker != null && !this.guiTickers.contains(ticker))
            this.guiTickers.add(ticker);
        if(child instanceof ITooltipSource t && !this.tooltipSources.contains(t))
            this.tooltipSources.add(t);
        if(child instanceof IMouseListener l && !this.mouseListeners.contains(l))
            this.mouseListeners.add(l);
        if(child instanceof IScrollListener l && !this.scrollListeners.contains(l))
            this.scrollListeners.add(l);
        if(child instanceof IPreRender r && !this.preRenders.contains(r))
            this.preRenders.add(r);
        if(child instanceof EasyWidget w)
            w.addAddons(this::addChild);
        if(child instanceof EasyWidgetWithChildren w && !w.addChildrenBeforeThis())
            w.addChildren();
        return child;
    }

    @Override
    public final void removeChild(Object child) {
        if(child instanceof Widget r)
            this.renderables.remove(r);
        if(child instanceof GuiEventListener l)
            super.removeWidget(l);
        IEasyTickable ticker = EasyScreenHelper.getWidgetTicker(child);
        this.guiTickers.remove(ticker);
        if(child instanceof ITooltipSource t)
            this.tooltipSources.remove(t);
        if(child instanceof IMouseListener l)
            this.mouseListeners.remove(l);
        if(child instanceof IScrollListener l)
            this.scrollListeners.remove(l);
        if(child instanceof EasyWidget w)
            w.removeAddons(this::removeChild);
        if(child instanceof IPreRender r)
            this.preRenders.remove(r);
        if(child instanceof EasyWidgetWithChildren w)
            w.removeChildren();
    }

    @Override
    protected final void containerTick() {
        for(IEasyTickable t : this.guiTickers)
            t.tick();
        this.screenTick();
    }

    protected void screenTick() {}

    @Nonnull
    @Override
    @Deprecated
    protected final <W extends GuiEventListener & NarratableEntry> W addWidget(@Nonnull W widget) { return this.addChild(widget); }

    @Nonnull
    @Override
    @Deprecated
    protected final <W extends GuiEventListener & Widget & NarratableEntry> W addRenderableWidget(@Nonnull W widget) { return this.addChild(widget); }

    @Nonnull
    @Override
    @Deprecated
    protected final <W extends Widget> W addRenderableOnly(@Nonnull W widget) { return this.addChild(widget); }

    @Override
    @Deprecated
    protected final void removeWidget(@Nonnull GuiEventListener widget) { this.removeChild(widget); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        for(IScrollListener l : this.scrollListeners)
        {
            if(l.mouseScrolled(mouseX, mouseY, scroll))
                return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for(IMouseListener l : this.mouseListeners)
        {
            if(l.onMouseClicked(mouseX, mouseY, button))
                return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for(IMouseListener l : this.mouseListeners)
        {
            if(l.onMouseReleased(mouseX, mouseY, button))
                return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
        InputConstants.Key mouseKey = InputConstants.getKey(p_97765_, p_97766_);
        //Manually block closing by inventory key, to allow usage of all letters while typing player names, etc.
        if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey) && this.blockInventoryClosing()) {
            return true;
        }
        return super.keyPressed(p_97765_, p_97766_, p_97767_);
    }

}
