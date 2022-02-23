package io.github.lightman314.lightmanscurrency.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.blockentity.interfaces.IOwnableBlockEntity;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.MessageRequestNBT;
import io.github.lightman314.lightmanscurrency.network.message.logger.MessageClearLogger;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageChangeSettings;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenStorage;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenTrades;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageRequestSyncUsers;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageSyncUsers;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.CoreTraderSettings;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil;
import io.github.lightman314.lightmanscurrency.util.BlockEntityUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

public abstract class TraderBlockEntity extends TickableBlockEntity implements IOwnableBlockEntity, ITrader{
	
	CoreTraderSettings coreSettings = new CoreTraderSettings(this, this::markCoreSettingsDirty, this::sendSettingsUpdateToServer);
	
	protected CoinValue storedMoney = new CoinValue();
	
	/** A list of players using this trader */
	private List<Player> users = new ArrayList<>();
	private int userCount = 0;
	
	private boolean versionUpdate = false;
	private int oldVersion = 0;
	
	protected TraderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public void userOpen(Player player)
	{
		if(!users.contains(player))
		{
			//LightmansCurrency.LOGGER.info("Player with ID " + player.getUniqueID() + " has opened the trader.");
			this.users.add(player);
			this.sendUserUpdate();
		}
	}
	
	public void userClose(Player player)
	{
		if(users.contains(player))
		{
			//LightmansCurrency.LOGGER.info("Player with ID " + player.getUniqueID() + " has closed the trader.");
			this.users.remove(player);
			this.sendUserUpdate();
		}
	}
	
	public int getUserCount()
	{
		if(this.level.isClientSide)
			return this.userCount;
		else
			return this.users.size();
	}
	
	public final void forceReopen() { if(this.isServer()) this.forceReopen(Lists.newArrayList(this.users)); }
	
	protected abstract void forceReopen(List<Player> users);
	
	protected List<Player> getUsers() { return this.users; }
	
