package io.github.lightman314.lightmanscurrency.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.money.CoinValue;
import io.github.lightman314.lightmanscurrency.money.MoneyUtil;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.MessageRequestNBT;
import io.github.lightman314.lightmanscurrency.network.message.logger.MessageClearLogger;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageRequestSyncUsers;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageSyncUsers;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageChangeSettings;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenStorage;
import io.github.lightman314.lightmanscurrency.network.message.trader.MessageOpenTrades;
import io.github.lightman314.lightmanscurrency.trader.ITrader;
import io.github.lightman314.lightmanscurrency.trader.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.trader.settings.CoreTraderSettings;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import io.github.lightman314.lightmanscurrency.trader.settings.Settings;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.TileEntityUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;

public abstract class TraderTileEntity extends TileEntity implements IOwnableTileEntity, ITickableTileEntity, IPermissions, ITrader{
	
	CoreTraderSettings coreSettings = new CoreTraderSettings(this, this::markCoreSettingsDirty, this::sendSettingsUpdateToServer);
	
	protected CoinValue storedMoney = new CoinValue();
	
	/** A list of players using this trader */
	private List<PlayerEntity> users = new ArrayList<>();
	private int userCount = 0;
	
	private boolean versionUpdate = false;
	private int oldVersion = 0;
	
	protected TraderTileEntity(TileEntityType<?> type)
	{
		super(type);
	}
	
	public void userOpen(PlayerEntity player)
	{
		if(!users.contains(player))
		{
			//LightmansCurrency.LOGGER.info("Player with ID " + player.getUniqueID() + " has opened the trader.");
			users.add(player);
			sendUserUpdate();
		}
	}
	
	public void userClose(PlayerEntity player)
	{
		if(users.contains(player))
		{
			//LightmansCurrency.LOGGER.info("Player with ID " + player.getUniqueID() + " has closed the trader.");
			users.remove(player);
			sendUserUpdate();
		}
	}
	
	public int getUserCount()
	{
		if(world.isRemote)
			return this.userCount;
		else
			return this.users.size();
	}
	
	public final void forceReopen() { this.forceReopen(Lists.newArrayList(this.users)); }
	
	protected abstract void forceReopen(List<PlayerEntity> users);
	
	protected List<PlayerEntity> getUsers() { return this.users; }
	
	private void sendUserUpdate()
	{
		if(!world.isRemote)
		{
			Chunk chunk = (Chunk)this.world.getChunk(this.pos);
			LightmansCurrencyPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new MessageSyncUsers(this.pos, this.getUserCount()));
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void setUserCount(int value)
	{
		this.userCount = value;
	}
	
	public CoreTraderSettings getCoreSettings()
	{
		return this.coreSettings;
	}
	
	public void markCoreSettingsDirty()
	{
		if(!this.world.isRemote)
		{
			CompoundNBT compound = this.writeCoreSettings(new CompoundNBT());
			TileEntityUtil.sendUpdatePacket(this, this.superWrite(compound));
		}
		this.markDirty();
	}
	
	protected final void sendSettingsUpdateToServer(ResourceLocation type, CompoundNBT compound)
	{
		if(this.world.isRemote)
		{
			//LightmansCurrency.LogInfo("Sending settings update packet from client to server.\n" + compound.toString());
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageChangeSettings(this.pos, type, compound));
		}
	}
	
