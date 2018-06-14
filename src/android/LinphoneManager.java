package cordova.scrachx.linphone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneNatPolicy;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.capture.hwconf.Hacks;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;

import cordova.scrachx.linphone.MainActivity;
import cordova.scrachx.linphone.R;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_VOICE_CALL;
import static android.media.AudioManager.MODE_RINGTONE;
import static android.media.AudioManager.STREAM_RING;


public class LinphoneManager implements LinphoneCoreListener {

	public static LinphoneManager mInstance;
	public static Context mContext;
	public static LinphoneCore mLinphoneCore;
	private static MediaPlayer mRingtone;
	private static boolean isRinging = false;

	public final static Integer NOTIFICATION_ID = 99;
	private static final int LINPHONE_VOLUME_STREAM = STREAM_VOICE_CALL;
	private static final int dbStep = 4;
	private final String mRingbackSoundFile;
    private final String mRingSoundFile;

	private AudioManager mAudioManager;
	private boolean mAudioFocused;
	private PowerManager mPowerManager;
	private PowerManager.WakeLock mIncallWakeLock;
	private Timer mTimer;
	private Resources mR;
	private String mBasePath;
	private Vibrator mVibrator;
	private MediaPlayer mRingerPlayer;

	private CallbackContext mCallbackRegister;
	private CallbackContext mCallbackAuthInfoReq;
	private CallbackContext mCallbackGlobal;
	private CallbackContext mCallbackCallState;
	private CallbackContext mCallbackCallStats;
	private CallbackContext mCallbackMessageReceived;

	public LinphoneManager(Context c) {
		mContext = c;
		mR = mContext.getResources();
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		mAudioManager = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mBasePath = mContext.getFilesDir().getAbsolutePath();
        mRingbackSoundFile = mBasePath + "/ringback.wav";
        mRingSoundFile = mBasePath + "/ringtone.mkv";

		mCallbackRegister = null;
		mCallbackAuthInfoReq = null;
		mCallbackGlobal = null;
		mCallbackCallState = null;
		mCallbackCallStats = null;
		mCallbackMessageReceived = null;
	}

