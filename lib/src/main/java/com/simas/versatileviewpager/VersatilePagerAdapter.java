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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

/**
 * Custom pager adapter that is based on {@link android.support.v4.app.FragmentStatePagerAdapter}.
 * In addition this gives more freedom in fetching and deleting fragments and their states.
 * Remember that the item at position 0 is always the empty item.
 */
public abstract class VersatilePagerAdapter extends PagerAdapter {

	private final String TAG = getClass().getName();
	private static final String ITEMS = "states";

	private final FragmentManager mFragmentManager;
	private FragmentTransaction mCurTransaction = null;
	private Object mPrimaryItem;
	private DataSetObservable mInternalObserver = new DataSetObservable();
	private int mCount, mRealCount;
	private ArrayList<Item> mItems = new ArrayList<Item>() {
		@Override
		public Item get(int index) {
			// Return null for negative indexes
			if (index < 0) return null;
			// Lazy instantiate items if they're not present in the list
			while (index >= size()) {
				add(new Item());
			}
			return super.get(index);
		}
	};

	public VersatilePagerAdapter(FragmentManager fm) {
		mFragmentManager = fm;
	}

	/**
	 * Create a fragment associated with a specific value. 0th position is the empty view.
	 */
	public abstract Fragment createItem(int position);

	/**
	 * Get the item for the specific position. Return null if not yet created or has been cached.
	 */
	public Fragment getItem(int position) {
		return mItems.get(position).fragment;
	}

	/**
	 * Remove the fragment that is connected to this position, also clear the fragment's state.<br/>
	 * This method must be called <b>before</b> notifying the internal observers with {@link
	 * #requestCount(int)}.
	 */
	public void onItemRemoved(int position) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		if (getItem(position) != null) {
			mCurTransaction.remove(getItem(position));
		}

		mItems.remove(position);
	}

	/**
	 * Set the new item count internally and notify the internal observers. The actual {@link
	 * #notifyDataSetChanged()} will not be called because that is expected from the internal
	 * observers. If items are removed, {@link #onItemRemoved(int)} must also be called to get
	 * rid of un-used fragments and states.
	 */
	public final void requestCount(int count) {
		if (count < 0) throw new IllegalArgumentException("Count cannot be less than 0!");
		mRealCount = count;
		notifyDataSetChangedInternal();
	}

	@Override
	public int getItemPosition(@Nullable Object object) {
		if (object != null) {
			// ToDo position unchanged
			for (int i = 0; i < mItems.size(); ++i) {
				Item item = mItems.get(i);
				if (item.fragment == object) {
					return i;
				}
			}
		}
		return POSITION_NONE;
	}

	@Override
	public void startUpdate(ViewGroup container) {}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		if (position > getRealCount()) return null;

		Item item = mItems.get(position);
		if (item.fragment != null) {
			return item.fragment;
		} else {
			item.fragment = createItem(position);
		}

		if (item.state != null) {
			item.fragment.setInitialSavedState(item.state);
		}

		item.fragment.setMenuVisibility(false);
		item.fragment.setUserVisibleHint(false);

		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		mCurTransaction.add(container.getId(), item.fragment);

		return item.fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}

		Item item = mItems.get(position);
		if (item.fragment != null) {
			item.state = mFragmentManager.saveFragmentInstanceState(item.fragment);
			mCurTransaction.remove(item.fragment);
			item.fragment = null;
		}
	}

	@Override
	public void finishUpdate(ViewGroup container) {
		if (mCurTransaction != null) {
			mCurTransaction.commitAllowingStateLoss();
			mCurTransaction = null;
			mFragmentManager.executePendingTransactions();
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		if (object == null || view == null || ((Fragment)object).getView() != view) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		super.setPrimaryItem(container, position, object);
		mPrimaryItem = object;
	}

	Object getPrimaryItem() {
		return mPrimaryItem;
	}

	@Override
	public final Parcelable saveState() {
		Bundle state = null;
		// Save items
		if (mItems.size() > 0) {
			state = new Bundle();
			state.putParcelableArrayList(ITEMS, mItems);

			// Save fragment references
			for (int i=0; i<mItems.size(); i++) {
				Fragment f = mItems.get(i).fragment;
				if (f != null && f.isAdded()) {
					String key = "f" + i;
					mFragmentManager.putFragment(state, key, f);
				}
			}
		}
		return state;
	}

	@Override
	public final void restoreState(Parcelable state, ClassLoader loader) {
		if (state != null) {
			Bundle bundle = (Bundle)state;
			bundle.setClassLoader(loader);
			mItems.clear();
			if (bundle.getParcelableArrayList(ITEMS) != null) {
				mItems = bundle.getParcelableArrayList(ITEMS);
			}
			Iterable<String> keys = bundle.keySet();
			for (String key: keys) {
				if (key.startsWith("f")) {
					int index = Integer.parseInt(key.substring(1));
					Fragment f = mFragmentManager.getFragment(bundle, key);
					if (f != null) {
						f.setMenuVisibility(false);
						mItems.get(index).fragment = f;
					} else {
						Log.w(TAG, "Bad fragment at key " + key);
					}
				}
			}
		}
	}

	private static class Item implements Parcelable {
		private Fragment fragment;
		private Fragment.SavedState state;

		public Item() {}

		protected Item(Parcel in) {
			state = in.readParcelable(Fragment.SavedState.class.getClassLoader());
		}

		public static final Creator<Item> CREATOR = new Creator<Item>() {
			@Override
			public Item createFromParcel(Parcel in) {
				return new Item(in);
			}

			@Override
			public Item[] newArray(int size) {
				return new Item[size];
			}
		};

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeParcelable(state, flags);
		}
	}

	@Override
	public final int getCount() {
		return mCount + 1;
	}

	int getRealCount() {
		return mRealCount;
	}

	void useRealCount() {
		mCount = mRealCount;
	}

	void notifyDataSetChangedInternal() {
		mInternalObserver.notifyChanged();
	}

	void registerDataSetObserverInternal(DataSetObserver observer) {
		mInternalObserver.registerObserver(observer);
	}

	void unregisterDataSetObserverInternal(DataSetObserver observer) {
		mInternalObserver.unregisterObserver(observer);
	}

}
