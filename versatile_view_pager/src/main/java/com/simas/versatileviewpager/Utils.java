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
import android.os.Handler;
import android.view.View;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Helper methods and classes
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

	/**
	 * This handler enables messages to block queued messages until some specific task is done.
	 * The blocking is done by calling {@code setPaused(true)} and then when the message has done
	 * it's job it must call {@code setPaused(false)}. If these two method calls are unnecessary
	 * for a task, then it shouldn't use this handler. It's more suited for a sequence of
	 * runnables which together execute a specific task.
	 */
	public static class PausableHandler {

		private final Queue<Runnable> mMessageQueue = new ConcurrentLinkedQueue<>();
		private final Handler mHandler;
		private final Object mPausableHandlerLock = new Object();
		private final Runnable mRunNext = new Runnable() {
			@Override
			public void run() {
				if (!isPaused()) {
					Runnable next = mMessageQueue.poll();
					if (next != null) {
						mHandler.post(next);
					}
				}
			}
		};
		private boolean mPaused;

		public PausableHandler() {
			mHandler = new Handler();
		}

		public PausableHandler(Handler handler) {
			mHandler = handler;
		}

		public void post(Runnable runnable) {
			synchronized (mPausableHandlerLock) {
				mMessageQueue.add(runnable);
				mMessageQueue.add(mRunNext);
				// Execute mRunNext. If the handler isn't paused it will run the next message,
				// otherwise will wait for setPaused(false) call.
				mHandler.post(mRunNext);
			}
		}

		public void setPaused(boolean paused) {
			synchronized (mPausableHandlerLock) {
				if (paused == isPaused()) return;
				mPaused = paused;
				if (!isPaused()) {
					mHandler.post(mRunNext);
				}
			}
		}

		public boolean isPaused() {
			synchronized (mPausableHandlerLock) {
				return mPaused;
			}
		}

	}

}
