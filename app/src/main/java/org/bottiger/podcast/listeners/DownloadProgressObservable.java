package org.bottiger.podcast.listeners;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

public class DownloadProgressObservable {

	/**
	 * How often the UI should refresh
	 */
	private static final long REFRESH_INTERVAL = 50; // 16 ms => 60 fps
	//TimeUnit.MILLISECONDS.convert(1,TimeUnit.SECONDS);

    private static HashMap<IEpisode, IDownloadEngine> mPodcastDownloadManager = null;

    private static SoundWaves mApplicationContext;

	/**
	 * Handler events types
	 */
	private static final int REFRESH_UI = 1;
    private static final int ADD_ID = 2;
    private static final int DELETED = 3;

    public DownloadProgressObservable(@NonNull SoundWaves context) {
        mPodcastDownloadManager = EpisodeDownloadManager.mDownloadingEpisodes;
        mApplicationContext = context;
    }

	/**
	 * Unregister an Observer from being updated on progress updates
     *
     * Returns true if the observer was found and removed
	 */
	public static final Handler sHandler = new DownloadProgressHandler();

    private static class DownloadProgressHandler extends Handler {

        private List<FeedItem> mUpdateEpisodess = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        @Override
        public void handleMessage(Message msg) {

            // http://developer.android.com/reference/java/util/concurrent/locks/ReentrantLock.html
            lock.lock();  // block until condition holds
            try {

                switch (msg.what) {
                    case REFRESH_UI: {

                        Log.d("Refresh UI:", "Run at: "+ (new Date().getTime()));

                        Iterator<FeedItem> iterator = mUpdateEpisodess.iterator();
                        while (iterator.hasNext()) {

                            FeedItem episode = iterator.next();

                            if (episode == null) {
                                throw new NullPointerException("Episode can not be null!");
                            }

                            DownloadStatus status = EpisodeDownloadManager.getStatus(episode);
                            IDownloadEngine download = mPodcastDownloadManager.get(episode);

                            DownloadProgress downloadProgress = null;

                            int progress;

                            switch (status) {
                                case DOWNLOADING:
                                    float progressFloat = download.getProgress();
                                    progress = (int)(progressFloat*100);

                                    downloadProgress = new DownloadProgress(episode, status, progress);
                                    Log.d("Refresh UI:", "Downloading: "+ episode.title + " (progress: " + progress + ")");
                                    break;
                                case PENDING:
                                    Log.d("Refresh UI:", "Pending: "+ episode.title);
                                    break;
                                case DONE:
                                case NOTHING:
                                case ERROR:
                                    Log.d("Refresh UI:", "End: "+ episode.title);
                                    downloadProgress = new DownloadProgress(episode, status, 100);
                                    iterator.remove();
                                    break;
                                default:
                                    break;
                            }

                            if (downloadProgress != null) {
                                mApplicationContext.getBus().post(downloadProgress);
                            }
                        }

                        if (!mUpdateEpisodess.isEmpty()) {
                            msg = sHandler.obtainMessage(REFRESH_UI);
                            sHandler.sendMessageDelayed(msg, REFRESH_INTERVAL);
                        }
                        break;
                    }
                    case ADD_ID: {
                        FeedItem episode = (FeedItem) msg.obj;
                        mUpdateEpisodess.add(episode);
                        refreshUI();
                        break;
                    }
                    case DELETED: {
                        IEpisode episode = (FeedItem) msg.obj;
                        DownloadProgress downloadProgress = new DownloadProgress(episode, DownloadStatus.DELETED, 0);
                        mApplicationContext.getBus().post(downloadProgress);
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
	}

    public void addEpisode(@NonNull FeedItem argEpisode) {
        Message msg = sHandler.obtainMessage(ADD_ID);
        msg.obj = argEpisode;
        sHandler.sendMessage(msg);
    }

    public static void deleteEpisode(@NonNull FeedItem argEpisode) {
        Message msg = sHandler.obtainMessage(DELETED);
        msg.obj = argEpisode;
        sHandler.sendMessage(msg);
    }

	/**
	 * Refrersh the UI handler
	 */
	private static void refreshUI() {
		sHandler.removeMessages(REFRESH_UI);
		Message msg = sHandler.obtainMessage(REFRESH_UI);
		sHandler.sendMessage(msg);
	}

}
