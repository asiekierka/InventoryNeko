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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

import java.util.Random;

public class LootFunctionRandomizeNeko extends LootFunction {
	protected LootFunctionRandomizeNeko(LootCondition[] conditionsIn) {
		super(conditionsIn);
	}

	@Override
	public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
		InventoryNeko.getOrCreateTagCompound(stack).setString("type", InventoryNeko.NEKO_POOL.get(rand.nextInt(InventoryNeko.NEKO_POOL.size())));
		return stack;
	}

	public static class Serializer extends LootFunction.Serializer<LootFunctionRandomizeNeko> {
		public Serializer(ResourceLocation location) {
			super(location, LootFunctionRandomizeNeko.class);
		}

		@Override
		public void serialize(JsonObject object, LootFunctionRandomizeNeko functionClazz, JsonSerializationContext serializationContext) {

		}

		@Override
		public LootFunctionRandomizeNeko deserialize(JsonObject object, JsonDeserializationContext deserializationContext, LootCondition[] conditionsIn) {
			return new LootFunctionRandomizeNeko(conditionsIn);
		}
	}
}
