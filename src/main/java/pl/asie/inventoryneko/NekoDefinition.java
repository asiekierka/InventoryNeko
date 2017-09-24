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

public class NekoDefinition {
	private String name;
	private String maskName;
	private int tickTime;

	public NekoDefinition(String name, int tickTime) {
		this(name, name, tickTime);
	}


	public NekoDefinition(String name, String maskName, int tickTime) {
		this.name = name;
		this.maskName = maskName;
		this.tickTime = tickTime;
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
