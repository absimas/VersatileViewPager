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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class MyVersatilePagerAdapter extends VersatilePagerAdapter {

	private final String TAG = getClass().getName();

	public MyVersatilePagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment createItem(final int position) {
		if (position < 1) {
			return new EmptyFragment();
		} else {
			// Return some other fragment
			return new EmptyFragment();
		}
	}

}
