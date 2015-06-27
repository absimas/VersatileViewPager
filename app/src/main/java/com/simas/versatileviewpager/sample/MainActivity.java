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

import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.simas.versatileviewpager.VersatilePagerAdapter;
import com.simas.versatileviewpager.VersatileViewPager;

public class MainActivity extends AppCompatActivity {

	VersatileViewPager pager;
	VersatilePagerAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		pager = (VersatileViewPager) findViewById(R.id.pager);
		adapter = new VersatilePagerAdapter(getSupportFragmentManager()) {
			@Override
			public Fragment createItem(int position) {
				Bundle args = new Bundle();
				args.putInt(NumberedFragment.ARG_INITIAL_POS, position);

				Fragment fragment = new NumberedFragment();
				fragment.setArguments(args);
				return fragment;
			}
		};
		pager.setAdapter(adapter);
//		adapter.setCount(1);
//		new Handler().postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				adapter.onItemRemoved(1);
//				adapter.setCount(0);
//			}
//		}, 2000);
	}

	public static class NumberedFragment extends Fragment {

		public static final String ARG_INITIAL_POS = "position";
		int position;
		int num;

		public NumberedFragment() {}

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Bundle args = getArguments();
			if (savedInstanceState != null && savedInstanceState.containsKey(ARG_INITIAL_POS)) {
				position = savedInstanceState.getInt(ARG_INITIAL_POS);
			} else if (args != null) {
				position = args.getInt(ARG_INITIAL_POS);
			}
			if (args != null) {
				num = args.getInt(ARG_INITIAL_POS);
			}
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putInt(ARG_INITIAL_POS, position);
		}

		@Nullable
		@Override
		public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedState) {
			View root = i.inflate(com.simas.versatileviewpager.R.layout.fragment_empty, c, false);
			TextView tv = (TextView) root.findViewById(R.id.text);
			tv.setText(String.format(getString(R.string.numbered_fragment_format), position));
			return root;
		}
	}

}
