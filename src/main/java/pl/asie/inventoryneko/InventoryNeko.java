/*
 * Copyright (c) 2017 Adrian Siekierka
 *
 * This file is part of InventoryNeko.
 *
 * InventoryNeko is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * InventoryNeko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with InventoryNeko.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.inventoryneko;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod(
		modid = "inventoryneko",
		name = "InventoryNeko",
		version = "${version}",
		acceptedMinecraftVersions = "[1.12,1.13)"
)
public class InventoryNeko {
	public static final Map<String, NekoDefinition> NEKO = new HashMap<>();
	public static final Map<String, NekoState> STATE = new HashMap<>();
	public static ItemNeko itemNeko;

	@SidedProxy(modId = "inventoryneko", clientSide = "pl.asie.inventoryneko.ProxyClient", serverSide = "pl.asie.inventoryneko.ProxyCommon")
	public static ProxyCommon proxy;
	public static Configuration config;

	private static Multimap<String, Item> itemMultimap = HashMultimap.create();

	public static void registerNeko(NekoDefinition definition) {
		if ("neko".equals(definition.getName()) || config.getBoolean(definition.getName(), "enabled", true, "Is " + definition.getName() + " enabled?")) {
			NEKO.put(definition.getName(), definition);
		}
	}

	public static void registerState(NekoState animation) {
		STATE.put(animation.getName(), animation);
	}

	public static NBTTagCompound getOrCreateTagCompound(ItemStack stack) {
		if (stack.hasTagCompound()) {
			return stack.getTagCompound();
		}

		NBTTagCompound compound = new NBTTagCompound();
		stack.setTagCompound(compound);
		return compound;
	}

	public static int sign(int v) {
		return Integer.compare(v, 0);
	}

	public static boolean isFood(ItemStack stack) {
		if (stack.getItem() instanceof ItemFood) {
			return true;
		}

		if (stack.getItem() == Items.CAKE) {
			return true;
		}

		if (itemMultimap.get("food").contains(stack.getItem())) {
			return true;
		}

		return false;
	}

	public static boolean isFlower(ItemStack stack) {
		if (Block.getBlockFromItem(stack.getItem()) instanceof BlockFlower) {
			return true;
		}

		if (stack.getItem() == Items.CAKE) {
			return true;
		}

		if (itemMultimap.get("flower").contains(stack.getItem())) {
			return true;
		}

		return false;
	}

	private void addItems(String name) {
		String[] items = config.getStringList(name, "customItems", new String[0], "Use registry names.");
		for (String s : items) {
			if (s.indexOf(':') >= 0) {
				ResourceLocation loc = new ResourceLocation(s);
				if (ForgeRegistries.ITEMS.containsKey(loc)) {
					itemMultimap.put(name, ForgeRegistries.ITEMS.getValue(loc));
				}
			}
		}
	}

	@Mod.EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());

		registerNeko(new NekoDefinition("neko", 250, InventoryNeko::isFood));
		registerNeko(new NekoDefinition("tora", "neko", 250, InventoryNeko::isFood));
		registerNeko(new NekoDefinition("dog", 250, InventoryNeko::isFood));
		registerNeko(new NekoDefinition("sakura", 250, InventoryNeko::isFlower));
		registerNeko(new NekoDefinition("tomoyo", 250, InventoryNeko::isFlower));

		registerState(new NekoState("awake", 1, 3));
		registerState(new NekoState("dtogi", 2, 10));
		registerState(new NekoState("jare2", 1, 10));
		registerState(new NekoState("kaki", 2, 4));
		registerState(new NekoState("ltogi", 2, 10));
		registerState(new NekoState("mati2", 1, 3));
		registerState(new NekoState("mati3", 1, 3));
		registerState(new NekoState("rtogi", 2, 10));
		registerState(new NekoState("sleep", 2, 4));
		registerState(new NekoState("utogi", 2, 10));

		registerState(new NekoState("down", 2, 2));
		registerState(new NekoState("dwleft", 2, 2));
		registerState(new NekoState("dwright", 2, 2));
		registerState(new NekoState("left", 2, 2));
		registerState(new NekoState("right", 2, 2));
		registerState(new NekoState("up", 2, 2));
		registerState(new NekoState("upleft", 2, 2));
		registerState(new NekoState("upright", 2, 2));

		itemNeko = new ItemNeko();
		itemNeko.setCreativeTab(CreativeTabs.MISC);
		itemNeko.setRegistryName("inventoryneko:neko");
		itemNeko.setUnlocalizedName("inventoryneko.neko");

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new NekoInventoryTicker());
		MinecraftForge.EVENT_BUS.register(proxy);
	}

	@Mod.EventHandler
	public void onInit(FMLInitializationEvent event) {
		addItems("food");
		addItems("flower");

		if (config.hasChanged()) {
			config.save();
		}
	}

	@SubscribeEvent
	public void onRegisterItems(RegistryEvent.Register<Item> event) {
		event.getRegistry().register(itemNeko);
	}
}
