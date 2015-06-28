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
package com.simas.versatileviewpager.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Looper;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAssertion;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;

public class TestUtils {

	public static final ViewAssertion IS_DISPLAYED = matches(isDisplayed());
	public static final ViewAssertion IS_COMPLETELY_DISPLAYED = matches(isCompletelyDisplayed());

	public static void runOnUiThread(Runnable runnable) {
		InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
	}

	public static void rotate(Activity activity) {
		switch (activity.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			default:
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		SystemClock.sleep(800);
	}

}
