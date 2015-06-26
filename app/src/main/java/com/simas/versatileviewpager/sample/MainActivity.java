package com.simas.versatileviewpager.sample;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
