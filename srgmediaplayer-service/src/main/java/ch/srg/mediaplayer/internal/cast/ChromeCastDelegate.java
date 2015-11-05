package ch.srg.mediaplayer.internal.cast;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;

import ch.srg.mediaplayer.PlayerDelegate;
import ch.srg.mediaplayer.SRGMediaPlayerException;
import ch.srg.mediaplayer.SRGMediaPlayerView;
import ch.srg.mediaplayer.internal.cast.exceptions.NoConnectionException;


/**
 * Created by npietri on 19.10.15.
 */
public class ChromeCastDelegate implements PlayerDelegate, ChromeCastManager.Listener {

    private static final String TAG = "ChromeCastDelegate";
    private final OnPlayerDelegateListener controller;
    private final ChromeCastManager chromeCastManager;

    private MediaInfo mediaInfo;

    private boolean delegateReady;
    private boolean playIfReady;
    private long pendingSeekTo;
    private String title;
    private String subTitle;
    private String mediaThumbnailUrl;
    private long lastKnownPosition;
    private long lastKnownDuration;

    public ChromeCastDelegate(OnPlayerDelegateListener controller) {
        this.chromeCastManager = ChromeCastManager.getInstance();
        this.controller = controller;
        chromeCastManager.addListener(this);
    }

    @Override
    public boolean canRenderInView(View view) {
        return true;
    }

    @Override
    public View createRenderingView(Context parentContext) {
        return new View(parentContext);
    }

    @Override
    public void bindRenderingViewInUiThread(SRGMediaPlayerView mediaPlayerView) throws SRGMediaPlayerException {

    }

    @Override
    public void unbindRenderingView() {

    }


    @Override
    public void prepare(Uri videoUri) throws SRGMediaPlayerException {
        Log.d(TAG, "Prepare: " + videoUri);
        controller.onPlayerDelegatePreparing(this);

        String metadataTitle;
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);

        if (title != null) {
            metadataTitle = title;
        } else {
            Context context = controller.getContext();
            metadataTitle = context.getString(context.getApplicationInfo().labelRes);
        }
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, metadataTitle);
        if (subTitle != null) {
            mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, subTitle);
        }

        if (mediaThumbnailUrl != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(mediaThumbnailUrl)));
        }

        mediaInfo = new MediaInfo.Builder(String.valueOf(videoUri))
                .setContentType("application/x-mpegurl")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            chromeCastManager.loadMedia(mediaInfo, playIfReady, pendingSeekTo);
        } catch (NoConnectionException e) {
            e.printStackTrace();
            throw new SRGMediaPlayerException(e);
        }
        Log.d(TAG, "onPlayerDelegatePlayWhenReadyCommited");
        delegateReady = true;
        controller.onPlayerDelegateReady(ChromeCastDelegate.this);
    }

    @Override
    public void playIfReady(boolean playIfReady) throws IllegalStateException {
        Log.d(TAG, "PlayIfReady: " + playIfReady);

        if (mediaInfo != null && this.playIfReady != playIfReady) {
            if (playIfReady) {
                if (chromeCastManager.getMediaStatus() == MediaStatus.PLAYER_STATE_PAUSED) {
                    Log.d(TAG, "remoteMediaPlayer.play");
                    try {
                        chromeCastManager.play();
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                        throw new IllegalStateException(e);
                    }
                }
            } else {
                Log.d(TAG, "remoteMediaPlayer.pause");
                try {
                    chromeCastManager.pause();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
                    throw new IllegalStateException(e);
                }
            }
        }

        this.playIfReady = playIfReady;
    }

    @Override
    public void seekTo(long positionInMillis) throws IllegalStateException {
        Log.d(TAG, "seekTo: " + positionInMillis);
        if (delegateReady) {
            try {
                if (chromeCastManager.isRemoteMediaPlaying()) {
                    controller.onPlayerDelegateBuffering(this);
                    Log.d(TAG, "remoteMediaPlayer.seek");
                    chromeCastManager.seek(positionInMillis);
                    controller.onPlayerDelegateReady(this);
                } else {
                    chromeCastManager.loadMedia(mediaInfo, playIfReady, positionInMillis);
                }
            } catch (NoConnectionException e) {
                throw new IllegalStateException(e);
            }
        } else {
            pendingSeekTo = positionInMillis;
        }
    }

    @Override
    public boolean isPlaying() {
        return chromeCastManager.getMediaStatus() == MediaStatus.PLAYER_STATE_PLAYING;
    }

    @Override
    public void setMuted(boolean muted) {
        try {
            chromeCastManager.setMuted(muted);
        } catch (NoConnectionException ignored) {
        }
    }

    @Override
    public long getCurrentPosition() {
        try {
            if (chromeCastManager.isRemoteMediaPlaying()) {
                // Check is necessary otherwise we get a 0 from the Google API
                lastKnownPosition = chromeCastManager.getMediaPosition();
            }
        } catch (NoConnectionException ignored) {
        }
        return lastKnownPosition;
    }

    @Override
    public long getDuration() {
        try {
            lastKnownDuration = chromeCastManager.getMediaDuration();
        } catch (NoConnectionException ignored) {
        }
        return lastKnownDuration;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public long getBufferPosition() {
        return 0;
    }

    @Override
    public int getVideoSourceHeight() {
        return 0;
    }

    @Override
    public void release() throws IllegalStateException {
        if (chromeCastManager.isConnected()) {
            try {
                chromeCastManager.stop();
            } catch (NoConnectionException e) {
                throw new IllegalStateException(e);
            }
        }
        chromeCastManager.removeListener(this);
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public long getPlaylistStartTime() {
        return 0;
    }

    public void setMediaTitle(String title) {
        this.title = title;
    }

    public void setMediaSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public void setMediaThumbnailUrl(String mediaThumbnailUrl) {
        this.mediaThumbnailUrl = mediaThumbnailUrl;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public void onChromeCastApplicationConnected() {
    }

    @Override
    public void onChromeCastApplicationDisconnected() {
    }

    @Override
    public void onChromeCastPlayerStatusUpdated(int state, int idleReason) {
        switch (state) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                controller.onPlayerDelegatePlayWhenReadyCommited(this);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                controller.onPlayerDelegatePlayWhenReadyCommited(this);
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = idle");
                switch (idleReason) {
                    case MediaStatus.IDLE_REASON_FINISHED:
                        controller.onPlayerDelegateCompleted(this);
                        break;
                    case MediaStatus.IDLE_REASON_ERROR:
                    case MediaStatus.IDLE_REASON_CANCELED:
                    case MediaStatus.IDLE_REASON_INTERRUPTED:
                        controller.onPlayerDelegateError(this, new SRGMediaPlayerException("ChromeCast " + idleReason));
                        break;
                    default:
                        Log.e(TAG, "onRemoteMediaPlayerStatusUpdated(): Unexpected Idle Reason "
                                + idleReason);
                }
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                controller.onPlayerDelegateBuffering(this);
                break;
            default:
                Log.d(TAG, "onRemoteMediaPlayerStatusUpdated(): Player status = unknown");
                break;
        }
    }
}