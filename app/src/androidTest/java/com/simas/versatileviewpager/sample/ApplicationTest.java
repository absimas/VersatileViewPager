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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.action.ViewActions.*;
import static com.simas.versatileviewpager.sample.TestUtils.*;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.os.SystemClock.sleep;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {

	public ApplicationTest() {
		super(MainActivity.class);
	}

	MainActivity mActivity;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		injectInstrumentation(InstrumentationRegistry.getInstrumentation());

		mActivity = getActivity();

		// Make sure only the empty item is present
		assertEquals(getActivity().adapter.getCount(), 1);

		// Delay so ViewPager and the empty item are properly set-up
		sleep(300);
	}

	@Test
	public void emptyItemCreated() {
		assertEquals(getActivity().adapter.getCount(), 1);

		ViewInteraction view = onView(withText(getString(R.string.position_format, 0)));
		view.check(IS_COMPLETELY_DISPLAYED);
	}

	@Test
	public void adapterItemCount() {
		final int COUNT = 5;
		setCount(COUNT);
		assertEquals(getActivity().adapter.getCount(), COUNT + 1); // 1 for empty item
	}

	@Test
	public void firstItemCreation() {
		setCount(1);

		// Check if the switch was successful
		ViewInteraction view = onView(withText(getString(R.string.position_format, 1)));
		view.check(IS_COMPLETELY_DISPLAYED);

		//  Make sure the count has increased
		assertEquals(getActivity().adapter.getCount(), 2); // 1 for empty item
	}

	@Test
	public void firstItemRemoval() {
		setCount(1);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.onItemRemoved(1);
			}
		});
		setCount(0);

		// Check if the pager has fallen back to the empty view
		ViewInteraction view = onView(withText(getString(R.string.position_format, 0)));
		view.check(IS_COMPLETELY_DISPLAYED);
	}

	@Test
	public void itemRemoval() {
		setCount(5);

		// Switch to the second to last item // 1 for empty item
		setCurrentItem(4);

		// Remove the second to last item
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.onItemRemoved(4);
			}
		});
		setCount(4);

		// Should now be on position 4 but because of caching should still say position 5
		ViewInteraction view = onView(withText(getString(R.string.position_format, 5)));
		view.check(IS_COMPLETELY_DISPLAYED);


		// Remove last item
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.onItemRemoved(4);
			}
		});
		setCount(3);

		// Should now be on the last position (3)
		view = onView(withText(getString(R.string.position_format, 3)));
		view.check(IS_COMPLETELY_DISPLAYED);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.onItemRemoved(3);
			}
		});
		setCount(2);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.onItemRemoved(2);
			}
		});
		setCount(1);

		assertEquals(getActivity().pager.getCurrentItem(), 1);
		assertEquals(getActivity().adapter.getCount(), 2);
	}

	@Test
	public void adapterItemGetter() {
		setCount(10);

		// Neighbours should already be created
		assertNotNull(getActivity().adapter.getItem(0));
		assertNotNull(getActivity().adapter.getItem(1));
		assertNotNull(getActivity().adapter.getItem(2));

		// If pre-creating only neighbours
		if (getActivity().pager.getOffscreenPageLimit() == 1) {
			// Third position should be uncreated
			assertNull(getActivity().adapter.getItem(3));
		}

		setCurrentItem(5);

		// Neighbours should already be created
		assertNotNull(getActivity().adapter.getItem(4));
		assertNotNull(getActivity().adapter.getItem(5));
		assertNotNull(getActivity().adapter.getItem(6));
	}

	@Test
	public void preventSwipingToEmptyItem() {
		setCount(1);

		ViewInteraction pager = onView(withId(R.id.pager));

		pager.perform(swipeRight(), swipeRight());

		// Should still be on position 1
		ViewInteraction view = onView(withText(getString(R.string.position_format, 1)));
		view.check(IS_COMPLETELY_DISPLAYED);

		pager.perform(ViewActions.swipeLeft(), swipeRight());

		// Should still be on position 1
		view = onView(withText(getString(R.string.position_format, 1)));
		view.check(IS_COMPLETELY_DISPLAYED);
	}

	@Test
	public void changeItemsBySwiping() {
		setCount(10);

		final ViewInteraction pager = onView(withId(R.id.pager));

		for (int i=0; i<3; i++) {
			pager.perform(swipeLeft());
		}
		onView(withText(getString(R.string.position_format, 4)))
				.check(IS_COMPLETELY_DISPLAYED);

		pager.perform(swipeRight(), swipeRight(), swipeRight());
		onView(withText(getString(R.string.position_format, 1)))
				.check(IS_COMPLETELY_DISPLAYED);

		setCurrentItem(10);

		onView(withText(getString(R.string.position_format, 10)))
				.check(IS_COMPLETELY_DISPLAYED);
	}


	/**
	 * This test should be taken lightly as Espresso tests are not menat for directly
	 * manipulating the activity.
	 */
	@Test
	public void itemStateRestoredOnRotate() {
		setCount(1);

		rotate();
		onView(withText(getString(R.string.position_format, 1)))
				.check(matches(withContentDescription("1"))); // Saved once

		rotate();
		onView(withText(getString(R.string.position_format, 1)))
				.check(matches(withContentDescription("2"))); // Saved twice
	}

	/**
	 * This test should be taken lightly as Espresso tests are not menat for directly
	 * manipulating the activity.
	 */
	@Test
	public void selectedItemRestoredOnRotate() {
		setCount(5);
		setCurrentItem(5);

		onView(withText(getString(R.string.position_format, 5)))
				.check(IS_COMPLETELY_DISPLAYED);

		rotate();
		rotate();

		onView(withText(getString(R.string.position_format, 5)))
				.check(IS_COMPLETELY_DISPLAYED);
	}

	/* Helper methods */
	private void setCount(final int count) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().adapter.setCount(count);
			}
		});
		sleep(400);
	}

	private void rotate() {
		TestUtils.rotate(mActivity);
	}

	private void setCurrentItem(final int position) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getActivity().pager.setCurrentItem(position);
			}
		});
		sleep(400);
	}

	private String getString(int resId) {
		return getActivity().getString(resId);
	}

	private String getString(int resId, Object... formatArgs) {
		return getActivity().getString(resId, formatArgs);
	}

}