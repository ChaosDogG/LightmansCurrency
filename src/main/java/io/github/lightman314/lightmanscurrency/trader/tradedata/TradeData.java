package io.github.lightman314.lightmanscurrency.trader.tradedata;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.events.TradeEvent.PostTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.PreTradeEvent;
import io.github.lightman314.lightmanscurrency.events.TradeEvent.TradeCostEvent;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.ITradeRuleHandler;
import io.github.lightman314.lightmanscurrency.trader.tradedata.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import net.minecraft.nbt.CompoundTag;

public abstract class TradeData implements ITradeRuleHandler {

	public static final String DEFAULT_KEY = "Trades";
	
	public enum TradeDirection { SALE, PURCHASE, NONE }
	
	protected CoinValue cost = new CoinValue();
	
	List<TradeRule> rules = new ArrayList<>();
	
	public abstract TradeDirection getTradeDirection();
	
	public final boolean validCost()
	{
		return this.cost.isFree() || cost.getRawValue() > 0;
	}
	
	public boolean isValid()
	{
		return validCost();
	}
	
	public CoinValue getCost()
	{
		return this.cost;
	}
	
	public void setCost(CoinValue value)
	{
		this.cost = value;
	}
	
	@Deprecated
	public boolean isFree()
	{
		return this.cost.isFree();
	}
	
	@Deprecated
	public void setFree(boolean free)
	{
		this.cost.setFree(free);
	}
	
	public CompoundTag getAsNBT()
	{
		CompoundTag tradeNBT = new CompoundTag();
		this.cost.writeToNBT(tradeNBT,"Price");
		TradeRule.writeRules(tradeNBT, this.rules);
		
		return tradeNBT;
	}
	
	protected void loadFromNBT(CompoundTag nbt)
	{
		cost.readFromNBT(nbt, "Price");
		//Set whether it's free or not
		if(nbt.contains("IsFree"))
			this.cost.setFree(nbt.getBoolean("IsFree"));
		
		this.rules.clear();
		this.rules = TradeRule.readRules(nbt);
		
	}
	
	@Override
	public void beforeTrade(PreTradeEvent event) {
		this.rules.forEach(rule -> rule.beforeTrade(event));
	}

	public void tradeCost(TradeCostEvent event)
	{
		this.rules.forEach(rule -> rule.tradeCost(event));
	}
	
	@Override
	public void afterTrade(PostTradeEvent event) {
		this.rules.forEach(rule -> rule.afterTrade(event));
	}
	
	public void addRule(TradeRule newRule)
	{
		if(newRule == null)
			return;
		//Confirm a lack of duplicate rules
		for(int i = 0; i < this.rules.size(); i++)
		{
			if(newRule.type == this.rules.get(i).type)
				return;
		}
		this.rules.add(newRule);
	}
	
	public List<TradeRule> getRules() { return this.rules; }
	
	public void setRules(List<TradeRule> rules) { this.rules = rules; }
	
	public void removeRule(TradeRule rule)
	{
		if(this.rules.contains(rule))
			this.rules.remove(rule);
	}
	
	public void clearRules()
	{
		this.rules.clear();
	}
	
	public void markRulesDirty() { }
	
}