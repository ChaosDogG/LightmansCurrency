package io.github.lightman314.lightmanscurrency.money;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.MoneyData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

public class CoinData
{
	//Coin item
	public final Item coinItem;
	//Coins chain id
	public final String chain;
	//Value inputs
	public final Item worthOtherCoin;
	public final int worthOtherCoinCount;
	//Coin's display initial 'c','d', etc.
	private final String initialTranslation;
	//Is this hidden or not
	public final boolean isHidden;
	
	private CoinData(Builder builder)
	{
		this.coinItem = builder.coinItem;
		this.chain = builder.chain;
		this.worthOtherCoin = builder.worthOtherCoin;
		this.worthOtherCoinCount = builder.worthOtherCoinCount;
		this.initialTranslation = builder.initialTranslation;
		this.isHidden = builder.isHidden;
	}
	
	public long getValue()
	{
		return getValue(MoneyUtil.getMoneyData());
	}
	
	public long getValue(MoneyData moneyData)
	{
		if(moneyData == null)
			return 0;
		if(!this.convertsDownwards())
		{
			//LightmansCurrency.LOGGER.info("CoinData.getValue() returning 1 due to being a bottom-coin.");
			return 1;
		}
		CoinData otherCoinData = moneyData.getData(this.worthOtherCoin);
		if(otherCoinData != null)
		{
			//LightmansCurrency.LOGGER.info("CoinData.getValue() calculated value of " + this.worthOtherCoinCount * otherCoinData.getValue() + ".");
			return this.worthOtherCoinCount * otherCoinData.getValue(moneyData);
		}
		else
		{
			LightmansCurrency.LogError("CoinData.getValue() for " + this.coinItem.getRegistryName() + " returning 1 due it's dependent coin (" + this.worthOtherCoin.getRegistryName() + ") not being registered.");
			return 1;
		}
	}
	
	/**
	 * Gets the display value in double format
	 */
	public double getDisplayValue()
	{
		double coreValue = this.getValue();
		double baseValue = MoneyUtil.getValue(Config.getBaseCoinItem());
		//LightmansCurrency.LogInfo("Core Value of " + this.getCoinItem().getRegistryName() + "=" + coreValue + "\nBase Value of " + Config.getBaseCoinItem().getRegistryName() + "=" + baseValue + "\nDisplay Value: " + coreValue/baseValue);
		return coreValue / baseValue;
	}
	
	public boolean convertsDownwards()
	{
		return this.worthOtherCoin != null && this.worthOtherCoinCount > 0;
	}
	
	public Pair<Item,Integer> getDownwardConversion()
	{
		return new Pair<>(this.worthOtherCoin, this.worthOtherCoinCount);
	}
	
	public Component getInitial()
	{
		if(this.initialTranslation != null && !this.initialTranslation.isBlank())
			return new TranslatableComponent(this.initialTranslation);
		//LightmansCurrency.LogWarning("No initial found for the coin '" + this.coinItem.getRegistryName().toString() + "'.");
		return new TextComponent(this.coinItem.getName(new ItemStack(this.coinItem)).getString().substring(0,1).toLowerCase());
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("coinitem", this.coinItem.getRegistryName().toString());
		json.addProperty("chain", this.chain);
		if(this.worthOtherCoin != null && this.worthOtherCoinCount > 0)
		{
			JsonObject worth = new JsonObject();
			worth.addProperty("coin", this.worthOtherCoin.getRegistryName().toString());
			worth.addProperty("count", this.worthOtherCoinCount);
			json.add("worth", worth);
		}
		if(this.initialTranslation != null && !this.initialTranslation.isBlank())
			json.addProperty("initial", this.initialTranslation);
		if(this.isHidden)
			json.addProperty("hidden", true);
		
		return json;
	}
	
	public static Builder getBuilder(ItemLike coinItem, String chain)
	{
		return new Builder(coinItem.asItem(), chain);
	}
	
	public static Builder getBuilder(JsonObject json)
	{
		//Coin Item
		Item coinItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("coinitem").getAsString()));
		String chain = json.get("chain").getAsString();
		Builder builder = new Builder(coinItem, chain);
		//Relative Worth
		if(json.has("worth"))
		{
			JsonObject worthData = json.get("worth").getAsJsonObject();
			Item otherCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation(worthData.get("coin").getAsString()));
			int count = worthData.get("count").getAsInt();
			builder.defineConversion(otherCoin, count);
		}
		//Initial
		if(json.has("initial"))
			builder.defineInitial(json.get("initial").getAsString());
		//Hidden
		if(json.has("hidden") && json.get("hidden").getAsBoolean())
			builder.setHidden();
		return builder;
	}
	
	public static class Builder
	{
		
		//The coin's item
		final Item coinItem;
		//The coin's conversion chain
		final String chain;
		//Defines its worth based on another coin's value
		Item worthOtherCoin = null;
		int worthOtherCoinCount = 0;
		//The shortened name of the coin
		String initialTranslation = "";
		//Whether it's publicly visible
		boolean isHidden = false;
		
		
		public Builder(@Nonnull Item coinItem, String chain)
		{
			this.coinItem = coinItem;
			this.chain = chain;
		}
		
		/**
		 * Defines what lesser coin can be converted into this one, and how many of those coins are worth 1 of this coin.
		 */
		public Builder defineConversion(ItemLike otherCoin, int coinAmount)
		{
			this.worthOtherCoin = otherCoin.asItem();
			this.worthOtherCoinCount = coinAmount;
			return this;
		}
		
		/**
		 * Defines the coins initial used in displaying the short form of an price/value;
		 */
		public Builder defineInitial(String translationString)
		{
			this.initialTranslation = translationString;
			return this;
		}
		
		public Builder setHidden()
		{
			this.isHidden = true;
			return this;
		}
		
		public CoinData build()
		{
			return new CoinData(this);
		}
	}
	
}
