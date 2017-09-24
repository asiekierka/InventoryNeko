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

public class NekoState {
	private String name;
	private int duration;
	private int frames;

	public NekoState(String name, int frames, int duration) {
		this.name = name;
		this.frames = frames;
		this.duration = frames * duration;
	}

	public boolean is(String name) {
		return this.name.equals(name);
	}

	public String getName() {
		return name;
	}

	public int getFrames() {
		return frames;
	}

	public int getDuration(NekoDefinition nekoDefinition) {
		return duration * Math.round(nekoDefinition.getTickTime() / 50.0f);
	}

	@Override
	public String toString() {
		return "NekoState{" + name + "}";
	}
}
