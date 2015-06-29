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

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.view.*;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <h4>How it works:</h4>
 * When deleting it switches to its neighbour with {@link #setCurrentItem(int)}, however if both
 * of the neighbours are deleted based on {@link VersatilePagerAdapter#getRealCount()}, then it
 * will switch to the last position based the real count. <br/><br/>
 * To switch seamlessly after the current item is no longer in at the current position, the pager
 * switches to the neighbour (forward if there are any, otherwise backwards). After switching to
 * the neighbour an image depicting the current view will be overlain while the real position
 * switching takes place by invoking {@link VersatilePagerAdapter#notifyDataSetChanged()}. <br/><br/>
 * While this two step switch takes place, the {@link android.support.v4.view.ViewPager
 * .OnPageChangeListener}s are disabled.
 * <h5>Notes:</h5>
 * <ul>
 *     <li>The preview will be added as sibling and can block the view of widgets like DrawerLayout.
 *     To avoid that, you may need to wrap this pager in a separate container, e.g. a
 *     {@link RelativeLayout}</li>
 *     <li>The item at position 0 is always the empty item</li>
 *     <li>The over-scroll disabled because of the left-most, i.e. the empty, item.</li>
 *     <li>Although you shouldn't be able to scroll to the empty item, it can still be selected
 *     with {@code setCurrentItem(0)}</li>
 * </ul>
 */
public class VersatileViewPager extends ViewPager {

	private final String TAG = getClass().getName();
	private CopyOnWriteArraySet<OnPageChangeListener> mListeners = new CopyOnWriteArraySet<>();
	private boolean mOnPageChangeListenersEnabled = true;
	private float mStartDragX;

	/* Overlay */
	private ImageView mOverlayImage;
	private ViewGroup mPagerParent, mPreviewOverlay;
	private int mRemovedPosition;
	private final Utils.PausableHandler mPausableHandler = new Utils.PausableHandler();
	private ViewPager.SimpleOnPageChangeListener mTemporarySwitchListener = new ViewPager
			.SimpleOnPageChangeListener() {
		private boolean mIgnoreFurtherCalls;
		@Override
		public void onPageScrollStateChanged(int state) {
			super.onPageScrollStateChanged(state);
			if (state == ViewPager.SCROLL_STATE_IDLE && !mIgnoreFurtherCalls) {
				mIgnoreFurtherCalls = true;
				// Overlay and image while working (prevent flickering)
				mOverlayImage.setImageBitmap(Utils.screenshot(VersatileViewPager.this));
				mPagerParent.addView(mPreviewOverlay);

				// Change the count and notify (for real now)
				getAdapter().useRealCount();
				getAdapter().notifyDataSetChanged();

				// Switch to the unused page, it's populated by notifyDataSetChanged
				if (mRemovedPosition != -1) {
					setCurrentItem(mRemovedPosition, false);
				}

				// When switches have settled, remove the preview and re-enable scrolling
				mPagerParent.post(new Runnable() {
					@Override
					public void run() {
						// Deal with the listener via the super method, to avoid saving/removing it
						VersatileViewPager.super
								.removeOnPageChangeListener(mTemporarySwitchListener);
						// Re-enable the default listeners before invoking a method that must be caught
						setListenersEnabled(true);
						mIgnoreFurtherCalls = false;
						mPagerParent.removeView(mPreviewOverlay);
						setEnabled(true);
						// Resume other messages
						mPausableHandler.setPaused(false);
					}
				});
			}
		}
	};
	DataSetObserver mObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
				throw new IllegalStateException("Must be notified on the main thread!");
			}
			mPausableHandler.post(new Runnable() {
				@Override
				public void run() {
					Object primaryItem = getAdapter().getPrimaryItem();
					if (primaryItem != null) {
						int primaryPos = getAdapter().getItemPosition(primaryItem);
						if (primaryPos == PagerAdapter.POSITION_NONE) {
							// Prevent other switches until finished
							mPausableHandler.setPaused(true);

							// Disable scrolling
							setEnabled(false);

							mRemovedPosition = getCurrentItem();
							// Prevent default listeners from being called
							setListenersEnabled(false);
							// Deal with the listener via the super method, to avoid saving/removing it
							VersatileViewPager.super
									.addOnPageChangeListener(mTemporarySwitchListener);

							if (getCurrentItem() == getAdapter().getCount() - 1 &&
									getCurrentItem() - 1 <= getAdapter().getRealCount() + 1) {
								// - 1 for previous; + 1 for empty item
								// Switch to previous item if it's available
								setCurrentItem(getCurrentItem() - 1);
							} else if (getCurrentItem() + 1 <= getAdapter().getRealCount() + 1) {
								// + 1 for next; + 1 for empty item
								// Switch to next item if it's available
								setCurrentItem(getCurrentItem() + 1);
							} else {
								// Otherwise switch to the last available item
								setListenersEnabled(true);
								setCurrentItem(getAdapter().getRealCount());
								mRemovedPosition = -1;
							}
							return;
						}
					}
					final int oldCount = getAdapter().getCount();
					getAdapter().useRealCount();
					getAdapter().notifyDataSetChanged();
					// If a new item has been added, switch to it
					if (oldCount == 1 && getAdapter().getCount() >= 2) {
						post(new Runnable() {
							@Override
							public void run() {
								setCurrentItem(1);
							}
						});
					}
				}
			});
		}
	};

	public VersatileViewPager(Context context) {
		super(context);
		init();
	}

	public VersatileViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		if (Build.VERSION.SDK_INT >= 9) {
			// Disable over-scrolling
			setOverScrollMode(View.OVER_SCROLL_NEVER);
		}
		// Create an overlay layout
		mOverlayImage = new ImageView(getContext());
		mOverlayImage.setLayoutParams(new ViewGroup
				.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mPreviewOverlay = new RelativeLayout(getContext());

		// Set preview background
		try {
			TypedValue ta = new TypedValue();
			getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, ta, true);
			if (ta.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
					ta.type <= TypedValue.TYPE_LAST_COLOR_INT) {
				// Color
				int color = ta.data;
				mPreviewOverlay.setBackgroundColor(color);
			} else {
				// Not a color, probably a drawable
				Drawable d = getContext().getResources().getDrawable(ta.resourceId);
				mPreviewOverlay.setBackgroundDrawable(d);
			}
		} catch (RuntimeException e) {
			Log.e(TAG, "Failed to set the background!", e);
		}

		mPreviewOverlay.setLayoutParams(new ViewGroup
				.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mPreviewOverlay.addView(mOverlayImage);

		// Delay until parent is known
		getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (getParent() != null) {
					getViewTreeObserver().removeGlobalOnLayoutListener(this);
					mPagerParent = (ViewGroup) getParent();
				}
			}
		});
	}

	@Override
	public void setAdapter(final PagerAdapter adapter) {
		if (!(adapter instanceof VersatilePagerAdapter)) {
			throw new IllegalArgumentException("VersatileViewPager can only use a " +
					"VersatilePagerAdapter.");
		}
		// Remove observer from the previous adapter
		if (getAdapter() != null) {
			getAdapter().unregisterDataSetObserverInternal(mObserver);
		}
		super.setAdapter(adapter);
		// Add observer
		getAdapter().registerDataSetObserverInternal(mObserver);
	}

	@Override
	public VersatilePagerAdapter getAdapter() {
		return (VersatilePagerAdapter) super.getAdapter();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled() || getAdapter() == null) {
			return true;
		} else if (getCurrentItem() <= 1 && getAdapter().getCount() > 1) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mStartDragX = event.getX();
					break;
				case MotionEvent.ACTION_MOVE:
					// ViewPager un-scrollable when there's only a single item (empty item excluded)
					if (event.getX() > mStartDragX || getAdapter().getCount() <= 2) {
						return true;
					}
					break;
				case MotionEvent.ACTION_UP:

					/**
					 * If scrollX < current page's left position then the page will be switched
					 * which means the empty item becomes exposed. In such cases, manually scroll to
					 * the end of the page.
					 */
					Fragment fragment = (Fragment) getAdapter().getPrimaryItem();
					if (fragment != null && fragment.getView() != null) {
						int viewLeft = fragment.getView().getLeft();
						if (getScrollX() < viewLeft) {
							Log.w(TAG, String.format("Bad ScrollX: %d ChildLeft: %d",
									getScrollX(), viewLeft));
							scrollTo(viewLeft, getScrollY());
						}
					} else {
						// In case the fragment or its view are not ready when scrolling, consume
						return true;
					}
					break;
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return true;
		} else if (getCurrentItem() <= 1 && getAdapter() != null && getAdapter().getCount() > 1) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mStartDragX = event.getX();
					break;
				case MotionEvent.ACTION_MOVE:
					if (event.getX() > mStartDragX) {
						return false;
					}
					break;
			}
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	public void addOnPageChangeListener(OnPageChangeListener listener) {
		super.addOnPageChangeListener(listener);
		mListeners.add(listener);
	}

	@Override
	public void removeOnPageChangeListener(OnPageChangeListener listener) {
		super.removeOnPageChangeListener(listener);
		mListeners.remove(listener);
	}

	@Override
	public void clearOnPageChangeListeners() {
		super.clearOnPageChangeListeners();
		mListeners.clear();
	}

	private void setListenersEnabled(boolean enabled) {
		if (isListenersEnabled() != enabled) {
			if (enabled) {
				for (OnPageChangeListener listener : mListeners) {
					super.addOnPageChangeListener(listener);
				}
			} else {
				super.clearOnPageChangeListeners();
			}
			mOnPageChangeListenersEnabled = enabled;
		}
	}

	private boolean isListenersEnabled() {
		return mOnPageChangeListenersEnabled;
	}

}
