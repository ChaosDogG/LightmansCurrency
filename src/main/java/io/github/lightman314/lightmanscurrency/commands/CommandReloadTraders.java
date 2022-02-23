package io.github.lightman314.lightmanscurrency.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TranslationTextComponent;

public class CommandReloadTraders {
	
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		LiteralArgumentBuilder<CommandSource> lcReloadCommand
			= Commands.literal("lcreload")
				.requires((commandSource) -> commandSource.hasPermissionLevel(2))
				.executes(CommandReloadTraders::execute);
		
		dispatcher.register(lcReloadCommand);
		
	}
	
	static int execute(CommandContext<CommandSource> commandContext) throws CommandSyntaxException{
		TradingOffice.reloadPersistentTraders();
		commandContext.getSource().sendFeedback(new TranslationTextComponent("command.lightmanscurrency.lcreload"), true);
		return 1; 
	}
	
}