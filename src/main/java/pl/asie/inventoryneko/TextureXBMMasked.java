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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TextureXBMMasked extends TextureAtlasSprite {
	private static final Field ANIMATION_METADATA_SETTER;

	static {
		ANIMATION_METADATA_SETTER = ReflectionHelper.findField(TextureAtlasSprite.class, "animationMetadata", "field_110982_k");
		ANIMATION_METADATA_SETTER.setAccessible(true);
	}

	private final String resDomain;
	private final NekoDefinition nekoDefinition;
	private final NekoState nekoState;

	protected TextureXBMMasked(NekoDefinition d, NekoState a) {
		super("inventoryneko:bitmaps/" + d.getName() + "/" + a.getName());
		this.nekoDefinition = d;
		this.nekoState = a;
		this.resDomain = getIconName().split(":")[0];
	}

	@Override
	public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
		return true;
	}

	private ResourceLocation getXBMLocation(String prefix, String nekoName, String suffix, int frame) {
		String s = prefix + "/" + nekoName + "/" + nekoState.getName();
		if (frame > 0) s += frame;
		if (!"neko".equals(nekoName)) s += "_" + nekoName;

		return new ResourceLocation(resDomain, s + suffix + ".xbm");
	}

	private int[][] getMaskedXBM(IResourceManager manager, int frame) throws IOException {
		XBMFile file = new XBMFile(manager.getResource(getXBMLocation("bitmaps", nekoDefinition.getName(), "", frame)).getInputStream());
		XBMFile fileMask = new XBMFile(manager.getResource(getXBMLocation("bitmasks", nekoDefinition.getMaskName(), "_mask", frame)).getInputStream());

		if (file.getWidth() != fileMask.getWidth() || file.getHeight() != fileMask.getHeight()) {
			throw new RuntimeException("Bitmap and bitmask size mismatch: " + getIconName());
		}

		setIconWidth(file.getWidth());
		setIconHeight(file.getHeight());

		int[][] pixels = new int[Minecraft.getMinecraft().gameSettings.mipmapLevels + 1][];
		pixels[0] = new int[file.getWidth() * file.getHeight()];

		for (int y = 0; y < file.getHeight(); y++) {
			for (int x = 0; x < file.getWidth(); x++) {
				if (fileMask.get(x, y) != 0) {
					pixels[0][y * width + x] = (0xFF000000 | (file.get(x, y) == 0 ? 0xFFFFFF : 0x000000));
				} else {
					pixels[0][y * width + x] = 0;
				}
			}
		}

		return pixels;
	}

	@Override
	public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
		clearFramesTextureData();

		try {
			if (nekoState.getFrames() == 1) {
				framesTextureData.add(getMaskedXBM(manager,0));
			} else {
				List<AnimationFrame> frames = new ArrayList<>(nekoState.getFrames());
				for (int i = 1; i <= nekoState.getFrames(); i++) {
					frames.add(new AnimationFrame(i - 1));
					framesTextureData.add(getMaskedXBM(manager, i));
				}
				ANIMATION_METADATA_SETTER.set(this, new AnimationMetadataSection(frames, getIconWidth(), getIconHeight(), Math.round(nekoDefinition.getTickTime() / 50.0f), false));
			}
		} catch (IOException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return false;
	}
}
