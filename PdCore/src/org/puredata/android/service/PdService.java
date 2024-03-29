/**
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */

package org.puredata.android.service;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.Properties;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

/**
 * 
 * PdService allows applications to run Pure Data as a (local) service, with foreground priority if desired.
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 *
 */
public class PdService extends Service {

	public class PdBinder extends Binder {
		public PdService getService() {
			return PdService.this;
		}
	}
	private final PdBinder binder = new PdBinder();
	private static final boolean hasEclair = Properties.version >= 5;
	private final ForegroundManager fgManager = hasEclair ? new ForegroundEclair() : new ForegroundCupcake();

	private static final String PD_SERVICE = "PD Service";
	private volatile int sampleRate = 0;
	private volatile int inputChannels = 0;
	private volatile int outputChannels = 0;
	private volatile float bufferSizeMillis = 0.0f;

	/**
	 * @return the current audio buffer size in milliseconds (approximate value;
	 * the exact value is a multiple of the Pure Data tick size (64 samples))
	 */
	public float getBufferSizeMillis() {
		return bufferSizeMillis;
	}

	/**
	 * @return number of input channels
	 */
	public int getInputChannels() {
		return inputChannels;
	}

	/**
	 * @return number of output channels
	 */
	public int getOutputChannels() {
		return outputChannels;
	}

	/**
	 * @return current sample rate
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * Initialize Pure Data and audio thread
	 * 
	 * @param srate   sample rate
	 * @param nic     number of input channels
	 * @param noc     number of output channels
	 * @param millis  audio buffer size in milliseconds
	 * @throws IOException  if the audio parameters are not supported by the device
	 */
	public synchronized void initAudio(int srate, int nic, int noc, float millis) throws IOException {
		fgManager.stopForeground();
		Resources res = getResources();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (srate < 0) {
			String s = prefs.getString(res.getString(R.string.pref_key_srate), null);
			srate = (s == null) ? AudioParameters.suggestSampleRate() : Integer.parseInt(s);
		}
		if (nic < 0) {
			String s = prefs.getString(res.getString(R.string.pref_key_inchannels), null);
			nic = (s == null) ? AudioParameters.suggestInputChannels() : Integer.parseInt(s);
		}
		if (noc < 0) {
			String s = prefs.getString(res.getString(R.string.pref_key_outchannels), null);
			noc = (s == null) ? AudioParameters.suggestOutputChannels() : Integer.parseInt(s);
		}
		if (millis < 0) {
			String s = prefs.getString(res.getString(R.string.pref_key_bufsize_millis), null);
			millis = (s == null) ? AudioParameters.suggestBufferSizeMillis() : Float.parseFloat(s);
		}
		int tpb = (int) (0.001f * millis * srate / PdBase.blockSize()) + 1;
		PdAudio.initAudio(srate, nic, noc, tpb, true);
		sampleRate = srate;
		inputChannels = nic;
		outputChannels = noc;
		bufferSizeMillis = millis;
	}

	/**
	 * Start the audio thread without foreground privileges
	 */
	public synchronized void startAudio() {
		PdAudio.startAudio(this);
	}

	/**
	 * Start the audio thread with foreground privileges
	 * 
	 * @param intent       intent to be triggered when the user selects the notification of the service
	 * @param icon         icon representing the notification
	 * @param title        title of the notification
	 * @param description  description of the notification
	 */
	public synchronized void startAudio(Intent intent, int icon, String title, String description) {
		fgManager.startForeground(intent, icon, title, description);
		PdAudio.startAudio(this);
	}

	/**
	 * Stop the audio thread
	 */
	public synchronized void stopAudio() {
		PdAudio.stopAudio();
		fgManager.stopForeground();
	}

	/**
	 * @return true if and only if the audio thread is running
	 */
	public synchronized boolean isRunning() {
		return PdAudio.isRunning();
	}

	/**
	 * Releases all resources
	 */
	public synchronized void release() {
		stopAudio();
		PdAudio.release();
		PdBase.release();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		release();
		return false;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Resources res = getResources();
		File dir = getFilesDir();
		try {
			IoUtils.extractZipResource(res.openRawResource(R.raw.extra_abs), dir, false);
			IoUtils.extractZipResource(res.openRawResource(Properties.hasArmeabiV7a ? R.raw.extra_ext_v7a : R.raw.extra_ext), dir, false);
		} catch (IOException e) {
			Log.e(PD_SERVICE, "unable to unpack abstractions/extras: " + e.toString());
		}
		PdBase.addToSearchPath(dir.getAbsolutePath());
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		release();
	}

	// Hack to support multiple versions of the Android API, based on an idea
	// from http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
	private interface ForegroundManager {
		void startForeground(Intent intent, int icon, String title, String description);
		void stopForeground();
	}

	// Another version support hack, this time adapted from
	// http://tuntis.net/2011/03/06/setforeground-missing-in-android-3-0-services/.
	// This one works around the disappearance of the setForeground method in Honeycomb.
	private void invokeSetForeground(boolean foreground) {
	    try {
	        Method method = getClass().getMethod("setForeground", boolean.class);
	        method.invoke(this, foreground);
	    } catch (Exception e) {
            Log.e(PD_SERVICE, e.toString());
	    }
	}

	private class ForegroundCupcake implements ForegroundManager {
		protected static final int NOTIFICATION_ID = 1;
		private boolean hasForeground = false;

		protected Notification makeNotification(Intent intent, int icon, String title, String description) {
			PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
			Notification notification = new Notification(icon, title, System.currentTimeMillis());
			notification.setLatestEventInfo(PdService.this, title, description, pi);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			return notification;
		}

		@Override
		public void startForeground(Intent intent, int icon, String title, String description) {
			stopForeground();
			versionedStart(intent, icon, title, description);
			hasForeground = true;
		}
		
		protected void versionedStart(Intent intent, int icon, String title, String description) {
			invokeSetForeground(true);
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(NOTIFICATION_ID, makeNotification(intent, icon, title, description));
		}

		@Override
		@Override
		@Override
		public void stopForeground() {
			if (hasForeground) {
				versionedStop();
				hasForeground = false;
			}
		}
		
		protected void versionedStop() {
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(NOTIFICATION_ID);
			invokeSetForeground(false);
		}
	}

	private class ForegroundEclair extends ForegroundCupcake {
		@Override
		protected void versionedStart(Intent intent, int icon, String title, String description) {
			PdService.this.startForeground(NOTIFICATION_ID, makeNotification(intent, icon, title, description));
		}

		@Override
		@Override
		@Override
		protected void versionedStop() {
			PdService.this.stopForeground(true);
		}
	}
}
