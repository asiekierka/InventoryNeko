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

import com.google.common.collect.Sets;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class NekoInventoryTicker {
	private Map<World, List<EntityItemFrame>> itemFrames = new HashMap<>();

	private Set<Object> checkedInventories = Sets.newSetFromMap(new IdentityHashMap<>());

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		if (event.getWorld().isRemote) return;
	}

	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
		if (event.getWorld().isRemote) return;

		itemFrames.remove(event.getWorld());
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote) return;

		if (event.getEntity() instanceof EntityItemFrame) {
			if (!itemFrames.containsKey(event.getWorld())) {
				itemFrames.put(event.getWorld(), new LinkedList<>());
			}

			itemFrames.get(event.getWorld()).add((EntityItemFrame) event.getEntity());
		}
	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
			List<EntityItemFrame> list = itemFrames.get(event.world);
			if (list != null) {
				Iterator<EntityItemFrame> iterator = list.iterator();
				while (iterator.hasNext()) {
					EntityItemFrame frame = iterator.next();
					if (frame.isDead) {
						iterator.remove();
					} else {
						ItemStack stack = frame.getDisplayedItem();
						if (stack.getItem() == InventoryNeko.itemNeko) {
							// TODO: Add item frame logic proxying.
							InventoryNeko.getOrCreateTagCompound(stack).setString("state", "sleep");
							InventoryNeko.getOrCreateTagCompound(stack).setInteger("tick", 0);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			checkedInventories.clear();
		}
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.START || event.player.getEntityWorld().isRemote) {
			return;
		}

		Container c = event.player.openContainer;
		List<Slot> nekoSlots = new ArrayList<>();
		TIntObjectMap<Slot> slots = new TIntObjectHashMap<>();
		TIntObjectMap<Slot> slotsXy = new TIntObjectHashMap<>();
		boolean tickingAllowed = true;
		int lowestPos = 0xFFFFFF;
		Slot lowest = null;

		Set<Object> checkedInventoriesTmp = Sets.newSetFromMap(new IdentityHashMap<>());

		for (Slot slot : c.inventorySlots) {
			if (!(slot.inventory instanceof InventoryPlayer || slot.inventory instanceof InventoryCrafting || slot.inventory instanceof InventoryCraftResult)) {
				if (slot instanceof SlotItemHandler) {
					if (checkedInventories.contains(((SlotItemHandler) slot).getItemHandler())) {
						continue;
					}

					checkedInventoriesTmp.add(((SlotItemHandler) slot).getItemHandler());
				} else if (slot.inventory != null) {
					if (checkedInventories.contains(slot.inventory)) {
						continue;
					}

					checkedInventoriesTmp.add(slot.inventory);
				}
				slots.put(slot.slotNumber, slot);
				int pos = ((slot.xPos & 0xFFF) << 12) | (slot.yPos & 0xFFF);
				if (slotsXy.containsKey(pos)) {
					tickingAllowed = false;
				} else {
					slotsXy.put(pos, slot);
				}

				if (pos < lowestPos) {
					lowestPos = pos;
					lowest = slot;
				}

				ItemStack stack = slot.getStack();
				if (!stack.isEmpty() && stack.getItem() == InventoryNeko.itemNeko) {
					nekoSlots.add(slot);
				}
			}
		}

		checkedInventories.addAll(checkedInventoriesTmp);

		if (nekoSlots.size() == 0 || lowest == null) {
			return;
		}

		int width = 0, height = 0;
		if (tickingAllowed) {
			Slot x = lowest;

			while (x != null) {
				width++;
				x = slotsXy.get((((x.xPos + 18) & 0xFFF) << 12) | (x.yPos & 0xFFF));
			}
			x = lowest;
			while (x != null) {
				height++;
				for (int i = 0; i < width; i++) {
					if (!slotsXy.containsKey((((x.xPos + 18) & 0xFFF) << 12) | (x.yPos & 0xFFF))) {
						tickingAllowed = false;
					}
				}
				x = slotsXy.get(((x.xPos & 0xFFF) << 12) | ((x.yPos + 18) & 0xFFF));
			}
		}

		int offX = lowest.xPos;
		int offY = lowest.yPos;

		final int fwidth = width;

		for (Slot s : nekoSlots) {
			if (tickingAllowed) {
				ItemStack stack = s.getStack();
				InventoryNeko.itemNeko.update(
						event.player.getEntityWorld(),
						stack, width, height,
						((s.yPos - offY) / 18) * width + ((s.xPos - offX) / 18),
						(i) -> slotsXy.get((((offX + (i % fwidth) * 18) & 0xFFF) << 12) | (((offY + (i / fwidth) * 18) & 0xFFF))).getStack(),
						(i, st) -> slotsXy.get((((offX + (i % fwidth) * 18) & 0xFFF) << 12) | (((offY + (i / fwidth) * 18) & 0xFFF))).putStack(st)
				);
			} else {
				s.getStack().getTagCompound().setString("state", "sleep");
				s.getStack().getTagCompound().setInteger("tick", 0);
			}
		}

		if (nekoSlots.size() > 0) {
			c.detectAndSendChanges();
		}
	}
}
