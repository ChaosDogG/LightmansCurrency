package io.github.lightman314.lightmanscurrency.common.universal_traders;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount;
import io.github.lightman314.lightmanscurrency.common.universal_traders.bank.BankAccount.AccountReference;
import io.github.lightman314.lightmanscurrency.money.CoinValue;
import io.github.lightman314.lightmanscurrency.trader.settings.PlayerReference;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class RemoteTradeData {
	
	public enum RemoteTradeResult {
		/**
		 * Remote trade was successfully executed
		 */
		SUCCESS,
		/**
		 * Trade failed as the trader is out of stock
		 */
		FAIL_OUT_OF_STOCK,
		
		/**
		 * Trade failed as the player could not afford the trade
		 */
		FAIL_CANNOT_AFFORD,
		/**
		 * Trade failed as there's no room for the output items
		 */
		FAIL_NO_OUTPUT_SPACE,
		/**
		 * Trade failed as there's no room for the input items
		 */
		FAIL_NO_INPUT_SPACE,
		/**
		 * Trade failed as the trade rules denied the trade
		 */
		FAIL_TRADE_RULE_DENIAL,
		/**
		 * Trade failed as the trade is no longer valid
		 */
		FAIL_INVALID_TRADE,
		/**
		 * Trade failed as this trader does not support remote trades
		 */
		FAIL_NOT_SUPPORTED,
		/**
		 * Trade failed as the trader was null
		 */
		FAIL_NULL
	}
	
	private final PlayerReference playerSource;
	public final PlayerReference getPlayerSource() { return this.playerSource; }
	
	private final AccountReference bankAccount;
	public boolean hasBankAccount() { return this.bankAccount != null && this.bankAccount.get() != null; }
	public BankAccount getBankAccount() { return this.bankAccount.get(); }
	
	private final CoinValue storedMoney;
	public boolean hasStoredMoney() { return this.storedMoney != null; }
	public CoinValue getStoredMoney() { return this.storedMoney; }
	
	private final IInventory itemSlots;
	public boolean hasItemSlots() { return this.itemSlots != null; }
	public IInventory getItemSlots() { return this.itemSlots; }
	
	private final IFluidHandler fluidTank;
	public boolean hasFluidTank() { return this.fluidTank != null; }
	public IFluidHandler getFluidTank() { return this.fluidTank; }
	
	private final IEnergyStorage energyTank;
	public boolean hasEnergyTank() { return this.energyTank != null; }
	public IEnergyStorage getEnergyTank() { return this.energyTank; }
	
	public RemoteTradeData(@Nonnull PlayerReference playerSource, @Nonnull AccountReference bankAccount, @Nullable CoinValue storedMoney, @Nullable IInventory itemSlots, @Nullable IFluidHandler fluidTank, @Nullable IEnergyStorage energyTank) {
		this.playerSource = playerSource;
		this.bankAccount = bankAccount;
		this.storedMoney = storedMoney;
		this.itemSlots = itemSlots;
		this.fluidTank = fluidTank;
		this.energyTank = energyTank;
	}
	
	public boolean hasPaymentMethod() { return this.hasBankAccount() || this.hasStoredMoney(); }
	
	public boolean hasFunds(CoinValue price)
	{
		long funds = 0;
		if(this.hasBankAccount())
			funds += this.getBankAccount().getCoinStorage().getRawValue();
		if(this.hasStoredMoney())
			funds += this.getStoredMoney().getRawValue();
		return funds > price.getRawValue();
	}
	
	public boolean getPayment(CoinValue price)
	{
		if(this.hasFunds(price))
		{
			long amountToWithdraw = price.getRawValue();
			if(this.hasStoredMoney())
			{
				CoinValue storedMoney = this.getStoredMoney();
				long removeAmount = Math.min(amountToWithdraw, storedMoney.getRawValue());
				amountToWithdraw -= removeAmount;
				storedMoney.readFromOldValue(storedMoney.getRawValue() - removeAmount);
			}
			if(this.hasBankAccount() && amountToWithdraw > 0)
			{
				this.getBankAccount().withdrawCoins(new CoinValue(amountToWithdraw));
			}
			return true;
		}
		return false;
	}
	
	public boolean givePayment(CoinValue price)
	{
		if(this.hasBankAccount())
		{
			this.getBankAccount().depositCoins(price);
			return true;
		}
		else if(this.hasStoredMoney())
		{
			CoinValue storedMoney = this.getStoredMoney();
			storedMoney.addValue(price);
		}
		return false;
	}
	
	public boolean hasItem(ItemStack item)
	{
		if(this.hasItemSlots())
			return InventoryUtil.GetItemCount(this.getItemSlots(), item) > item.getCount();
		return false;
	}
	
	public boolean collectItem(ItemStack item)
	{
		if(this.hasItem(item) && this.hasItemSlots())
		{
			InventoryUtil.RemoveItemCount(this.getItemSlots(), item);
			return true;
		}
		return false;
	}
	
	public boolean canFitItem(ItemStack item)
	{
		if(this.hasItemSlots())
			return InventoryUtil.CanPutItemStack(this.getItemSlots(), item);
		return false;
	}
	
	public boolean putItem(ItemStack item)
	{
		if(this.canFitItem(item) && this.hasItemSlots())
			return InventoryUtil.PutItemStack(this.getItemSlots(), item);
		return false;
	}
	
	public boolean hasFluid(FluidStack fluid)
	{
		if(this.hasFluidTank())
		{
			FluidStack result = this.getFluidTank().drain(fluid, FluidAction.SIMULATE);
			if(result.isEmpty() || result.getAmount() < fluid.getAmount())
				return false;
			return true;
		}
		return false;
	}
	
	public FluidStack drainFluid(FluidStack fluid)
	{
		if(this.hasFluid(fluid) && this.hasFluidTank())
			return this.getFluidTank().drain(fluid, FluidAction.EXECUTE);
		return FluidStack.EMPTY;
	}
	
	public boolean canFitFluid(FluidStack fluid)
	{
		if(this.hasFluidTank())
			return this.getFluidTank().fill(fluid, FluidAction.SIMULATE) == fluid.getAmount();
		return false;
	}
	
	public boolean fillFluid(FluidStack fluid)
	{
		if(this.canFitFluid(fluid) && this.hasFluidTank())
		{
			this.getFluidTank().fill(fluid, FluidAction.EXECUTE);
			return true;
		}
		return false;
	}
	
	public boolean hasEnergy(int amount)
	{
		if(this.hasEnergyTank())
			return this.getEnergyTank().extractEnergy(amount, true) == amount;
		return false;
	}
	
	public boolean drainEnergy(int amount)
	{
		if(this.hasEnergy(amount) && this.hasEnergyTank())
		{
			this.getEnergyTank().extractEnergy(amount, false);
			return true;
		}
		return false;
	}
	
	public boolean canFitEnergy(int amount)
	{
		if(this.hasEnergyTank())
			return this.getEnergyTank().receiveEnergy(amount, true) == amount;
		return false;
	}
	
	public boolean fillEnergy(int amount)
	{
		if(this.canFitEnergy(amount) && this.hasEnergyTank())
		{
			this.getEnergyTank().receiveEnergy(amount, true);
			return true;
		}
		return false;
	}
	
}
