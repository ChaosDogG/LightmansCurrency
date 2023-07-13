package io.github.lightman314.lightmanscurrency.common.notifications.types.bank;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationCategory;
import io.github.lightman314.lightmanscurrency.common.notifications.categories.BankCategory;
import io.github.lightman314.lightmanscurrency.common.player.PlayerReference;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public abstract class DepositWithdrawNotification extends Notification {

	public static final ResourceLocation PLAYER_TYPE = new ResourceLocation(LightmansCurrency.MODID, "bank_deposit_player");
	public static final ResourceLocation TRADER_TYPE = new ResourceLocation(LightmansCurrency.MODID, "bank_deposit_trader");
	public static final ResourceLocation SERVER_TYPE = new ResourceLocation(LightmansCurrency.MODID, "bank_deposit_server");

	protected MutableComponent accountName;
	protected boolean isDeposit;
	protected CoinValue amount = CoinValue.EMPTY;

	protected DepositWithdrawNotification(MutableComponent accountName, boolean isDeposit, CoinValue amount) { this.accountName = accountName; this.isDeposit = isDeposit; this.amount = amount; }
	protected DepositWithdrawNotification() {}
	
	@Override
	public NotificationCategory getCategory() { return new BankCategory(this.accountName); }

	@Override
	protected void saveAdditional(CompoundTag compound) {
		compound.putString("Name", EasyText.Serializer.toJson(this.accountName));
		compound.putBoolean("Deposit", this.isDeposit);
		compound.put("Amount", this.amount.save());
	}

	@Override
	protected void loadAdditional(CompoundTag compound) {
		this.accountName = EasyText.Serializer.fromJson(compound.getString("Name"));
		this.isDeposit = compound.getBoolean("Deposit");
		this.amount = CoinValue.safeLoad(compound, "Amount");
	}
	
	protected abstract MutableComponent getName();
	
	@Override
	public MutableComponent getMessage() {
		return EasyText.translatable("log.bank", this.getName(), EasyText.translatable(this.isDeposit ? "log.bank.deposit" : "log.bank.withdraw"), this.amount.getComponent());
	}
	
	public static class Player extends DepositWithdrawNotification {

		PlayerReference player;
		
		public Player(PlayerReference player, MutableComponent accountName, boolean isDeposit, CoinValue amount) { super(accountName, isDeposit, amount); this.player = player; }
		public Player(CompoundTag compound) { this.load(compound); }
		
		@Override
		protected MutableComponent getName() { return this.player.getNameComponent(true); }
		
		@Override
		protected ResourceLocation getType() { return PLAYER_TYPE; }
		
		@Override
		protected void saveAdditional(CompoundTag compound) {
			super.saveAdditional(compound);
			compound.put("Player", this.player.save());
		}
		
		@Override
		protected void loadAdditional(CompoundTag compound) {
			super.loadAdditional(compound);
			this.player = PlayerReference.load(compound.getCompound("Player"));
		}
		
		@Override
		protected boolean canMerge(Notification other) {
			if(other instanceof Player n)
				return n.accountName.equals(this.accountName) && n.isDeposit == this.isDeposit && n.amount.equals(this.amount) && n.player.is(this.player);
			return false;
		}
		
	}
	
	public static class Trader extends DepositWithdrawNotification {
		MutableComponent traderName;
		
		public Trader(MutableComponent traderName, MutableComponent accountName, boolean isDeposit, CoinValue amount) { super(accountName, isDeposit, amount); this.traderName = traderName; }
		public Trader(CompoundTag compound) { this.load(compound); }
		
		@Override
		protected MutableComponent getName() { return this.traderName; }
		
		@Override
		protected ResourceLocation getType() { return TRADER_TYPE; }
		
		@Override
		protected void saveAdditional(CompoundTag compound) {
			super.saveAdditional(compound);
			compound.putString("Trader", EasyText.Serializer.toJson(this.traderName));
		}
		
		@Override
		protected void loadAdditional(CompoundTag compound) {
			super.loadAdditional(compound);
			this.traderName = EasyText.Serializer.fromJson(compound.getString("Trader"));
		}
		
		@Override
		protected boolean canMerge(Notification other) {
			if(other instanceof Trader n)
				return n.accountName.equals(this.accountName) && n.isDeposit == this.isDeposit && n.amount.equals(this.amount) && n.traderName.equals(this.traderName);
			return false;
		}
		
	}

	public static class Server extends DepositWithdrawNotification {

		public Server(MutableComponent accountName, boolean isDeposit, CoinValue amount) { super(accountName, isDeposit, amount); }
		public Server(CompoundTag compound) { this.load(compound); }

		@Override
		protected MutableComponent getName() { return EasyText.translatable("notifications.bank.server"); }

		@Override
		protected ResourceLocation getType() { return SERVER_TYPE; }

		@Override
		protected boolean canMerge(Notification other) { return false; }

	}
	
}
