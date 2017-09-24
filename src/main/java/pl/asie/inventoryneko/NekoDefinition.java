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

import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public class NekoDefinition {
	private final Predicate<ItemStack> isInteresting;
	private final String name;
	private final String maskName;
	private final int tickTime;

	public NekoDefinition(String name, int tickTime, Predicate<ItemStack> isInteresting) {
		this(name, name, tickTime, isInteresting);
	}

	public NekoDefinition(String name, String maskName, int tickTime, Predicate<ItemStack> isInteresting) {
		this.name = name;
		this.maskName = maskName;
		this.tickTime = tickTime;
		this.isInteresting = isInteresting;
	}

	public boolean isInterestingStack(ItemStack stack) {
		return !stack.isEmpty() && isInteresting.test(stack);
	}

	public String getName() {
		return name;
	}

	public String getMaskName() {
		return maskName;
	}

	public int getTickTime() {
		return tickTime;
	}

	@Override
	public String toString() {
		return "NekoDefinition{" + name + "}";
	}
}
