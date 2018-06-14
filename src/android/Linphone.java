package cordova.scrachx.linphone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.LayoutInflater;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;

public class Linphone extends CordovaPlugin {

    private static LinphoneProxyConfig mProxyCfg;
    private static String mDefaultDomain;
    private static String mDefaultStun;
    private static Context mContext;
    private static LinphoneManager mLinphoneManager;
    private static LinphoneChatRoom mChatRoom;
    private static SharedPreferences mSettings;
    private static SharedPreferences.Editor mSettingsEditor;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static Resources mResources;
    private static String mPackageName;
    private static LayoutInflater mLayoutInflator;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        mDefaultDomain = "sip.linphone.org";
        mDefaultStun = "stun.linphone.org";
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mContext = cordova.getActivity().getApplicationContext();
        mSettings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mSettingsEditor = mSettings.edit();
        mLinphoneManager = new LinphoneManager(mContext);
        mLinphoneManager.createAndStart();
        if (mResources == null)
            mResources = cordova.getActivity().getApplication().getResources();
        if (mPackageName == null)
            mPackageName = cordova.getActivity().getApplication().getPackageName();
        if (mLayoutInflator == null) {
            mLayoutInflator = cordova.getActivity().getLayoutInflater();
        }
    }

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if(action.equals("saveAuthInfo")) {
            saveAuthInfo(args.getString(0), args.getString(1), args.getString(2), args.getString(3), callbackContext);
            return true;
        } else if(action.equals("saveProxyInfo")) {
            saveProxyInfo(args.getString(0), args.getString(1), args.getInt(2), args.getBoolean(3), args.getBoolean(4), args.getBoolean(5), callbackContext);
            return true;
        } else if(action.equals("saveSipInfo")) {
            saveSipInfo(args.getInt(0), args.getBoolean(1), args.getInt(2), args.getInt(3), callbackContext);
            return true;
        } else if (action.equals("login")) {
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                try {
                    login(callbackContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            });
            return true;
        } else if (action.equals("logout")) {
            logout(callbackContext);
            return true;
        } else if (action.equals("call")) {
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                    try {
                        call(args.getString(0), args.getString(1), callbackContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (action.equals("sendMessage")) {
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                    try {
                        sendMessage(args.getString(0), args.getString(1), args.getBoolean(2), callbackContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        } else if (action.equals("answerCall")) {
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                try {
                    answerCall(callbackContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            });
            return true;
        } else if (action.equals("videoCall")) {
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                try {
                    videoCall(args.getString(0), args.getString(1), callbackContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            });
            return true;
        } else if(action.equals("hangup")){
            cordova.getThreadPool().execute(new Runnable() {public void run() {
                try {
                    hangup(callbackContext);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            });
            return true;
        } else if(action.equals("toggleVideo")){
            toggleVideo(callbackContext);
            return true;
        } else if(action.equals("toggleSpeaker")){
            toggleSpeaker(callbackContext);
            return true;
        } else if(action.equals("toggleMute")){
            toggleMute(callbackContext);
            return true;
        } else if(action.equals("sendDtmf")){
            sendDtmf(args.getString(0), callbackContext);
            return true;
        } else if(action.equals("registerCallback")) {
            mLinphoneManager.setCallbackRegister(callbackContext);
            return true;
        } else if(action.equals("authInfoReqCallback")) {
            mLinphoneManager.setCallbackAuthInfoReq(callbackContext);
            return true;
        } else if(action.equals("globalCallback")) {
            mLinphoneManager.setCallbackGlobal(callbackContext);
            return true;
        } else if(action.equals("callStateCallback")) {
            mLinphoneManager.setCallbackCallState(callbackContext);
            return true;
        } else if(action.equals("callStatsCallback")) {
            mLinphoneManager.setCallbackCallStats(callbackContext);
            return true;
        } else if(action.equals("messageReceivedCallback")) {
            mLinphoneManager.setCallbackMessageReceived(callbackContext);
            return true;
        }
        return false;
    }

    /**https://developer.android.com/guide/topics/data/data-storage.html
     * Save and update sip info in Shared Preferences
     * @param pPort int
     * @param pIpv6 Boolean
     * @param pIncTimeOut int
     * @param pKeepAlive int
     * @param callbackContext CallbackContext
     */
    public static synchronized void saveSipInfo(final int pPort, final Boolean pIpv6, final int pIncTimeOut, final int pKeepAlive, final CallbackContext callbackContext) {
        try {
            if(pPort != 0 && pIpv6 != null && pIncTimeOut != 0 && pKeepAlive != 0) {
                mSettingsEditor.putInt("sipPort", pPort);
                mSettingsEditor.putBoolean("sipIPV6", pIpv6);
                mSettingsEditor.putInt("sipIncTimeOut", pIncTimeOut);
                mSettingsEditor.putInt("sipKeepAlive", pKeepAlive);
                mSettingsEditor.apply();
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Sip parameters saved/updated"));
            } else {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error",  "Sip parameters are empty"));
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * Save and update auth info in Shared Preferences
     * @param pUsername String
     * @param pUserId String
     * @param pRealm String
     * @param pPaswd String
     * @param callbackContext CallbackContext
     */
    public static synchronized void saveAuthInfo(final String pUsername, final String pUserId, final String pRealm, final String pPaswd, final CallbackContext callbackContext) {
        try {
            if(!pUsername.isEmpty() && !pUserId.isEmpty() && !pPaswd.isEmpty()) {
                mSettingsEditor.putString("authUsername", pUsername);
                mSettingsEditor.putString("authUserId", pUserId);
                mSettingsEditor.putString("authPaswd", pPaswd);
                if(pRealm.isEmpty()) {
                    mSettingsEditor.putString("authRealm", mDefaultDomain);
                } else {
                    mSettingsEditor.putString("authRealm", pRealm);
                }
                mSettingsEditor.apply();
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Authentication parameters saved/updated"));
            } else {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Authentication parameters are empty"));
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * Save and update proxy info in Shared Preferences
     * @param pRegProxy String
     * @param pRegIdentity String
     * @param pRegExpires int
     * @param pRegSendRegister Boolean
     * @param pPublish Boolean
     * @param pDialEscapePlus Boolean
     * @param callbackContext CallbackContext
     */
    public static synchronized void saveProxyInfo(final String pRegProxy, final String pRegIdentity, final int pRegExpires, final Boolean pRegSendRegister, final Boolean pPublish, final Boolean pDialEscapePlus, final CallbackContext callbackContext) {
        try {
            if(!pRegProxy.isEmpty() && !pRegIdentity.isEmpty() && pRegExpires != 0 && pRegSendRegister != null && pPublish != null && pDialEscapePlus != null) {
                mSettingsEditor.putString("regProxy", pRegProxy);
                mSettingsEditor.putString("regIdentity", pRegIdentity);
                mSettingsEditor.putInt("regExpires", pRegExpires);
                mSettingsEditor.putBoolean("regSendRegister", pRegSendRegister);
                mSettingsEditor.putBoolean("publish", pPublish);
                mSettingsEditor.putBoolean("dialEscapePlus", pDialEscapePlus);
                mSettingsEditor.apply();
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Proxy parameters saved/updated"));
            } else {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Proxy parameters are empty"));
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    /**
     * Log into the sip server, trigger authInfoRequested and registrationState function
     * @param callbackContext CallbackContext
     */
    public static synchronized void login(final CallbackContext callbackContext) throws JSONException {
        try {
            String username = LinphoneUtils.getDisplayableUsernameFromAddress(mSettings.getString("authUsername", "default"));
            String domain = LinphoneUtils.getDisplayableUsernameFromAddress(mSettings.getString("authRealm", "default"));
            String identity = "sip:" + username + "@" + domain;

            mProxyCfg = mLinphoneManager.getLc().createProxyConfig();
            LinphoneAuthInfo info;
            info = LinphoneCoreFactory.instance().createAuthInfo(username, mSettings.getString("authPaswd", "default"), null, domain);
            //mLinphoneManager.setStunServer(mDefaultStun);
            mLinphoneManager.getLc().addAuthInfo(info);
            mProxyCfg.setIdentity(identity);
            mProxyCfg.setProxy(domain);
            mProxyCfg.enableRegister(true);
            mLinphoneManager.getLc().addProxyConfig(mProxyCfg);
            mLinphoneManager.getLc().setDefaultProxyConfig(mProxyCfg);
            callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Logging..."));
        } catch (Exception e) {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

    /**
     * Log out the sip server, trigger authInfoRequested and registrationState function
     * @param callbackContext CallbackContext
     */
    public static synchronized void logout(final CallbackContext callbackContext) throws JSONException {
        try {
            mLinphoneManager.getLc().setDefaultProxyConfig(mProxyCfg);
            mProxyCfg.edit();
            mProxyCfg.enableRegister(false);
            mProxyCfg.done();
            callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Disconnecting..."));
        } catch (Exception e) {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

    /**
     * Log out the sip server, trigger authInfoRequested and registrationState function
     * @param pMessage String
     * @param pSipUri String
     * @param pNewChatConversation boolean
     * @param callbackContext CallbackContext
     */
    public static synchronized void sendMessage(final String pMessage, final String pSipUri, final boolean pNewChatConversation, final CallbackContext callbackContext) throws JSONException {
        try {
		boolean isNetworkReachable = mLinphoneManager.getLc() == null ? false : mLinphoneManager.getLc().isNetworkReachable();
		LinphoneAddress lAddress = null;

		if(pNewChatConversation && mChatRoom == null) {
			if (pSipUri != null && !pSipUri.equals("")) {
				lAddress = mLinphoneManager.getLc().interpretUrl(pSipUri);
				mChatRoom = mLinphoneManager.getLc().getChatRoom(lAddress);
			}
		}
		if (mChatRoom != null && pMessage != null && pMessage.length() > 0 && isNetworkReachable) {
			LinphoneChatMessage message = mChatRoom.createLinphoneChatMessage(pMessage);
			mChatRoom.sendChatMessage(message);
			// TODO : listener message state
			callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Sending..."));
		} 
        } catch (Exception e) {
            	callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

    /**
     * Outgoing call an sip address without video
     * @param pAddress String
     * @param pDisplayName String
     * @param callbackContext CallbackContext
     */
    public static synchronized void call(final String pAddress, final String pDisplayName, final CallbackContext callbackContext) throws JSONException {
        LinphoneAddress lAddress = null;
        try {
            lAddress = mLinphoneManager.getLc().interpretUrl(pAddress);
            LinphoneProxyConfig lpc = mLinphoneManager.getLc().getDefaultProxyConfig();
            if (lpc != null && lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
                return;
            }
        } catch (LinphoneCoreException e) {
            callbackContext.error(e.getMessage());
        }
        if(!pDisplayName.isEmpty()) {
          lAddress.setDisplayName(pDisplayName);
        } else {
          lAddress.setDisplayName(LinphoneUtils.getDisplayableUsernameFromAddress(pAddress));
        }
        boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(mContext);
        if (mLinphoneManager.getLc().isNetworkReachable()) {
            try {
                LinphoneCallParams params = mLinphoneManager.getLc().createCallParams(null);
                params.enableLowBandwidth(isLowBandwidthConnection);
                mLinphoneManager.getLc().inviteAddressWithParams(lAddress, params);
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Calling..."));
            } catch (LinphoneCoreException e) {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
            }
        } else {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Network unreachable"));
        }
    }

    /**
     * Answer incoming call
     * @param callbackContext CallbackContext
     */
    public static synchronized void answerCall(final CallbackContext callbackContext) throws JSONException {
        if (mLinphoneManager.getLc().getCurrentCall() != null && mLinphoneManager.getLc().getCurrentCall().getState() == LinphoneCall.State.IncomingReceived) {
            try {
                mLinphoneManager.getLc().acceptCall(mLinphoneManager.getLc().getCurrentCall());
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Take call"));
            } catch (LinphoneCoreException e) {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
            }
        } else {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Not calling"));
        }
    }

    /**
     * Terminate call
     * @param callbackContext CallbackContext
     */
    public static synchronized void hangup(final CallbackContext callbackContext) throws JSONException {
        if(mLinphoneManager.getLc().getCurrentCall() != null) {
            try {
                if(mLinphoneManager.isRinging()) mLinphoneManager.stopRinging();
                mLinphoneManager.getLc().terminateCall(mLinphoneManager.getLc().getCurrentCall());
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Terminate call"));
            } catch (Exception e) {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
            }
        } else {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Not calling"));
        }
    }

    /**
     * Toggle speaker
     * @param callbackContext CallbackContext
     */
    public static synchronized void toggleSpeaker(final CallbackContext callbackContext) throws JSONException {
        if(mLinphoneManager.getLc().getCurrentCall() != null && mLinphoneManager.getLc().getCurrentCall().getState() == LinphoneCall.State.StreamsRunning) {
            try {
                mLinphoneManager.toggleEnableSpeaker();
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Speaker ON/OFF"));
            } catch (Exception e) {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
            }
        } else {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Not calling"));
        }
    }

    /**
     * Toggle mute mic
     * @param callbackContext CallbackContext
     */
    public static synchronized void toggleMute(final CallbackContext callbackContext) throws JSONException {
        if(mLinphoneManager.getLc().getCurrentCall() != null && mLinphoneManager.getLc().getCurrentCall().getState() == LinphoneCall.State.StreamsRunning) {
            try {
                mLinphoneManager.toggleMute();
                callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Mic ON/OFF"));
            } catch (Exception e) {
                callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
            }
        } else {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", "Not calling"));
        }
    }

    /**
     * Send DTMF
     * @param number String cast to char
     * @param callbackContext CallbackContext
     */
    public static synchronized void sendDtmf(final String character, final CallbackContext callbackContext) throws JSONException {
        try {
            mLinphoneManager.sendDtmf(character.charAt(0));
            callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Dtmf sent : " + number));
        } catch (Exception e) {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

    /**
     * Toggle video
     * @param number String cast to char
     * @param callbackContext CallbackContext
     */
    public static synchronized void toggleVideo(final CallbackContext callbackContext) throws JSONException {
        try {
            // TODO
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

    public static synchronized void videoCall(final String address, final String displayName, final CallbackContext callbackContext) throws JSONException {
        LinphoneAddress lAddress = null;
        try {
            int camId = 0;
            AndroidCameraConfiguration.AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
            for (AndroidCameraConfiguration.AndroidCamera androidCamera : cameras) {
                if (androidCamera.frontFacing)
                    camId = androidCamera.id;
            }
            lAddress = mLinphoneManager.getLc().interpretUrl(address);
            boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(mContext);
            LinphoneCallParams params = mLinphoneManager.getLc().createCallParams(null);
            params.setVideoEnabled(true);
            if(isLowBandwidthConnection) {
                params.enableLowBandwidth(true);
            }
            mLinphoneManager.getLc().setVideoDevice(camId);
            mLinphoneManager.getLc().inviteAddressWithParams(lAddress, params);
            callbackContext.success(LinphoneUtils.formatCallbackAnswer("success", "Calling with video..."));

            // TODO
            //Intent intent = new Intent(mContext, LinphoneCallVideoActivity.class);
            //mContext.startActivity(intent);
        } catch (Exception e) {
            callbackContext.error(LinphoneUtils.formatCallbackAnswer("error", e.getMessage()));
        }
    }

}