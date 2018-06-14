package cordova.scrachx.linphone;

import org.linphone.mediastream.video.AndroidVideoWindowImpl;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LinphoneCallVideoActivity extends Activity {

    private SurfaceView mVideoView;
    private SurfaceView mCaptureView;
    private AndroidVideoWindowImpl androidVideoWindowImpl;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getResources().getIdentifier("video_call_fragment", "layout", getPackageName()));

        mVideoView = (SurfaceView) findViewById(getViewId("videoSurface"));
        mCaptureView = (SurfaceView) findViewById(getViewId("videoCaptureSurface"));
        mCaptureView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Warning useless because value is ignored and automatically set by new APIs.

        androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, mCaptureView, new AndroidVideoWindowImpl.VideoWindowListener() {
            public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
                mVideoView = surface;
                LinphoneManager.getLc().setVideoWindow(vw);
            }

            public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {

            }

            public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
                mCaptureView = surface;
                LinphoneManager.getLc().setPreviewWindow(mCaptureView);
            }

            public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (androidVideoWindowImpl != null) {
            synchronized (androidVideoWindowImpl) {
                LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
            }
        }
    }

    @Override
    public void onPause() {
        if (androidVideoWindowImpl != null) {
            synchronized (androidVideoWindowImpl) {
				/*
				 * this call will destroy native opengl renderer which is used by
				 * androidVideoWindowImpl
				 */
                LinphoneManager.getLc().setVideoWindow(null);
            }
        }
        super.onPause();
    }

    public int getViewId(String name) {
        return getResources().getIdentifier(name, "id", getPackageName());
    }
    
}