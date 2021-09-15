package io.github.lightman314.lightmanscurrency.tradedata;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.tradedata.rules.ITradeRuleDeserializer;
import io.github.lightman314.lightmanscurrency.tradedata.rules.ITradeRuleHandler;
import io.github.lightman314.lightmanscurrency.tradedata.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;

public abstract class TradeData implements ITradeRuleHandler {

	public static final String DEFAULT_KEY = "Trades";
	
	protected CoinValue cost = new CoinValue();
	protected boolean isFree = false;
	
	List<TradeRule> rules = new ArrayList<>();
	
	public boolean isFree()
	{
		return this.isFree && cost.getRawValue() <= 0;
	}
	
	public void setFree(boolean isFree)
	{
		this.isFree = isFree;
		LightmansCurrency.LogInfo("Set free state of a trade to " + isFree);
	}
	
	public CoinValue getCost()
	{
		return this.cost;
	}
	
	public void setCost(CoinValue value)
	{
		this.cost = value;
	}
	
	public CompoundNBT getAsNBT()
	{
		CompoundNBT tradeNBT = new CompoundNBT();
		this.cost.writeToNBT(tradeNBT,"Price");
		tradeNBT.putBoolean("IsFree", this.isFree);
		ListNBT ruleData = new ListNBT();
		for(int i = 0; i < rules.size(); i++)
		{
			CompoundNBT thisRuleData = rules.get(i).getNBT();
			if(thisRuleData != null)
				ruleData.add(thisRuleData);
		}
		tradeNBT.put("Rules", ruleData);
		
		return tradeNBT;
	}
	
	protected void loadFromNBT(CompoundNBT nbt)
	{
		if(nbt.contains("Price", Constants.NBT.TAG_INT))
			cost.readFromOldValue(nbt.getInt("Price"));
		else if(nbt.contains("Price", Constants.NBT.TAG_LIST))
			cost.readFromNBT(nbt, "Price");
		//Set whether it's free or not
		if(nbt.contains("IsFree"))
			this.isFree = nbt.getBoolean("IsFree");
		else
			this.isFree = false;
		
		this.rules.clear();
		if(nbt.contains("Rules", Constants.NBT.TAG_LIST))
		{
			ListNBT ruleData = nbt.getList("Rules", Constants.NBT.TAG_COMPOUND);
			for(int i = 0; i < ruleData.size(); i++)
			{
				CompoundNBT thisRuleData = ruleData.getCompound(i);
				TradeRule thisRule = ITradeRuleDeserializer.Deserialize(thisRuleData);
				if(thisRule != null)
					this.rules.add(thisRule);
			}
		}
		
	}
	
	public boolean hasEnoughMoney(CoinValue coinStorage)
	{
		return tradesPossibleWithStoredMoney(coinStorage) > 0;
	}
	
	public long tradesPossibleWithStoredMoney(CoinValue coinStorage)
	{
		if(this.isFree)
			return 1;
		if(this.cost.getRawValue() == 0)
			return 0;
		long coinValue = coinStorage.getRawValue();
		long price = this.cost.getRawValue();
		return coinValue / price;
	}
	
	@Override
	public void beforeTrade(PreTradeEvent event) {
		this.rules.forEach(rule -> rule.beforeTrade(event));
	}

	@Override
	public void afterTrade(PostTradeEvent event) {
		this.rules.forEach(rule -> rule.afterTrade(event));
	}
	
}