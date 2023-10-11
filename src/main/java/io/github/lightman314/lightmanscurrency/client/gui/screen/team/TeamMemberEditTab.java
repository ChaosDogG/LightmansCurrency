package io.github.lightman314.lightmanscurrency.client.gui.screen.team;

import java.util.List;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.screen.TeamManagerScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollTextDisplay;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyTextButton;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.network.packet.LazyPacketData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

public class TeamMemberEditTab extends TeamTab {
	
	public TeamMemberEditTab(TeamManagerScreen screen) { super(screen); }
	
	@Nonnull
	@Override
	public IconData getIcon() { return IconAndButtonUtil.ICON_ALEX_HEAD; }

	@Override
	public MutableComponent getTooltip() { return EasyText.translatable("tooltip.lightmanscurrency.team.member_edit"); }

	@Override
	public boolean allowViewing(Player player, Team team) {
		return team != null;
	}
	
	ScrollTextDisplay memberDisplay;
	
	EditBox memberNameInput;
	EasyButton buttonAddMember;
	EasyButton buttonPromoteMember;
	EasyButton buttonRemoveMember;

	@Override
	public boolean blockInventoryClosing() { return true; }
	@Override
	public void initialize(ScreenArea screenArea, boolean firstOpen) {
		
		this.memberNameInput = this.addChild(new EditBox(this.getFont(), screenArea.x + 11, screenArea.y + 9, 178, 20, EasyText.empty()));
		this.memberNameInput.setMaxLength(16);
		
		this.buttonAddMember = this.addChild(new EasyTextButton(screenArea.pos.offset(10, 30), 60, 20, EasyText.translatable("gui.button.lightmanscurrency.team.member.add"), this::addMember));
		this.buttonPromoteMember = this.addChild(new EasyTextButton(screenArea.pos.offset(70, 30), 60, 20, EasyText.translatable("gui.button.lightmanscurrency.team.member.promote"), this::addAdmin));
		this.buttonRemoveMember = this.addChild(new EasyTextButton(screenArea.pos.offset(130, 30), 60, 20, EasyText.translatable("gui.button.lightmanscurrency.team.member.remove"), this::removeMember));
		this.buttonAddMember.active = this.buttonPromoteMember.active = this.buttonRemoveMember.active = false;
		
		this.memberDisplay = this.addChild(new ScrollTextDisplay(screenArea.pos.offset(10, 55), screenArea.width - 20, screenArea.height - 65, this::getMemberList));
		this.memberDisplay.setColumnCount(2);
		
	}
	
	private List<Component> getMemberList()
	{
		List<Component> list = Lists.newArrayList();
		Team team = this.getActiveTeam();
		if(team != null)
		{
			//Do NOT List Owner
			//list.add(new TextComponent(team.getOwner().lastKnownName()).withStyle(ChatFormatting.GREEN));
			//List Admins
			team.getAdmins().forEach(admin -> list.add(admin.getNameComponent(true).withStyle(ChatFormatting.DARK_GREEN)));
			//List members
			team.getMembers().forEach(member -> list.add(member.getNameComponent(true)));
		}
		
		return list;
	}

	@Override
	public void renderBG(@Nonnull EasyGuiGraphics gui) { }

	@Override
	public void tick() {
		
		if(this.getActiveTeam().isAdmin(this.getPlayer()))
		{
			this.buttonAddMember.active = this.buttonPromoteMember.active = this.buttonRemoveMember.active = !this.memberNameInput.getValue().isBlank();
		}
		else
		{
			this.buttonAddMember.active = this.buttonPromoteMember.active = false;
			this.buttonRemoveMember.active = this.getPlayer().getGameProfile().getName().equalsIgnoreCase(this.memberNameInput.getValue());
		}
		
	}

	private void addMember(EasyButton button)
	{
		if(this.memberNameInput.getValue().isBlank() || this.getActiveTeam() == null)
			return;
		
		Team team = this.getActiveTeam();
		team.changeAddMember(this.getPlayer(), this.memberNameInput.getValue());
		this.RequestChange(LazyPacketData.simpleString("AddMember", this.memberNameInput.getValue()));
		this.memberNameInput.setValue("");
		
	}
	
	private void addAdmin(EasyButton button)
	{
		if(this.memberNameInput.getValue().isBlank() || this.getActiveTeam() == null)
			return;
		
		Team team = this.getActiveTeam();
		team.changeAddAdmin(this.getPlayer(), this.memberNameInput.getValue());
		this.RequestChange(LazyPacketData.simpleString("AddAdmin", this.memberNameInput.getValue()));
		this.memberNameInput.setValue("");
	}
	
	private void removeMember(EasyButton button)
	{
		if(this.memberNameInput.getValue().isBlank() || this.getActiveTeam() == null)
			return;
		
		Team team = this.getActiveTeam();
		team.changeRemoveMember(this.getPlayer(), this.memberNameInput.getValue());
		this.RequestChange(LazyPacketData.simpleString("RemoveMember", this.memberNameInput.getValue()));
		this.memberNameInput.setValue("");
	}

}
