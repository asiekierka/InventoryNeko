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

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemNeko extends Item {
	private final Random random = new Random();

	public NekoDefinition getNekoDefinition(ItemStack stack) {
		String name = InventoryNeko.getOrCreateTagCompound(stack).getString("type");
		if (!InventoryNeko.NEKO.containsKey(name)) {
			name = "neko";
			stack.getTagCompound().setString("type", name);
		}
		return InventoryNeko.NEKO.get(name);
	}

	public NekoState getNekoState(ItemStack stack) {
		String name = InventoryNeko.getOrCreateTagCompound(stack).getString("state");
		if (!InventoryNeko.STATE.containsKey(name)) {
			name = "sleep";
			stack.getTagCompound().setString("state", name);
		}
		return InventoryNeko.STATE.get(name);
	}

	private int updateTick(ItemStack stack, NekoDefinition definition, NekoState state) {
		NBTTagCompound compound = InventoryNeko.getOrCreateTagCompound(stack);
		int tick = compound.hasKey("tick") ? compound.getInteger("tick") : 0;
		compound.setInteger("tick", (tick + 1) % state.getDuration(definition));
		return tick;
	}

	private double getDistance(int i, int slotPosition, int slotWidth) {
		int xDiff = (i % slotWidth) - (slotPosition % slotWidth);
		int yDiff = (i / slotWidth) - (slotPosition / slotWidth);
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

	private int[] findNextSlot(NekoDefinition definition, int slotWidth, int slotHeight, int slotPosition, Function<Integer, ItemStack> stackGetter) {
		int[] count = new int[slotWidth * slotHeight];
		int[] from = new int[slotWidth * slotHeight];
		for (int i = 0; i < slotWidth * slotHeight; i++) {
			from[i] = -1;
		}

		LinkedList<Integer> posQueue = new LinkedList<>();
		posQueue.add(slotPosition);
		from[slotPosition] = -2;

		int lowestCount = Integer.MAX_VALUE;
		int lowestPos = -1;

		while (!posQueue.isEmpty()) {
			int pos = posQueue.remove();

			ItemStack stack = stackGetter.apply(pos);
			if (pos != slotPosition && !stack.isEmpty()) {
				if (definition.isInterestingStack(stack)) {
					if (count[pos] < lowestCount) {
						lowestCount = count[pos];
						lowestPos = pos;
					}
				} else {
					// no can do.
				}
			} else {
				// can trespass
				for (int y = (pos / slotWidth) - 1; y <= (pos / slotWidth) + 1; y++) {
					for (int x = (pos % slotWidth) - 1; x <= (pos % slotWidth) + 1; x++) {
						if (x >= 0 && y >= 0 && x < slotWidth && y < slotHeight) {
							int posNew = y * slotWidth + x;
							if (posNew != pos && (from[posNew] == -1 || (count[pos] + 1) < count[posNew])) {
								ItemStack stackNew = stackGetter.apply(posNew);
								if (definition.isInterestingStack(stackNew) && x != (pos % slotWidth) && y != (pos / slotWidth)) {
									// must be adjacent to attracting stack
									continue;
								}

								posQueue.add(posNew);
								from[posNew] = pos;
								count[posNew] = count[pos] + 1;
							}
						}
					}
				}
			}
		}

		if (lowestPos < 0) {
			return new int[]{-1, -1};
		} else {
			int p = lowestPos;
			int[] p1 = new int[]{lowestPos, lowestPos};
			while (from[p] != -2) {
				p1[1] = p1[0];
				p1[0] = p;
				p = from[p];
			}
			return p1;
		}
	}

	private boolean tryMove(ItemStack stack, int from, int to, Function<Integer, ItemStack> stackGetter, BiConsumer<Integer, ItemStack> stackSetter) {
		if (from == to) {
			return false;
		}

		if (stackGetter.apply(to).isEmpty()) {
			stackSetter.accept(from, ItemStack.EMPTY);
			stackSetter.accept(to, stack);
			return true;
		} else {
			return false;
		}
	}

	public void update(World world, ItemStack stack, int slotWidth, int slotHeight, int slotPosition, Function<Integer, ItemStack> stackGetter, BiConsumer<Integer, ItemStack> stackSetter) {
		if (world.isRemote) {
			return;
		}

		NekoDefinition definition = getNekoDefinition(stack);
		NekoState state = getNekoState(stack);
		int tick = updateTick(stack, definition, state);
		boolean shouldChangeState = tick >= (state.getDuration(definition) - 1);

		if (shouldChangeState) {
			int[] slot = findNextSlot(definition, slotWidth, slotHeight, slotPosition, stackGetter);
			if (slot[0] < 0 || slot[0] >= slotWidth*slotHeight) {
				if (!"sleep".equals(state.getName())) {
					switch (random.nextInt(8)) {
						case 1:
						case 2:
						case 7:
							state = InventoryNeko.STATE.get("sleep");
							break;
						case 5:
							state = InventoryNeko.STATE.get("mati3");
							break;
						default:
							state = InventoryNeko.STATE.get("mati2");
							break;
					}
				}
			} else {
				if (state.is("sleep")) {
					state = InventoryNeko.STATE.get("awake");
				} else {
					ItemStack stackFound = stackGetter.apply(slot[0]);
					if (definition.isInterestingStack(stackFound)) {
						if (state.is("mati2")) {
							switch (random.nextInt(40)) {
								case 5:
								case 7:
								case 9:
								case 11:
									state = InventoryNeko.STATE.get("mati3");
									break;
								case 14:
									state = InventoryNeko.STATE.get("kaki");
									break;
							}
						} else {
							if (random.nextInt(10) > 0 || !(state.is("kaki") || state.is("mati3"))) {
								state = InventoryNeko.STATE.get("mati2");
							}
						}
					} else {
						int xDiff = (slot[0] % slotWidth) - (slotPosition % slotWidth);
						int yDiff = (slot[0] / slotWidth) - (slotPosition / slotWidth);

						if (tryMove(stack, slotPosition, slotPosition + InventoryNeko.sign(xDiff) + InventoryNeko.sign(yDiff) * slotWidth, stackGetter, stackSetter)) {
							slotPosition = slotPosition + InventoryNeko.sign(xDiff) + InventoryNeko.sign(yDiff) * slotWidth;
						} else {
							if (xDiff > yDiff) {
								if (tryMove(stack, slotPosition, slotPosition + InventoryNeko.sign(xDiff), stackGetter, stackSetter)) {
									slotPosition = slotPosition + InventoryNeko.sign(xDiff);
								}

								if (tryMove(stack, slotPosition, slotPosition + InventoryNeko.sign(yDiff) * slotWidth, stackGetter, stackSetter)) {
									slotPosition = slotPosition + InventoryNeko.sign(yDiff) * slotWidth;
								}
							} else {
								if (tryMove(stack, slotPosition, slotPosition + InventoryNeko.sign(yDiff) * slotWidth, stackGetter, stackSetter)) {
									slotPosition = slotPosition + InventoryNeko.sign(yDiff) * slotWidth;
								}

								if (tryMove(stack, slotPosition, slotPosition + InventoryNeko.sign(xDiff), stackGetter, stackSetter)) {
									slotPosition = slotPosition + InventoryNeko.sign(xDiff);
								}
							}
						}

						xDiff = (slot[1] % slotWidth) - (slotPosition % slotWidth);
						yDiff = (slot[1] / slotWidth) - (slotPosition / slotWidth);

						if (xDiff < 0 && yDiff < 0) {
							state = InventoryNeko.STATE.get("upleft");
						} else if (xDiff > 0 && yDiff < 0) {
							state = InventoryNeko.STATE.get("upright");
						} else if (xDiff < 0 && yDiff > 0) {
							state = InventoryNeko.STATE.get("dwleft");
						} else if (xDiff > 0 && yDiff > 0) {
							state = InventoryNeko.STATE.get("dwright");
						} else if (xDiff < 0) {
							state = InventoryNeko.STATE.get("left");
						} else if (xDiff > 0) {
							state = InventoryNeko.STATE.get("right");
						} else if (yDiff < 0) {
							state = InventoryNeko.STATE.get("up");
						} else if (yDiff > 0) {
							state = InventoryNeko.STATE.get("down");
						}
					}
				}
			}

			stack.getTagCompound().setString("state", state.getName());
		}

		stackSetter.accept(slotPosition, stack);
	}

	private int playerToView(int slot) {
		return slot < 9 ? slot + 27 : slot - 9;
	}

	private int viewToPlayer(int slot) {
		return slot >= 27 ? slot - 27 : slot + 9;
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		// Creative mode inventories are bugged
		if (entityIn instanceof EntityPlayer && !(entityIn instanceof FakePlayer) && !((EntityPlayer) entityIn).isCreative() && itemSlot >= 0 && itemSlot < 36) {
			update(worldIn, stack, 9, 4, playerToView(itemSlot),
					(i) -> ((EntityPlayer) entityIn).inventory.getStackInSlot(viewToPlayer(i)),
					(i, s) -> ((EntityPlayer) entityIn).inventory.setInventorySlotContents(viewToPlayer(i), s)
			);
		} else {
			InventoryNeko.getOrCreateTagCompound(stack).setString("state", "sleep");
			InventoryNeko.getOrCreateTagCompound(stack).setInteger("tick", 0);
		}
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (this.isInCreativeTab(tab)) {
			for (String type : InventoryNeko.NEKO.keySet()) {
				ItemStack stack = new ItemStack(this);
				stack.setTagCompound(new NBTTagCompound());
				stack.getTagCompound().setString("type", type);
				items.add(stack);
			}
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return "item.inventoryneko." + getNekoDefinition(stack).getName();
	}
}