	private void sendUserUpdate()
	{
		if(!this.level.isClientSide)
		{
			LevelChunk chunk = (LevelChunk)this.level.getChunk(this.worldPosition);
			LightmansCurrencyPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new MessageSyncUsers(this.worldPosition, this.getUserCount()));
		}
	}
	
	public void setUserCount(int value)
	{
		this.userCount = value;
	}
	
	public CoreTraderSettings getCoreSettings() { return this.coreSettings; }
	
	public void markCoreSettingsDirty()
	{
		if(!this.level.isClientSide)
		{
			CompoundTag compound = this.writeCoreSettings(new CompoundTag());
			BlockEntityUtil.sendUpdatePacket(this, this.superWrite(compound));
		}
		this.setChanged();
	}
	
	protected final void sendSettingsUpdateToServer(ResourceLocation type, CompoundTag updateInfo)
	{
		if(this.level.isClientSide)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageChangeSettings(this.worldPosition, type, updateInfo));
		}
	}
	
	public final void changeSettings(ResourceLocation type, Player requestor, CompoundTag updateInfo)
	{
		if(this.level.isClientSide)
			LightmansCurrency.LogError("TraderTileEntity.changeSettings was called on a client.");
		if(type.equals(this.coreSettings.getType()))
			this.coreSettings.changeSetting(requestor, updateInfo);
		else
		{
			this.getAdditionalSettings().forEach(setting ->{
				if(type.equals(setting.getType()))
					setting.changeSetting(requestor, updateInfo);
			});
		}
	}
	
	public boolean hasPermission(Player player, String permission)
	{
		return this.coreSettings.hasPermission(player, permission);
	}
	
	public int getPermissionLevel(Player player, String permission)
	{
		return this.coreSettings.getPermissionLevel(player, permission);
	}
	
	public void initOwner(PlayerReference player)
	{
		this.coreSettings.initializeOwner(player);
	}
	
	/**
	 * Returns whether the player is allowed to break the block.
	 * @return Returns true if the player is the owner, or if the player is both creative & opped.
	 */
	public boolean canBreak(Player player)
	{
		return this.hasPermission(player, Permissions.BREAK_TRADER);
	}
	
	/**
	 * Gets the amount of stored money.
	 * If a bank account is linked, it will return the amount stored in the account instead.
	 */
	public CoinValue getStoredMoney()
	{
		if(this.coreSettings.hasBankAccount())
		{
			BankAccount account = this.coreSettings.getBankAccount();
			return account.getCoinStorage().copy();
		}
		return this.storedMoney;
	}
	
	/**
	 * Gets the amount of stored money.
	 * Always returns the amount contained within regardless of the linked bank account
	 */
	public CoinValue getInternalStoredMoney() {
		return this.storedMoney;
	}
	
	/**
	 * Adds the given amount of money to the stored money.
	 * If a bank account is linked, it will add it to the account instead.
	 */
	public void addStoredMoney(CoinValue addedAmount)
	{
		if(this.coreSettings.hasBankAccount())
		{
			BankAccount account = this.coreSettings.getBankAccount();
			account.depositCoins(addedAmount);
			return;
		}
		storedMoney.addValue(addedAmount);
		if(!this.level.isClientSide)
		{
			BlockEntityUtil.sendUpdatePacket(this, this.writeStoredMoney(new CompoundTag()));
		}
	}
	
	/**
	 * Removes the given amount of money to the stored money.
	 * If a bank account is linked, it will remove it from the account instead.
	 */
	public void removeStoredMoney(CoinValue removedAmount)
	{
		if(this.coreSettings.hasBankAccount())
		{
			BankAccount account = this.coreSettings.getBankAccount();
			account.withdrawCoins(removedAmount);
			return;
		}
		long newValue = this.storedMoney.getRawValue() - removedAmount.getRawValue();
		this.storedMoney.readFromOldValue(newValue);
		if(!this.level.isClientSide)
		{
			BlockEntityUtil.sendUpdatePacket(this, this.writeStoredMoney(new CompoundTag()));
		}
	}
	
	/**
	 * Resets the stored money to 0.
	 */
	public void clearStoredMoney()
	{
		storedMoney = new CoinValue();
		if(!this.level.isClientSide)
		{
			BlockEntityUtil.sendUpdatePacket(this, this.writeStoredMoney(new CompoundTag()));
		}
	}
	
	@Override
	public void serverTick()
	{
		if(this.versionUpdate && this.level != null)
		{
			this.versionUpdate = false;
			this.onVersionUpdate(oldVersion);
		}
	}
	
	public Component getName()
	{
		if(this.coreSettings.hasCustomName())
			return new TextComponent(this.coreSettings.getCustomName());
		if(this.level.isClientSide)
			return this.getBlockState().getBlock().getName();
		return new TextComponent("");
	}
	
	public Component getTitle()
	{
		if(this.coreSettings.isCreative() || this.coreSettings.getOwnerName().isEmpty())
			return this.getName();
		return new TranslatableComponent("gui.lightmanscurrency.trading.title", this.getName(), this.coreSettings.getOwnerName());
	}
	
	public abstract MenuProvider getTradeMenuProvider();
	
	public void openTradeMenu(Player player)
	{
		MenuProvider provider = getTradeMenuProvider();
		if(provider == null)
		{
			LightmansCurrency.LogError("No trade menu container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayer))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the trade menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayer)player, provider, this.worldPosition);
	}
	
	public abstract MenuProvider getStorageMenuProvider(); 
	
	public void openStorageMenu(Player player)
	{
		if(!this.hasPermission(player, Permissions.OPEN_STORAGE))
		{
			Settings.PermissionWarning(player, "open trader storage", Permissions.OPEN_STORAGE);
			return;
		}
		MenuProvider provider = getStorageMenuProvider();
		if(provider == null)
		{
			LightmansCurrency.LogError("No storage container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayer))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the storage menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayer)player, provider, this.worldPosition);
	}
	
	public abstract MenuProvider getCashRegisterTradeMenuProvider(CashRegisterBlockEntity cashRegister);
	
	public void openCashRegisterTradeMenu(Player player, CashRegisterBlockEntity cashRegister)
	{
		MenuProvider provider = getCashRegisterTradeMenuProvider(cashRegister);
		if(provider == null)
		{
			LightmansCurrency.LogError("No cash register container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayer))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the cash register menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayer)player, provider, new CRDataWriter(this.worldPosition, cashRegister.getBlockPos()));
	}
	
	@Override
	public void saveAdditional(CompoundTag compound)
	{
		writeCoreSettings(compound);
		writeStoredMoney(compound);
		writeVersion(compound);
		
		super.saveAdditional(compound);
		
	}
	
	@Deprecated //Unecessary. Metadata is written directly into the update packet.
	public CompoundTag superWrite(CompoundTag compound)
	{
		return compound;
	}
	
	protected CompoundTag writeStoredMoney(CompoundTag compound)
	{
		this.storedMoney.writeToNBT(compound, "StoredMoney");
		return compound;
	}
	
	protected CompoundTag writeCoreSettings(CompoundTag compound)
	{
		compound.put("CoreSettings", this.coreSettings.save(new CompoundTag()));
		return compound;
	}
	
	protected CompoundTag writeVersion(CompoundTag compound)
	{
		compound.putInt("TraderVersion", this.GetCurrentVersion());
		return compound;
	}
	
	@Override
	public void load(CompoundTag compound)
	{
		//Core Settings
		if(compound.contains("CoreSettings", Tag.TAG_COMPOUND))
			this.coreSettings.load(compound.getCompound("CoreSettings"));
		else if(compound.contains("OwnerID"))
			this.coreSettings.loadFromOldTraderData(compound);
		//Stored Money
		//Load stored money
		this.storedMoney.readFromNBT(compound, "StoredMoney");
		
		//Version
		if(compound.contains("TraderVersion", Tag.TAG_INT))
			oldVersion = compound.getInt("TraderVersion");
		//Validate the version #
		if(oldVersion < this.GetCurrentVersion())
			this.versionUpdate = true; //Flag this to perform a version update later once the world has been defined
		
		super.load(compound);
	}
	
	protected abstract void onVersionUpdate(int oldVersion);
	
	public int GetCurrentVersion() { return 0; };
	
	@Override
	public void onLoad()
	{
		if(this.level.isClientSide)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageRequestNBT(this));
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageRequestSyncUsers(this.worldPosition));
		}
	}
	
	public void dumpContents(Level world, BlockPos pos)
	{
		List<ItemStack> coinItems = MoneyUtil.getCoinsOfValue(this.storedMoney);
		if(coinItems.size() > 0)
			InventoryUtil.dumpContents(world, pos, coinItems);
	}
	
	@Override
	public CompoundTag getUpdateTag() { return this.saveWithFullMetadata(); }
	
	private class CRDataWriter implements Consumer<FriendlyByteBuf>
	{
		
		BlockPos traderPos;
		BlockPos registerPos;
		
		public CRDataWriter(BlockPos traderPos, BlockPos registerPos)
		{
			this.traderPos = traderPos;
			this.registerPos = registerPos;
		}
		
		@Override
		public void accept(FriendlyByteBuf buffer) {
			buffer.writeBlockPos(traderPos);
			buffer.writeBlockPos(registerPos);
			
		}
	}
	
	protected class TradeIndexDataWriter implements Consumer<FriendlyByteBuf>
	{
		BlockPos traderPos;
		int tradeIndex;
		
		public TradeIndexDataWriter(BlockPos traderPos, int tradeIndex)
		{
			this.traderPos = traderPos;
			this.tradeIndex = tradeIndex;
		}
		
		@Override
		public void accept(FriendlyByteBuf buffer)
		{
			buffer.writeBlockPos(traderPos);
			buffer.writeInt(tradeIndex);
		}
		
	}
	
	public boolean isClient()
	{
		if(this.level == null)
			return true;
		return this.level.isClientSide;
	}
	
	@Override
	public void sendOpenTraderMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenTrades(this.worldPosition));
	}

	@Override
	public void sendOpenStorageMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenStorage(this.worldPosition));
	}

	@Override
	public void sendClearLogMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageClearLogger(this.worldPosition));
	}
	
}