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
import static android.os.SystemClock.sleep;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private MainActivity mActivity;

	public ApplicationTest() {
		super(MainActivity.class);
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());
		mActivity = getActivity();

		// Make sure the count is 1, because empty view is considered an item
		assertEquals(mActivity.adapter.getCount(), 1);
	}

	@Test
	public void emptyItemCreated() {
		assertEquals(mActivity.adapter.getCount(), 1);

		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 0)));
		view.check(IS_DISPLAYED_ASSERTION);
	}

	@Test
	public void adapterItemCount() {
		final int COUNT = 5;
		requestCount(COUNT);
		assertEquals(mActivity.adapter.getCount(), COUNT + 1); // 1 for empty item
	}

	@Test
	public void firstItemCreation() {
		final int COUNT = 1;
		requestCount(COUNT);

		// Check if the switch was successful
		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 1)));
		view.check(IS_DISPLAYED_ASSERTION);

		//  Make sure the count has increased
		assertEquals(mActivity.adapter.getCount(), COUNT + 1); // 1 for empty item
	}

	@Test
	public void firstItemRemoval() {
		requestCount(1);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.adapter.onItemRemoved(1);
				mActivity.adapter.requestCount(0);
			}
		});
		// Wait for the switch
		sleep(1000);

		// Check if the pager has fallen back to the empty view
		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 0)));
		view.check(IS_DISPLAYED_ASSERTION);
	}

	@Test
	public void itemRemoval() {
		requestCount(5);

		// Switch to the second to last item
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.pager.setCurrentItem(4); // 1 for empty item
			}
		});

		// Wait for the switch
		sleep(1000);

		// Remove the second to last item
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.adapter.onItemRemoved(4);
				mActivity.adapter.requestCount(4);
			}
		});

		// Wait for the switch
		sleep(1000);

		// Should now be on position 4 but because of caching should still say position 5
		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 5)));
		view.check(IS_DISPLAYED_ASSERTION);


		// Remove last item
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.adapter.onItemRemoved(4);
				mActivity.adapter.requestCount(3);
			}
		});

		// Wait for the switch
		sleep(1000);

		// Should now be on the last position (3)
		view = onView(withText(getString(R.string.numbered_fragment_format, 3)));
		view.check(IS_DISPLAYED_ASSERTION);

		assertEquals(mActivity.pager.getCurrentItem(), 3);
		assertEquals(mActivity.adapter.getCount(), 4);
	}

	@Test
	public void adapterItemGetter() {
		requestCount(10);

		// Neighbours should already be created
		assertNotNull(mActivity.adapter.getItem(0));
		assertNotNull(mActivity.adapter.getItem(1));
		assertNotNull(mActivity.adapter.getItem(2));

		// If pre-creating only neighbours
		if (mActivity.pager.getOffscreenPageLimit() == 1) {
			// Third position should be uncreated
			assertNull(mActivity.adapter.getItem(3));
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mActivity.pager.setCurrentItem(5);
			}
		});

		sleep(1000);

		// Neighbours should already be created
		assertNotNull(mActivity.adapter.getItem(4));
		assertNotNull(mActivity.adapter.getItem(5));
		assertNotNull(mActivity.adapter.getItem(6));
	}

	@Test
	public void preventSwipingToEmptyItem() {
		requestCount(1);

		ViewInteraction pager = onView(withId(R.id.pager));

		pager.perform(swipeRight(), swipeRight());

		// Should still be on position 1
		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 1)));
		view.check(IS_DISPLAYED_ASSERTION);

		pager.perform(ViewActions.swipeLeft(), swipeRight());

		// Should still be on position 1
		view = onView(withText(getString(R.string.numbered_fragment_format, 1)));
		view.check(IS_DISPLAYED_ASSERTION);
	}

	@Test
	public void changeItemsBySwiping() {
		requestCount(10);

		ViewInteraction pager = onView(withId(R.id.pager));
		pager.perform(swipeLeft());

		ViewInteraction view = onView(withText(getString(R.string.numbered_fragment_format, 2)));
		view.check(IS_DISPLAYED_ASSERTION);

		pager.perform(swipeLeft(), swipeLeft());

		view = onView(withText(getString(R.string.numbered_fragment_format, 4)));
		view.check(IS_DISPLAYED_ASSERTION);

		pager.perform(swipeRight(), swipeRight(), swipeRight());

		view = onView(withText(getString(R.string.numbered_fragment_format, 1)));
		view.check(IS_DISPLAYED_ASSERTION);
	}


	/* Helper methods */
	private void requestCount(final int count) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// Increment count
				mActivity.adapter.requestCount(count);
			}
		});

		sleep(1000);
	}

	private String getString(int resId) {
		return getActivity().getString(resId);
	}

	private String getString(int resId, Object... formatArgs) {
		return getActivity().getString(resId, formatArgs);
	}

}