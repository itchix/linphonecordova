package cordova.scrachx.linphone;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.core.LpConfig;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.telephony.TelephonyManager;

public final class LinphoneUtils {

	private LinphoneUtils(){}

	public static boolean isSipAddress(String numberOrAddress) {
		try {
			LinphoneCoreFactory.instance().createLinphoneAddress(numberOrAddress);
			return true;
		} catch (LinphoneCoreException e) {
			return false;
		}
	}

	public static boolean isNumberAddress(String numberOrAddress) {
		LinphoneProxyConfig proxy = LinphoneManager.getLc().createProxyConfig();
		return proxy.normalizePhoneNumber(numberOrAddress) != null;
	}

	public static boolean isStrictSipAddress(String numberOrAddress) {
		return isSipAddress(numberOrAddress) && numberOrAddress.startsWith("sip:");
	}

	public static String getAddressDisplayName(String uri){
		LinphoneAddress lAddress;
		try {
			lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(uri);
			return getAddressDisplayName(lAddress);
		} catch (LinphoneCoreException e) {
			return null;
		}
	}

	public static String getAddressDisplayName(LinphoneAddress address){
		if(address.getDisplayName() != null) {
			return address.getDisplayName();
		} else {
			if(address.getUserName() != null){
				return address.getUserName();
			} else {
				return address.asStringUriOnly();
			}
		}
	}

	public static String formatCallbackAnswer(String status, String message) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("status", status);
		json.put("message", message);
		return json.toString();
	}

	public static String getUsernameFromAddress(String address) {
		if (address.contains("sip:"))
			address = address.replace("sip:", "");

		if (address.contains("@"))
			address = address.split("@")[0];

		return address;
	}

	public static String timestampToHumanDate(Context context, long timestamp, String format) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp);

			SimpleDateFormat dateFormat;
			if (isToday(cal)) {
				dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
			} else {
				dateFormat = new SimpleDateFormat(format, Locale.getDefault());
			}

			return dateFormat.format(cal.getTime());
		} catch (NumberFormatException nfe) {
			return String.valueOf(timestamp);
		}
	}

	static boolean isToday(Calendar cal) {
		return isSameDay(cal, Calendar.getInstance());
	}

	static boolean isSameDay(Calendar cal1, Calendar cal2) {
		if (cal1 == null || cal2 == null) {
			return false;
		}

		return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}

    public static void copyIfNotExist(Context context, int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(context, ressourceId, lFileToCopy.getName());
        }
    }

    public static void copyFromPackage(Context context, int ressourceId, String target) throws IOException {
        FileOutputStream lOutputStream = context.openFileOutput (target, 0);
        InputStream lInputStream = context.getResources().openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while (( readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff,0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

	public static boolean isHighBandwidthConnection(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && isConnectionFast(info.getType(),info.getSubtype()));
    }

	private static boolean isConnectionFast(int type, int subType){
		if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_IDEN:
            	return false;
            }
		}
        //in doubt, assume connection is good.
        return true;
    }

	public static void clearLogs() {
		try {
			Runtime.getRuntime().exec(new String[] { "logcat", "-c" });
		} catch (IOException e) {
			Log.e(e);
		}
	}

    public static boolean zipLogs(StringBuilder sb, String toZipFile){
        boolean success = false;
        try {
            FileOutputStream zip = new FileOutputStream(toZipFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(zip));
            ZipEntry entry = new ZipEntry("logs.txt");
            out.putNextEntry(entry);

            out.write(sb.toString().getBytes());

            out.close();
            success = true;

        } catch (Exception e){
            Log.e("Exception when trying to zip the logs: " + e.getMessage());
        }

        return success;
    }

	public static void collectLogs(Context context, String email) {
        BufferedReader br = null;
        Process p = null;
        StringBuilder sb = new StringBuilder();

    	try {
			p = Runtime.getRuntime().exec(new String[] { "logcat", "-d", "|", "grep", "`adb shell ps | grep " + context.getPackageName() + " | cut -c10-15`" });
	    	br = new BufferedReader(new InputStreamReader(p.getInputStream()), 2048);

            String line;
	    	while ((line = br.readLine()) != null) {
	    		sb.append(line);
	    		sb.append("\r\n");
	    	}
            String zipFilePath = context.getExternalFilesDir(null).getAbsolutePath() + "/logs.zip";
            Log.i("Saving logs to " + zipFilePath);

            if( zipLogs(sb, zipFilePath) ) {
            	final String appName = (context != null) ? "Test" : "Linphone(?)";

                Uri zipURI = Uri.parse("file://" + zipFilePath);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                i.putExtra(Intent.EXTRA_SUBJECT, appName + " Logs");
                i.putExtra(Intent.EXTRA_TEXT, appName + " logs");
                i.setType("application/zip");
                i.putExtra(Intent.EXTRA_STREAM, zipURI);
                try {
                    context.startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {

                }
            }

		} catch (IOException e) {
			Log.e(e);
		}
	}

	public static String getExtensionFromFileName(String fileName) {
		String extension = null;
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}
		return extension;
	}

	public static void recursiveFileRemoval(File root) {
		if (!root.delete()) {
			if (root.isDirectory()) {
				File[] files = root.listFiles();
		        if (files != null) {
		            for (File f : files) {
		            	recursiveFileRemoval(f);
		            }
		        }
			}
		}
	}

	public static String getDisplayableUsernameFromAddress(String sipAddress) {
		String username = sipAddress;
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) return username;

		if (username.startsWith("sip:")) {
			username = username.substring(4);
		}

		if (username.contains("@")) {
			String domain = username.split("@")[1];
			LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
			if (lpc != null) {
				if (domain.equals(lpc.getDomain())) {
					return username.split("@")[0];
				}
			} else {
				if (domain.equals("sip.linphone.org")) {
					return username.split("@")[0];
				}
			}
		}
		return username;
	}

	public static String getFullAddressFromUsername(String username) {
		String sipAddress = username;
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc == null) return sipAddress;

		if (!sipAddress.startsWith("sip:")) {
			sipAddress = "sip:" + sipAddress;
		}

		if (!sipAddress.contains("@")) {
			LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
			if (lpc != null) {
				sipAddress = sipAddress + "@" + lpc.getDomain();
			} else {
				sipAddress = sipAddress + "@sip.linphone.org";
			}
		}
		return sipAddress;
	}

	public static void storeImage(Context context, LinphoneChatMessage msg) {
		if (msg == null || msg.getFileTransferInformation() == null || msg.getAppData() == null) return;
		File file = new File(Environment.getExternalStorageDirectory(), msg.getAppData());
		Bitmap bm = BitmapFactory.decodeFile(file.getPath());
		if (bm == null) return;

		ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, file.getName());
        String extension = msg.getFileTransferInformation().getSubtype();
        values.put(Images.Media.MIME_TYPE, "image/" + extension);
        ContentResolver cr = context.getContentResolver();
        Uri path = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        OutputStream stream;
		try {
			stream = cr.openOutputStream(path);
			if (extension != null && extension.toLowerCase(Locale.getDefault()).equals("png")) {
				bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
			} else {
				bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			}

			stream.close();
			file.delete();
	        bm.recycle();

	        msg.setAppData(path.toString());
		} catch (FileNotFoundException e) {
			Log.e(e);
		} catch (IOException e) {
			Log.e(e);
		}
	}

	public static String getRingtone(String defaultRingtone, LinphoneCore lc) {
        String ringtone = lc.getConfig().getString("app", "ringtone", defaultRingtone);
        if (ringtone == null || ringtone.length() == 0)
            ringtone = defaultRingtone;
        return ringtone;
    }

}

