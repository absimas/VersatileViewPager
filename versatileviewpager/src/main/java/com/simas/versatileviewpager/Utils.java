/*
 * Copyright (c) 2015. Simas Abramovas
 *
 * This file is part of VersatileViewPager.
 *
 * VersatileViewPager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VersatileViewPager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VersatileViewPager. If not, see <http://www.gnu.org/licenses/>.
 */
package com.simas.versatileviewpager;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

/**
 * Helper methods
 */
public class Utils {

	public static Bitmap screenshot(View v) {
		v.setDrawingCacheEnabled(true);
		Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
		v.setDrawingCacheEnabled(false);

		return b;
	}

	public static Bitmap screenshot2(View v) {
		Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(b);
		v.draw(canvas);

		return b;
	}

}
