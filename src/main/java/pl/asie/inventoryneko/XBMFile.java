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

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XBMFile {
	private static final Pattern WIDTH = Pattern.compile("_width (\\d+)");
	private static final Pattern HEIGHT = Pattern.compile("_height (\\d+)");
	private static final Pattern HEX = Pattern.compile("0x([0-9a-fA-F]+)");

	private int width = 0, height = 0;
	private int[] data = null;

	public XBMFile(InputStream stream) throws IOException {
		List<String> strings = IOUtils.readLines(stream, Charsets.US_ASCII);
		boolean foundWidth = false, foundHeight = false;
		int i = 0;

		for (String s : strings) {
			if (data != null) {
				Matcher hexMatcher = HEX.matcher(s);
				while (hexMatcher.find()) {
					data[i++] = Integer.parseInt(hexMatcher.group(1), 16);
				}
			} else {
				Matcher m = WIDTH.matcher(s);
				if (m.find()) {
					String widthStr = m.group(1);
					if (widthStr != null) {
						width = new Integer(widthStr);
						foundWidth = true;
					}
				}

				m = HEIGHT.matcher(s);
				if (m.find()) {
					String heightStr = m.group(1);
					if (heightStr != null) {
						height = new Integer(heightStr);
						foundHeight = true;
					}
				}

				if (foundWidth && foundHeight) {
					data = new int[width * height / 8];
					i = 0;
				}
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int get(int x, int y) {
		int offset = (y * width + x);
		return (data[offset >> 3] >> (offset & 7)) & 1;
	}
}
