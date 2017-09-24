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

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.*;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.vecmath.Vector3f;

public class ProxyClient extends ProxyCommon {
	private ModelResourceLocation getLocation(NekoDefinition definition, NekoState animation) {
		return new ModelResourceLocation("inventoryneko:" + definition.getName(), animation.getName());
	}


	public void addTransformation(ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> transformMap, ItemCameraTransforms.TransformType type, TRSRTransformation transformation) {
		transformMap.put(type, TRSRTransformation.blockCornerToCenter(transformation));
	}

	public void addThirdPersonTransformation(ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> transformMap, TRSRTransformation transformation) {
		addTransformation(transformMap, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, transformation);
		addTransformation(transformMap, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND,  toLeftHand(transformation));
	}

	public void addFirstPersonTransformation(ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> transformMap, TRSRTransformation transformation) {
		addTransformation(transformMap, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, transformation);
		addTransformation(transformMap, ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,  toLeftHand(transformation));
	}

	private static final TRSRTransformation flipX = new TRSRTransformation(null, null, new Vector3f(-1, 1, 1), null);

	private static TRSRTransformation toLeftHand(TRSRTransformation transform) {
		return TRSRTransformation.blockCenterToCorner(flipX.compose(TRSRTransformation.blockCornerToCenter(transform)).compose(flipX));
	}

	private static TRSRTransformation getTransformation(float tx, float ty, float tz, float ax, float ay, float az, float s) {
		return TRSRTransformation.blockCenterToCorner(new TRSRTransformation(
				new Vector3f(tx / 16, ty / 16, tz / 16),
				TRSRTransformation.quatFromXYZDegrees(new Vector3f(ax, ay, az)),
				new Vector3f(s, s, s),
				null));
	}

	public ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> getDefaultItemTransforms() {
		ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> transformMapBuilder = ImmutableMap.builder();
		TRSRTransformation thirdperson = getTransformation(0, 3, 1, 0, 0, 0, 0.55f);
		TRSRTransformation firstperson = getTransformation(1.13f, 3.2f, 1.13f, 0, -90, 25, 0.68f);
		addTransformation(transformMapBuilder, ItemCameraTransforms.TransformType.GROUND, getTransformation(0, 2, 0, 0, 0, 0, 0.5f));
		addTransformation(transformMapBuilder, ItemCameraTransforms.TransformType.HEAD, getTransformation(0, 13, 7, 0, 180, 0, 1));
		addThirdPersonTransformation(transformMapBuilder, thirdperson);
		addFirstPersonTransformation(transformMapBuilder, firstperson);
		return transformMapBuilder.build();
	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		ProgressManager.ProgressBar bar = ProgressManager.push("InventoryNeko: baking models", InventoryNeko.NEKO.size() * InventoryNeko.STATE.size());

		for (NekoDefinition neko : InventoryNeko.NEKO.values()) {
			for (NekoState animation : InventoryNeko.STATE.values()) {
				String texName = "inventoryneko:bitmaps/" + neko.getName() + "/" + animation.getName();
				IBakedModel model = ItemLayerModel.INSTANCE.retexture(ImmutableMap.of("layer0", texName)).bake(
						new SimpleModelState(getDefaultItemTransforms()),
						DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
				event.getModelRegistry().putObject(getLocation(neko, animation), model);
				bar.step(neko.getName() + "." + animation.getName());
			}
		}

		ProgressManager.pop(bar);
	}

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		for (NekoDefinition neko : InventoryNeko.NEKO.values()) {
			for (NekoState animation : InventoryNeko.STATE.values()) {
				event.getMap().setTextureEntry(new TextureXBMMasked(neko, animation));
			}
		}
	}

	@SubscribeEvent
	public void onRegisterModels(ModelRegistryEvent event) {
		ModelLoader.setCustomMeshDefinition(InventoryNeko.itemNeko, stack -> getLocation(
				InventoryNeko.itemNeko.getNekoDefinition(stack),
				InventoryNeko.itemNeko.getNekoState(stack))
		);
	}
}
