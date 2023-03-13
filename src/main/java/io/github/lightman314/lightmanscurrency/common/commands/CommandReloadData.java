package io.github.lightman314.lightmanscurrency.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.lightman314.lightmanscurrency.common.atm.ATMData;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.traders.TraderSaveData;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class CommandReloadData {
	
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		LiteralArgumentBuilder<CommandSource> lcReloadCommand
			= Commands.literal("lcreload")
				.requires((commandSource) -> commandSource.hasPermission(2))
				.executes(CommandReloadData::execute);
		
		dispatcher.register(lcReloadCommand);
		
	}
	
	static int execute(CommandContext<CommandSource> commandContext) throws CommandSyntaxException {
		
		TraderSaveData.ReloadPersistentTraders();
		MoneyUtil.reloadMoneyData();
		ATMData.reloadATMData();
		commandContext.getSource().sendSuccess(EasyText.translatable("command.lightmanscurrency.lcreload"), true);
		return 1;
		
	}
	
}