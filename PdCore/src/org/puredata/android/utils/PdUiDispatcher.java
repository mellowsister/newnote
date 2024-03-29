/**
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */

package org.puredata.android.utils;

import org.puredata.core.utils.PdDispatcher;

/**
 * Subclass of {@link PdDispatcher} for executing callbacks on the main UI thread
 * of an Android app.  It is actually more general than that; instances of this
 * class will execute their callbacks in whichever thread they were created in,
 * but in practice it really only makes sense to create instances of this class
 * in the main UI thread.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 */
public class PdUiDispatcher extends PdDispatcher {
	
	private final static String TAG = PdUiDispatcher.class.getSimpleName();
	private final Handler handler;
	
	/**
	 * Constructor; invoke from the main UI thread
	 */
	public PdUiDispatcher() {
		handler = new Handler();
	}

	@Override
	@Override
	@Override
	public void print(String s) {
		Log.i(TAG, "print: " + s);
	}
	
	@Override
	@Override
	@Override
	public synchronized void receiveBang(final String source) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdUiDispatcher.super.receiveBang(source);
			}
		});
	}

	@Override
	@Override
	@Override
	public synchronized void receiveFloat(final String source, final float x) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdUiDispatcher.super.receiveFloat(source, x);
			}
		});
	}

	@Override
	@Override
	@Override
	public synchronized void receiveSymbol(final String source, final String symbol) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdUiDispatcher.super.receiveSymbol(source, symbol);
			}
		});
	}
	
	@Override
	@Override
	@Override
	public synchronized void receiveList(final String source, final Object... args) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdUiDispatcher.super.receiveList(source, args);
			}
		});
	}
	
	@Override
	@Override
	@Override
	public synchronized void receiveMessage(final String source, final String symbol, final Object... args) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdUiDispatcher.super.receiveMessage(source, symbol, args);
			}
		});
	}
}