	public final void changeSettings(ResourceLocation type, PlayerEntity requestor, CompoundNBT updateInfo)
	{
		if(this.world.isRemote)
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
	
	/**
	 * Defines the owner of the Trader.
	 * Does nothing if the owner is already defined.
	 * @param player The player that will be defined as the trader's owner.
	 */
	public void initOwner(PlayerReference player)
	{
		this.coreSettings.initializeOwner(player);
	}
	
	/**
	 * Returns whether the player is allowed to break the block.
	 * @return Returns true if the player is the owner, or if the player is both creative & opped.
	 */
	public boolean canBreak(PlayerEntity player)
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
	
	public CoinValue getInternalStoredMoney() 
	{
		return this.storedMoney;
	}
	
	/**
	 * Adds the given amount of money to the stored money.
	 */
	public void addStoredMoney(CoinValue addedAmount)
	{
		if(this.coreSettings.hasBankAccount())
		{
			BankAccount account = this.coreSettings.getBankAccount();
			account.depositCoins(addedAmount);
			account.LogInteraction(this, addedAmount, true);
			return;
		}
		this.storedMoney.addValue(addedAmount);
		if(!this.world.isRemote)
		{
			CompoundNBT compound = this.writeStoredMoney(new CompoundNBT());
			TileEntityUtil.sendUpdatePacket(this, super.write(compound));
		}
	}
	
	/**
	 * Removes the given amount of money to the stored money.
	 */
	public void removeStoredMoney(CoinValue removedAmount)
	{
		if(this.coreSettings.hasBankAccount())
		{
			BankAccount account = this.coreSettings.getBankAccount();
			account.withdrawCoins(removedAmount);
			account.LogInteraction(this, removedAmount, false);
			return;
		}
		long newValue = this.storedMoney.getRawValue() - removedAmount.getRawValue();
		this.storedMoney.readFromOldValue(newValue);
		if(!this.world.isRemote)
		{
			CompoundNBT compound = this.writeStoredMoney(new CompoundNBT());
			TileEntityUtil.sendUpdatePacket(this, super.write(compound));
		}
	}
	
	/**
	 * Resets the stored money to 0.
	 */
	public void clearStoredMoney()
	{
		this.storedMoney = new CoinValue();
		if(!this.world.isRemote)
		{
			CompoundNBT compound = this.writeStoredMoney(new CompoundNBT());
			TileEntityUtil.sendUpdatePacket(this, super.write(compound));
		}
	}
	
	public void tick()
	{
		if(this.versionUpdate && this.world != null)
		{
			this.versionUpdate = false;
			if(!this.world.isRemote)
				this.onVersionUpdate(oldVersion);
		}
	}
	
	public ITextComponent getName()
	{
		if(this.coreSettings.hasCustomName())
			return new StringTextComponent(this.coreSettings.getCustomName());
		return this.getBlockName();
	}
	
	//Seperated into it's own function, as this was a slightly more complicated process in 1.16
	private ITextComponent getBlockName() {
		return new TranslationTextComponent(this.getBlockState().getBlock().getTranslationKey());
	}
	
	public ITextComponent getTitle()
	{
		if(this.coreSettings.isCreative() || this.coreSettings.getOwnerName().isEmpty())
			return this.getName();
		return new TranslationTextComponent("gui.lightmanscurrency.trading.title", this.getName(), this.coreSettings.getOwnerName());
	}
	
	public abstract INamedContainerProvider getTradeMenuProvider();
	
	public void openTradeMenu(PlayerEntity player)
	{
		INamedContainerProvider provider = getTradeMenuProvider();
		if(provider == null)
		{
			LightmansCurrency.LogError("No trade menu container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayerEntity))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the trade menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayerEntity)player, provider, pos);
	}
	
	public abstract INamedContainerProvider getStorageMenuProvider(); 
	
	public void openStorageMenu(PlayerEntity player)
	{
		if(!this.hasPermission(player, Permissions.OPEN_STORAGE))
		{
			Settings.PermissionWarning(player, "open trader storage", Permissions.OPEN_STORAGE);
			return;
		}
		INamedContainerProvider provider = getStorageMenuProvider();
		if(provider == null)
		{
			LightmansCurrency.LogError("No storage container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayerEntity))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the storage menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayerEntity)player, provider, pos);
	}
	
	public abstract INamedContainerProvider getCashRegisterTradeMenuProvider(CashRegisterTileEntity cashRegister);
	
	public void openCashRegisterTradeMenu(PlayerEntity player, CashRegisterTileEntity cashRegister)
	{
		INamedContainerProvider provider = getCashRegisterTradeMenuProvider(cashRegister);
		if(provider == null)
		{
			LightmansCurrency.LogError("No cash register container provider was given for the trader of type " + this.getType().getRegistryName().toString());
			return;
		}
		if(!(player instanceof ServerPlayerEntity))
		{
			LightmansCurrency.LogError("Player is not a server player entity. Cannot open the cash register menu.");
			return;
		}
		NetworkHooks.openGui((ServerPlayerEntity)player, provider, new CRDataWriter(pos, cashRegister.getPos()));
	}
	
	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		writeCoreSettings(compound);
		writeStoredMoney(compound);
		writeVersion(compound);
		
		return super.write(compound);
	}
	
	public CompoundNBT superWrite(CompoundNBT compound)
	{
		return super.write(compound);
	}
	
	protected CompoundNBT writeStoredMoney(CompoundNBT compound)
	{
		this.storedMoney.writeToNBT(compound, "StoredMoney");
		return compound;
	}
	
	protected CompoundNBT writeCoreSettings(CompoundNBT compound)
	{
		compound.put("CoreSettings", this.coreSettings.save(new CompoundNBT()));
		return compound;
	}
	
	protected CompoundNBT writeVersion(CompoundNBT compound)
	{
		compound.putInt("TraderVersion", this.GetCurrentVersion());
		return compound;
	}
	
	@Override
	public void read(BlockState state, CompoundNBT compound)
	{
		//Core Settings
		if(compound.contains("CoreSettings", Constants.NBT.TAG_COMPOUND))
			this.coreSettings.load(compound.getCompound("CoreSettings"));
		else if(compound.contains("OwnerID"))//Only load from old trader data if an old tag is present to prevent invalid reloading on an update message
			this.coreSettings.loadFromOldTraderData(compound);
		//Stored Money
		//Load stored money
		if(compound.contains("StoredMoney", Constants.NBT.TAG_INT))
		{
			LightmansCurrency.LogInfo("Reading stored money from older value format. Will be updated to newer value format.");
			this.storedMoney.readFromOldValue(compound.getInt("StoredMoney"));
		}
		else if(compound.contains("StoredMoney"))
			this.storedMoney.readFromNBT(compound, "StoredMoney");
		
		//Version
		if(compound.contains("TraderVersion", Constants.NBT.TAG_INT))
			oldVersion = compound.getInt("TraderVersion");
		//Validate the version #
		if(oldVersion < this.GetCurrentVersion())
			this.versionUpdate = true; //Flag this to perform a version update later once the world has been defined
		
		super.read(state, compound);
	}
	
	protected abstract void onVersionUpdate(int oldVersion);
	
	public int GetCurrentVersion() { return 0; };
	
	@Override
	public void onLoad()
	{
		if(world.isRemote)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageRequestNBT(this));
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageRequestSyncUsers(this.pos));
		}
	}
	
	public void dumpContents(World world, BlockPos pos)
	{
		List<ItemStack> coinItems = MoneyUtil.getCoinsOfValue(this.storedMoney);
		if(coinItems.size() > 0)
			InventoryUtil.dumpContents(world, pos, coinItems);
	}
	
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket()
	{
		return new SUpdateTileEntityPacket(this.pos, 0, this.write(new CompoundNBT()));
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
	{
		CompoundNBT compound = pkt.getNbtCompound();
		//CurrencyMod.LOGGER.info("Loading NBT from update packet.");
		this.read(this.getBlockState(), compound);
	}
	
	private class CRDataWriter implements Consumer<PacketBuffer>
	{
		
		BlockPos traderPos;
		BlockPos registerPos;
		
		public CRDataWriter(BlockPos traderPos, BlockPos registerPos)
		{
			this.traderPos = traderPos;
			this.registerPos = registerPos;
		}
		
		@Override
		public void accept(PacketBuffer buffer) {
			buffer.writeBlockPos(traderPos);
			buffer.writeBlockPos(registerPos);
			
		}
	}
	
	protected class TradeIndexDataWriter implements Consumer<PacketBuffer>
	{
		BlockPos traderPos;
		int tradeIndex;
		
		public TradeIndexDataWriter(BlockPos traderPos, int tradeIndex)
		{
			this.traderPos = traderPos;
			this.tradeIndex = tradeIndex;
		}
		
		@Override
		public void accept(PacketBuffer buffer)
		{
			buffer.writeBlockPos(traderPos);
			buffer.writeInt(tradeIndex);
		}
		
	}
	
	@Override
	public boolean isClient()
	{
		if(this.world == null)
			return true;
		return this.world.isRemote;
	}
	
	@Override
	public void sendOpenTraderMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenTrades(this.pos));
	}
	
	@Override
	public void sendOpenStorageMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageOpenStorage(this.pos));
	}
	
	@Override
	public void sendClearLogMessage() {
		if(this.isClient())
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageClearLogger(this.pos));
	}
	
}