	/**
	 * Create and start Linphone core
	 * @return mInstance LinphoneManager
	 */
	public synchronized final LinphoneManager createAndStart() {
		if (mInstance == null) {
			mInstance = new LinphoneManager(mContext);
			try {
			    copyAssetsFromPackage();
				mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, mContext);
				mLinphoneCore.setRingback(mRingbackSoundFile);
				//mLinphoneCore.setRing(mRingSoundFile);
			} catch (LinphoneCoreException e) {
				Log.e(e);
			} catch (Exception e) {
                Log.e(e);
                Log.e(e, "Cannot start linphone");
            }
			mInstance.startIterate();
		}
		return mInstance;
	}

	/**
	 * Get current context
	 * @return mContext Context
	 */
    public Context getContext() {
        try {
             return mContext;
        } catch (Exception e) {
            Log.e(e);
        }
        return null;
    }

	/**
	 * Get linphone core instance
	 * @return mLinphoneCore LinphoneCore
	 */
    public static synchronized final LinphoneCore getLc() {
		return getInstance().mLinphoneCore;
	}

	/**
	 * Get linphone manager instance
	 * @return mInstance LinphoneManager
	 */
	public static synchronized final LinphoneManager getInstance() {
		if (mInstance != null) return mInstance;
        throw new RuntimeException("Linphone Manager should be created before accessed");
	}

	/**
	 * Check if the current linphone manager instance is instanciated
	 * @return mInstance boolean
	 */
	public static final boolean isInstanciated() {
        return mInstance != null;
    }

	/**
	 * Destroy current linphone core & manager instance
	 */
	public void destroy() {
		try {
			mTimer.cancel();
			mLinphoneCore.destroy();
		} catch (RuntimeException e) {
			Log.e(e);
		} finally {
			mLinphoneCore = null;
			mInstance = null;
		}
	}

	/**
	 * Required to start the Linphone core
	 * DO NOT REMOVE
	 */
	private void startIterate() {
		try {
			TimerTask lTask = new TimerTask() {
				@Override
				public void run() {
					UIThreadDispatcher.dispatch(new Runnable() {
						@Override
						public void run() {
							if (mLinphoneCore != null) {
								mLinphoneCore.iterate();
							}
						}
					});
				}
			};

            /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
			mTimer = new Timer("Linphone scheduler");
			mTimer.schedule(lTask, 0, 20);
		} catch (Exception e) {
			Log.e(e);
			Log.e(e, "Cannot start linphone");
		}
	}


	public static synchronized LinphoneCore getLcIfManagerNotDestroyedOrNull() {
		if (isInstanciated()) {
			// Can occur if the UI thread play a posted event but in the meantime the LinphoneManager was destroyed
			// Ex: stop call and quickly terminate application.
			return null;
		}
		return getLc();
	}

    public boolean toggleEnableSpeaker() {
        if (mLinphoneCore.isIncall()) {
			boolean enabled = !mLinphoneCore.isSpeakerEnabled();
			mLinphoneCore.enableSpeaker(enabled);
            return enabled;
        }
        return false;
    }

    public boolean toggleMute() {
        if (mLinphoneCore.isIncall()) {
			boolean enabled = !mLinphoneCore.isMicMuted();
			mLinphoneCore.muteMic(enabled);
			return enabled;
        }
        return false;
    }

    private void copyAssetsFromPackage() throws IOException {
        copyIfNotExist(R.raw.notes_of_the_optimistic, mRingSoundFile);
        copyIfNotExist(R.raw.ringback, mRingbackSoundFile);
    }

    public void copyIfNotExist(int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(ressourceId,lFileToCopy.getName());
        }
    }

    public void copyFromPackage(int ressourceId, String target) throws IOException{
        FileOutputStream lOutputStream = mContext.openFileOutput (target, 0);
        InputStream lInputStream = mR.openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while (( readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff,0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

	public boolean isRinging() {
		return isRinging;
	}

	public void startRinging() {
		mAudioManager.setMode(MODE_RINGTONE);
		try {
			if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
				long[] patern = {0,1000,1000};
				mVibrator.vibrate(patern, 1);
			}
			if (mRingerPlayer == null) {
				requestAudioFocus(STREAM_RING);
				mRingerPlayer = new MediaPlayer();
				mRingerPlayer.setAudioStreamType(STREAM_RING);

				String ringtone = LinphoneUtils.getRingtone(android.provider.Settings.System.DEFAULT_RINGTONE_URI.toString(), mLinphoneCore);
				try {
					if (ringtone.startsWith("content://")) {
						mRingerPlayer.setDataSource(mContext, Uri.parse(ringtone));
					} else {
						FileInputStream fis = new FileInputStream(ringtone);
						mRingerPlayer.setDataSource(fis.getFD());
						fis.close();
					}
				} catch (IOException e) {
					Log.e(e, "Cannot set ringtone");
				}

				mRingerPlayer.prepare();
				mRingerPlayer.setLooping(true);
				mRingerPlayer.start();
			} else {
				Log.w("already ringing");
			}
		} catch (Exception e) {
			Log.e(e,"cannot handle incoming call");
		}
		isRinging = true;
	}

	public void stopRinging() {
        if (mRingerPlayer != null) {
            mRingerPlayer.stop();
            mRingerPlayer.release();
            mRingerPlayer = null;
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }

        mAudioManager.setMode(AudioManager.MODE_NORMAL);
		isRinging = false;
	}

	public void requestAudioFocus(int stream){
		if (!mAudioFocused){
			int res = mAudioManager.requestAudioFocus(null, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
			if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) mAudioFocused=true;
		}
	}

	public void adjustVolume(int i) {
		if (Build.VERSION.SDK_INT < 15) {
			int oldVolume = mAudioManager.getStreamVolume(LINPHONE_VOLUME_STREAM);
			int maxVolume = mAudioManager.getStreamMaxVolume(LINPHONE_VOLUME_STREAM);

			int nextVolume = oldVolume +i;
			if (nextVolume > maxVolume) nextVolume = maxVolume;
			if (nextVolume < 0) nextVolume = 0;

			mLinphoneCore.setPlaybackGain((nextVolume - maxVolume)* dbStep);
		} else
			// starting from ICS, volume must be adjusted by the application, at least for STREAM_VOICE_CALL volume stream
			mAudioManager.adjustStreamVolume(LINPHONE_VOLUME_STREAM, i < 0 ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
	}

	public void setAudioManagerInCallMode() {
		if (mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
			return;
		}
		mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
	}

    public void sendDtmf(char number) {
		mLinphoneCore.sendDtmf(number);
    }

	/**
	 * Set register callback
	 * @param pCallbackRegister CallbackContext
	 */
	public void setCallbackRegister(CallbackContext pCallbackRegister) {
		mCallbackRegister = pCallbackRegister;
	}

	/**
	 * Set register callback
	 * @param pCallbackAuthInfoReq CallbackContext
	 */
	public void setCallbackAuthInfoReq(CallbackContext pCallbackAuthInfoReq) {
		mCallbackAuthInfoReq = pCallbackAuthInfoReq;
	}

	/**
	 * Set global callback
	 * @param pCallbackGlobal CallbackContext
	 */
	public void setCallbackGlobal(CallbackContext pCallbackGlobal) {
		mCallbackGlobal = pCallbackGlobal;
	}

	/**
	 * Set call state callback
	 * @param pCallbackCallState CallbackContext
	 */
	public void setCallbackCallState(CallbackContext pCallbackCallState) {
		mCallbackCallState = pCallbackCallState;
	}

	/**
	 * Set call stats callback
	 * @param pCallbackCallStats CallbackContext
	 */
	public void setCallbackCallStats(CallbackContext pCallbackCallStats) {
		mCallbackCallStats = pCallbackCallStats;
	}

	/**
	 * Set message received callback
	 * @param pCallbackCallStats CallbackContext
	 */
	public void setCallbackMessageReceived(CallbackContext pCallbackCallStats) {
		mCallbackMessageReceived = pCallbackCallStats;
	}

	private LinphoneNatPolicy getOrCreateNatPolicy() {
		LinphoneNatPolicy nat = getLc().getNatPolicy();
		if (nat == null) {
			nat = getLc().createNatPolicy();
		}
		return nat;
	}

	public void setStunServer(String stun) {
		LinphoneNatPolicy nat = getOrCreateNatPolicy();
		nat.setStunServer(stun);

		if (stun != null && !stun.isEmpty()) {
			nat.enableStun(true);
		}
		getLc().setNatPolicy(nat);
	}

	/**
	 * Generate notification
	 * @param title String
	 * @param message String
	 */
	private void generateNotificationRegisterState(LinphoneCore.RegistrationState registrationState) {
		String message = "";
		if(registrationState == registrationState.RegistrationFailed) {
			message = "Échoué";
		} else if(registrationState == registrationState.RegistrationOk) {
			message = "Connecté";
		} else if(registrationState == registrationState.RegistrationProgress) {
			message = "En cours de connexion";
		} else if(registrationState == registrationState.RegistrationCleared) {
          message = "Déconnecté";
        }
		Intent notificationIntent = new Intent(mContext, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
		Notification myNotification = new NotificationCompat.Builder(mContext)
		    .setContentTitle(mContext.getResources().getString(R.string.app_name_notification))
		    .setSmallIcon(android.R.drawable.ic_menu_call)
		    .setContentText(mContext.getResources().getString(R.string.register_state_notification) + " " + message)
		    .setPriority(NotificationCompat.PRIORITY_MAX)
		    .setVibrate(new long[] {1, 1, 1})
		    .setOngoing(true)
		    .setContentIntent(contentIntent)
		    .setCategory(NotificationCompat.CATEGORY_EVENT)
		    .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
		    .build();
		myNotification.flags |= Notification.FLAG_NO_CLEAR;
		getNotificationManager().notify(this.NOTIFICATION_ID, myNotification);
	}

	    private NotificationManager getNotificationManager(){
		return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	    }

	/*************************************************************************/
	/*************************** CORE LISTENER *******************************/
	/*************************************************************************/

	@Override
	public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {

	}

	@Override
    public void authenticationRequested(LinphoneCore linphoneCore, LinphoneAuthInfo linphoneAuthInfo, LinphoneCore.AuthMethod authMethod) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, "Auth Info Requested : " + linphoneAuthInfo.getRealm() + " " + linphoneAuthInfo.getUsername() + " " + linphoneAuthInfo.getDomain());
		result.setKeepCallback(true);
		mCallbackAuthInfoReq.sendPluginResult(result);
    }

	@Override
	public void messageReceivedUnableToDecrypted(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {

	}

	@Override
	public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {

	}

	@Override
	public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {

	}

	@Override
	public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {

	}

	@Override
	public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {

	}

	@Override
	public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {

	}

	@Override
	public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {

	}

	@Override
	public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {

	}

	@Override
	public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {

	}

	@Override
	public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {

	}

	@Override
	public void show(LinphoneCore linphoneCore) {

	}

	@Override
	public void displayStatus(LinphoneCore linphoneCore, String s) {

	}

	@Override
	public void displayMessage(LinphoneCore linphoneCore, String s) {

	}

	@Override
	public void displayWarning(LinphoneCore linphoneCore, String s) {

	}

	@Override
	public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {

	}

	@Override
	public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {

	}

	@Override
	public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
		return 0;
	}

	@Override
	public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
		if(mCallbackGlobal != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, globalState.toString());
			result.setKeepCallback(true);
			mCallbackGlobal.sendPluginResult(result);
		}
	}

	@Override
	public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
		PluginResult result = new PluginResult(PluginResult.Status.OK, registrationState.toString());
		generateNotificationRegisterState(registrationState);
		result.setKeepCallback(true);
		mCallbackRegister.sendPluginResult(result);
	}

	@Override
	public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {

	}

	@Override
	public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {
		String address = "";
		if(linphoneChatRoom.getPeerAddress().asString().contains("<") && linphoneChatRoom.getPeerAddress().asString().contains(">")) {
			address = linphoneChatRoom.getPeerAddress().asString();
		} else {
			address = "<" + linphoneChatRoom.getPeerAddress().asString() + ">";
		}
		PluginResult result = new PluginResult(PluginResult.Status.OK, address + " : '" + linphoneChatMessage.getText() + "'" );
		result.setKeepCallback(true);
		mCallbackRegister.sendPluginResult(result);
		mCallbackMessageReceived.sendPluginResult(result);
	}

	@Override
	public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {

		PluginResult result = new PluginResult(PluginResult.Status.OK, state.toString() + " " + LinphoneUtils.getAddressDisplayName(linphoneCall.getRemoteAddress()) + " " + linphoneCall.getRemoteAddress().asString());
		result.setKeepCallback(true);
		mCallbackCallState.sendPluginResult(result);

		if (state == LinphoneCall.State.IncomingReceived && !linphoneCall.equals(linphoneCore.getCurrentCall())) {
			if (linphoneCall.getReplacedCall() != null) {
				return;
			}
			// TODO : handle incoming call during calling
		}

		if (state == LinphoneCall.State.IncomingReceived) {
            if (mLinphoneCore.getCallsNb() == 1) {
                requestAudioFocus(STREAM_RING);
                startRinging();
            }
			//generateNotification("Call", "Incoming call by " + LinphoneUtils.getAddressDisplayName(linphoneCall.getRemoteAddress()) + " " + linphoneCall.getRemoteAddress().asString());
		}

		if (state == LinphoneCall.State.Connected) {
			if(isRinging()) stopRinging();
			if (mLinphoneCore.getCallsNb() == 1) {
				mAudioManager.abandonAudioFocus(null);
				requestAudioFocus(STREAM_VOICE_CALL);
			}
			if (Hacks.needSoftvolume()) {
				Log.w("Using soft volume audio hack");
				adjustVolume(0);
			}
		}

		if (state == LinphoneCall.State.OutgoingEarlyMedia) {
			setAudioManagerInCallMode();
		}

		if (state == LinphoneCall.State.CallEnd || state == LinphoneCall.State.Error) {
			if (mLinphoneCore.getCallsNb() == 0) {
				if (mAudioFocused){
					int res = mAudioManager.abandonAudioFocus(null);
					Log.d("Audio focus released a bit later: " + (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted" : "Denied"));
					mAudioFocused = false;
				}
				if (mContext != null) {
					TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
					if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
						Log.d("---AudioManager: back to MODE_NORMAL");
						mAudioManager.setMode(AudioManager.MODE_NORMAL);
					}
				}
				if (mIncallWakeLock != null && mIncallWakeLock.isHeld()) {
					mIncallWakeLock.release();
					Log.i("Last call ended: releasing incall (CPU only) wake lock");
				} else {
					Log.i("Last call ended: no incall (CPU only) wake lock were held");
				}
			}
		}

		if (state == LinphoneCall.State.OutgoingInit) {
			setAudioManagerInCallMode();
			requestAudioFocus(STREAM_VOICE_CALL);
		}

		if (state == LinphoneCall.State.CallReleased) {
			if(isRinging()) stopRinging();
		}


		if (state == LinphoneCall.State.StreamsRunning) {
			if (mIncallWakeLock == null) {
				mIncallWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,	"incall");
			}
			if (!mIncallWakeLock.isHeld()) {
				Log.i("New call active : acquiring incall (CPU only) wake lock");
				mIncallWakeLock.acquire();
			} else {
				Log.i("New call active while incall (CPU only) wake lock already active");
			}
		}
	}

	@Override
	public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {

	}

	@Override
	public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {

	}

	@Override
	public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {

	}

	@Override
	public void ecCalibrationStatus(LinphoneCore linphoneCore, LinphoneCore.EcCalibratorStatus ecCalibratorStatus, int i, Object o) {

	}

	@Override
	public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {

	}

	@Override
	public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {

	}

	@Override
	public void friendListCreated(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

	}

	@Override
	public void friendListRemoved(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

	}

	@Override
	public void networkReachableChanged(LinphoneCore linphoneCore, boolean reachable) {

	}

}
