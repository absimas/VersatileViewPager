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
import android.support.v4.view.*;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * <h5>Notes:</h5>
 * <ul>
 *     <li>The item at position 0 is always the empty item</li>
 *     <li>Left overscroll is not available since the empty item is considered a valid item.</li>
 *     <li>The header can still be selected with {@code setCurrentItem(0)}</li>
 * </ul>
 */
public class VersatileViewPager extends ViewPager {

	private final String TAG = getClass().getName();
	private ImageView mOverlayImage;
	private ViewGroup mPagerParent, mPreviewOverlay;
	private int mRemovedPosition;
	private float mStartDragX;
	private ViewPager.SimpleOnPageChangeListener mTemporarySwitchListener = new ViewPager
			.SimpleOnPageChangeListener() {
		@Override
		public void onPageScrollStateChanged(int state) {
			super.onPageScrollStateChanged(state);
			if (state == ViewPager.SCROLL_STATE_IDLE) {
				removeOnPageChangeListener(this);

				// Overlay and image while working (prevent flickering)
				mOverlayImage.setImageBitmap(Utils.screenshot(VersatileViewPager.this));
				mPagerParent.addView(mPreviewOverlay);

				// Change the count and notify (for real now)
				getAdapter().useRealCount();
				getAdapter().notifyDataSetChanged();

				// Switch to the unused page, it will be populated after notifyDataSetChanged
				setCurrentItem(mRemovedPosition, false);

				// When switches have settled, remove the preview and re-enable scrolling
				post(new Runnable() {
					@Override
					public void run() {
						mPagerParent.removeView(mPreviewOverlay);
						setEnabled(true);
					}
				});
			}
		}
	};
	DataSetObserver mObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			Object primaryItem = getAdapter().getPrimaryItem();
			if (primaryItem != null) {
				int primaryPos = getAdapter().getItemPosition(primaryItem);
				if (primaryPos == PagerAdapter.POSITION_NONE) {
					// Disable scrolling
					setEnabled(false);

					mRemovedPosition = getCurrentItem();
					addOnPageChangeListener(mTemporarySwitchListener);
					// Animate to the previous item if last one removed, forward otherwise
					if (getCurrentItem() == getAdapter().getCount() - 1) {
						setCurrentItem(getCurrentItem() - 1);
					} else {
						setCurrentItem(getCurrentItem() + 1);
					}
					return;
				}
			}
			final int oldCount = getAdapter().getCount();
			getAdapter().useRealCount();
			getAdapter().notifyDataSetChanged();
			// If a new item has been added, switch to it
			post(new Runnable() {
				@Override
				public void run() {
					if (oldCount == 1 && getAdapter().getCount() >= 2) {
						setCurrentItem(1);
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
		// Disable overscrolling
		setOverScrollMode(View.OVER_SCROLL_NEVER);
		// Create an overlay layout
		mOverlayImage = new ImageView(getContext());
		mOverlayImage.setLayoutParams(new ViewGroup
				.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mPreviewOverlay = new RelativeLayout(getContext());
		mPreviewOverlay.setBackgroundColor(Color.parseColor("#EEEEEE"));
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
			throw new IllegalArgumentException("ItemViewPager can only use an VersatilePagerAdapter.");
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
		if (!isEnabled()) {
			return true;
		} else if (getCurrentItem() <= 1) {
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
					 * If (scrollX < getClientWidth()) then the page will be switched, i.e.
					 * empty item becomes exposed. Then, manually scroll to the end of the page.
					 */
					if (getScrollX() < 0) {
						if (Math.abs(getScrollX()) > getClientWidth()) {
							Log.w(TAG, "Bad ScrollX: " + getScrollX());
							scrollTo(getClientWidth() * -1, getScrollY());
						}
					} else {
						if (getScrollX() < getClientWidth()) {
							Log.w(TAG, "Bad ScrollX: " + getScrollX());
							scrollTo(getClientWidth(), getScrollY());
						}
					}
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return true;
		} else if (getCurrentItem() <= 1) {
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

	private int getClientWidth() {
		return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
	}

}
