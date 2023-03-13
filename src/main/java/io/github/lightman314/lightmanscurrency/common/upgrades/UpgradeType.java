package io.github.lightman314.lightmanscurrency.common.upgrades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.items.UpgradeItem;
import io.github.lightman314.lightmanscurrency.common.upgrades.types.SpeedUpgrade;
import io.github.lightman314.lightmanscurrency.common.upgrades.types.capacity.ItemCapacityUpgrade;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.eventbus.api.Event;

public abstract class UpgradeType {

	private static final Map<ResourceLocation,UpgradeType> UPGRADE_TYPE_REGISTRY = new HashMap<>();
	
	public static final ItemCapacityUpgrade ITEM_CAPACITY = register(new ResourceLocation(LightmansCurrency.MODID, "item_capacity"), new ItemCapacityUpgrade());
	
	public static final SpeedUpgrade SPEED = register(new ResourceLocation(LightmansCurrency.MODID, "speed"), new SpeedUpgrade());
	
	public static final Simple NETWORK = register(new ResourceLocation(LightmansCurrency.MODID, "trader_network"), new Simple(EasyText.translatable("tooltip.lightmanscurrency.upgrade.network")));
	
	public static final Simple HOPPER = register(new ResourceLocation(LightmansCurrency.MODID, "hopper"), new Simple(EasyText.translatable("tooltip.lightmanscurrency.upgrade.hopper")));
	
	private ResourceLocation type;
	
	protected abstract List<String> getDataTags();
	protected abstract Object defaultTagValue(String tag);
	public List<ITextComponent> getTooltip(UpgradeData data) { return Lists.newArrayList(); }
	public final UpgradeData getDefaultData() { return new UpgradeData(this); }
	
	public UpgradeType setRegistryName(ResourceLocation name) {
		this.type = name;
		return this;
	}

	public ResourceLocation getRegistryName() {
		return this.type;
	}

	public Class<UpgradeType> getRegistryType() {
		return UpgradeType.class;
	}
	
	private static <T extends UpgradeType> T register(ResourceLocation type, T upgradeType)
	{
		upgradeType.setRegistryName(type);
		UPGRADE_TYPE_REGISTRY.put(type, upgradeType);
		return upgradeType;
	}
	
	public interface IUpgradeable
	{
		default boolean allowUpgrade(UpgradeItem item) { return this.allowUpgrade(item.getUpgradeType()); }
		boolean allowUpgrade(UpgradeType type);
	}
	
	public interface IUpgradeItem
	{
		UpgradeType getUpgradeType();
		UpgradeData getDefaultUpgradeData();
		default void onApplied(IUpgradeable target) { }
	}
	
	public static class UpgradeData
	{

		public static final UpgradeData EMPTY = new UpgradeData();

		private final Map<String,Object> data = new HashMap<>();
		
		public Set<String> getKeys() { return data.keySet(); }
		
		public boolean hasKey(String tag)
		{
			return this.getKeys().contains(tag);
		}

		private UpgradeData() {}

		public UpgradeData(UpgradeType upgrade)
		{
			for(String tag : upgrade.getDataTags())
			{
				Object defaultValue = upgrade.defaultTagValue(tag);
				data.put(tag, defaultValue);
			}
		}
		
		public void setValue(String tag, Object value)
		{
			if(data.containsKey(tag))
				data.put(tag, value);
		}
		
		public Object getValue(String tag)
		{
			if(data.containsKey(tag))
				return data.get(tag);
			return null;
		}
		
		public int getIntValue(String tag)
		{
			Object value = getValue(tag);
			if(value instanceof Integer)
				return (Integer)value;
			return 0;
		}
		
		public float getFloatValue(String tag)
		{
			Object value = getValue(tag);
			if(value instanceof Float)
				return (Float)value;
			return 0f;
		}
		
		public String getStringValue(String tag)
		{
			Object value = getValue(tag);
			if(value instanceof String)
				return (String)value;
			return "";
		}
		
		public void read(CompoundNBT compound)
		{
			compound.getAllKeys().forEach(key ->{
				if(this.hasKey(key))
				{
					if(compound.contains(key, Constants.NBT.TAG_INT))
						this.setValue(key, compound.getInt(key));
					else if(compound.contains(key, Constants.NBT.TAG_FLOAT))
						this.setValue(key, compound.getFloat(key));
					else if(compound.contains(key, Constants.NBT.TAG_STRING))
						this.setValue(key, compound.getString(key));
				}
			});
		}
		
		public CompoundNBT writeToNBT() { return writeToNBT(null); }
		
		public CompoundNBT writeToNBT(@Nullable UpgradeType source)
		{
			Map<String,Object> modifiedEntries = source == null ? this.data : getModifiedEntries(this,source);
			CompoundNBT compound = new CompoundNBT();
			modifiedEntries.forEach((key,value) ->{
				if(value instanceof Integer)
					compound.putInt(key, (Integer)value);
				else if(value instanceof Float)
					compound.putFloat(key, (Float)value);
				else if(value instanceof String)
					compound.putString(key, (String)value);
			});
			return compound;
		}
		
		public static Map<String,Object> getModifiedEntries(UpgradeData queryData, UpgradeType source)
		{
			Map<String,Object> modifiedEntries = Maps.newHashMap();
			source.getDefaultData().data.forEach((key, value) -> {
				if(queryData.data.containsKey(key) && !Objects.equal(queryData.data.get(key), value))
						modifiedEntries.put(key, value);
			});
			return modifiedEntries;
		}
		
		
		
	}
	
	public static class RegisterUpgradeTypeEvent extends Event{
		
		public RegisterUpgradeTypeEvent() { }
		
		public <T extends UpgradeType> void Register(ResourceLocation type, T upgradeType) { register(type, upgradeType); }
		
	}
	
	
	public static boolean hasUpgrade(UpgradeType type, IInventory upgradeContainer) {
		for(int i = 0; i < upgradeContainer.getContainerSize(); ++i)
		{
			ItemStack stack = upgradeContainer.getItem(i);
			if(stack.getItem() instanceof UpgradeItem)
			{
				UpgradeItem upgradeItem = (UpgradeItem)stack.getItem();
				if(upgradeItem.getUpgradeType() == type)
					return true;
			}
		}
		return false;
	}
	
	public static class Simple extends UpgradeType {

		private final List<ITextComponent> tooltips;
		public Simple(ITextComponent... tooltips) { this.tooltips = Lists.newArrayList(tooltips); }
		
		@Override
		protected List<String> getDataTags() { return new ArrayList<>(); }

		@Override
		protected Object defaultTagValue(String tag) { return null; }
		
		@Override
		public List<ITextComponent> getTooltip(UpgradeData data) { return this.tooltips; }
		
	}
	
}