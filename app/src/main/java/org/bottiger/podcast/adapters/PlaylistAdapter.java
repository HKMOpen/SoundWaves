package org.bottiger.podcast.adapters;

import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.validator.routines.UrlValidator;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.images.FrescoHelper;
import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.Crypto;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.ThemeHelper;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlaylistViewHolder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.imagepipeline.request.BasePostprocessor;
import com.squareup.otto.Subscribe;

public class PlaylistAdapter extends AbstractPodcastAdapter<PlaylistViewHolder> {

    private static final String TAG = "PlaylistAdapter";

    public static final int TYPE_EXPAND = 1;
	public static final int TYPE_COLLAPSE = 2;

    public static final int PLAYLIST_OFFSET = 1;

    public static ExpandableViewHoldersUtil.KeepOneH<PlaylistViewHolder> keepOne = new ExpandableViewHoldersUtil.KeepOneH<>();

    private Activity mActivity;
    private View mOverlay;

    private DownloadProgressObservable mDownloadProgressObservable = null;

	public static TreeSet<Number> mExpandedItemID = new TreeSet<>();

	private static DownloadManager mDownloadManager = null;

    public PlaylistAdapter(@NonNull Activity argActivity, View argOverlay, DownloadProgressObservable argDownloadProgressObservable) {
        super(argActivity);
        mActivity = argActivity;
        mOverlay = argOverlay;
        mInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDownloadProgressObservable = argDownloadProgressObservable;

        mDownloadManager = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);

        notifyDataSetChanged();
    }

    @Override
    public PlaylistViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Log.v(TAG, "onCreateViewHolder");

        View view = mInflater.inflate(R.layout.episode_list, viewGroup, false);
        PlaylistViewHolder holder = new PlaylistViewHolder(view, mActivity);

        return holder;
    }

    private final int[] mGradientColors = new int[] {0,0xDD000000};
    private GradientDrawable mActionBarGradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, mGradientColors);

    @Override
    public void onBindViewHolder(final PlaylistViewHolder viewHolder, final int position) {
        Log.v(TAG, "onBindViewHolder(pos: " + position + ")");

        final IEpisode item = mPlaylist.getItem(position+PLAYLIST_OFFSET);
        final Activity activity = mActivity;

        if (item == null) {
            // This should only happen if the playlist only contain 1 item
            return;
        }

        viewHolder.setArtwork(null);

        UrlValidator urlValidator = new UrlValidator();
        String image = item.getArtwork(mActivity);
        if (!TextUtils.isEmpty(image) && urlValidator.isValid(image)) {

            FrescoHelper.PalettePostProcessor postProcessor = new FrescoHelper.PalettePostProcessor(mActivity, image);
            FrescoHelper.loadImageInto(viewHolder.mItemBackground, image, postProcessor);

            viewHolder.setArtwork(image);
        }

        keepOne.bind(viewHolder, position);

        Log.d("ExpanderHelper", "pos: " + position + " episode: " + item.getTitle());

        String artwork = item.getArtwork(activity);
        PaletteHelper.generate(artwork, activity, viewHolder.mPlayPauseButton);
        PaletteHelper.generate(artwork, activity, new PaletteListener() {
            @Override
            public void onPaletteFound(Palette argChangedPalette) {
                int white = mActivity.getResources().getColor(R.color.white_opaque);

                ColorExtractor colorExtractor = new ColorExtractor(mActivity, argChangedPalette);
                //viewHolder.mLayout.setCardBackgroundColor(colorExtractor.getPrimary());
                //viewHolder.mMainTitle.setTextColor(colorExtractor.getTextColor());
                ////viewHolder.buttonLayout.setBackgroundColor(colorExtractor.getPrimary());
                //viewHolder.description.setTextColor(colorExtractor.getTextColor());
                //viewHolder.currentTime.setTextColor(colorExtractor.getTextColor());
                //viewHolder.mTimeDuration.setTextColor(colorExtractor.getTextColor());
            }

            @Override
            public String getPaletteUrl() {
                return null;
            }
        });

        viewHolder.episode = item;
        viewHolder.mAdapter = this;

        viewHolder.mMainTitle.setText(item.getTitle());
        viewHolder.description.setText(item.getDescription());
        bindDuration(viewHolder, item);

        if (item.getPriority() > 0) {
            viewHolder.mPlaylistPosition.setText(Integer.toString(position));
            viewHolder.mPlaylistPosition.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mPlaylistPosition.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= 16) {
            //viewHolder.mActionBarGradientView.setBackground(mActionBarGradientDrawable);
        }

        viewHolder.mPlayPauseButton.setEpisode(item, PlayPauseImageView.LOCATION.PLAYLIST);
        viewHolder.mPlayPauseButton.setStatus(PlayerStatusObservable.STATUS.PAUSED);

        viewHolder.downloadButton.setEpisode(item);
        //mDownloadProgressObservable.registerObserver(viewHolder.downloadButton);


        bindExandedPlayer(mActivity, item, viewHolder, position);
    }

    /**
     * Expands the StubView and creates the expandable extended_player. This is done for
     * the current playing episode and at most one other episode which the user
     * is interacting with
     *
     * @param holder
     * @param position
     */
    public void bindExandedPlayer(final Context context, final IEpisode feedItem,
                                  final PlaylistViewHolder holder, final int position) {
        Log.v("PlaylistAdapter", "bindExandedPlayer");


        ThemeHelper themeHelper = new ThemeHelper(context);

        //holder.playerRelativeLayout.setVisibility(View.VISIBLE);

        long playerPosition = 0;
        long playerDuration = 0;

        if (MainActivity.sBoundPlayerService != null) {
            if (position == 0
                    && MainActivity.sBoundPlayerService.isPlaying()) {
                playerPosition = MainActivity.sBoundPlayerService
                        .position();
                playerDuration = MainActivity.sBoundPlayerService
                        .duration();
            } else {
                if (feedItem instanceof FeedItem) {
                    playerPosition = ((FeedItem)feedItem).offset;
                }
                playerDuration = 0;//feedItem.getDuration();
            }
        }

        holder.currentTime.setText(StrUtils.formatTime(playerPosition));

        //holder.downloadButton.registerListener(paletteObservable);

        holder.seekbar.setEpisode(feedItem);
        holder.seekbar.setOverlay(mOverlay);

        holder.mPlayPauseButton.setEpisode(feedItem, PlayPauseImageView.LOCATION.PLAYLIST);
        holder.downloadButton.setEpisode(feedItem);
        holder.favoriteButton.setEpisode(feedItem);
        holder.removeButton.setEpisode(feedItem);
        holder.downloadButton.setEpisode(feedItem);

        PaletteHelper.generate(feedItem.getArtwork(mActivity), mActivity, holder.downloadButton);
        PaletteHelper.generate(feedItem.getArtwork(mActivity), mActivity, holder.favoriteButton);
        PaletteHelper.generate(feedItem.getArtwork(mActivity), mActivity, holder.removeButton);


        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistAdapter.toggle(holder, position);

                if (feedItem instanceof FeedItem) { // FIXME
                    ((FeedItem) feedItem).removeFromPlaylist(context.getContentResolver());
                }
                //PlaylistAdapter.this.notifyItemRemoved(position);
                notifyDataSetChanged();
                mPlaylist.removeItem(position + PLAYLIST_OFFSET);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.v("PlaylistAdapter", "getItemCount");
        //int cursorCount = mCursor == null ? 0 : mCursor.getCount();
        int playlistCount = mPlaylist == null ? 0 : mPlaylist.size();
        //int minCount = Math.min(cursorCount, playlistCount);

        //return minCount-PLAYLIST_OFFSET;
        return playlistCount-PLAYLIST_OFFSET;
    }

    @Override
    public void onViewRecycled(PlaylistViewHolder viewHolder) {
        Log.v("PlaylistAdapter", "onViewRecycled");

        if (viewHolder == null)
            return;

        PlaylistViewHolder holder = viewHolder;

        if (holder.episode == null) {
            return;
        }

        //mDownloadProgressObservable.unregisterObserver(holder.downloadButton);


        holder.mPlayPauseButton.unsetEpisodeId();
        holder.favoriteButton.unsetEpisodeId();
        holder.removeButton.unsetEpisodeId();
        holder.downloadButton.unsetEpisodeId();
    }

    @Override
    public void onViewAttachedToWindow (PlaylistViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        SoundWaves.getBus().register(holder.mPlayPauseButton);
        SoundWaves.getBus().register(holder.seekbar);
    }

    @Override
    public void  onViewDetachedFromWindow(PlaylistViewHolder holder) {
        SoundWaves.getBus().unregister(holder.mPlayPauseButton);
        SoundWaves.getBus().unregister(holder.seekbar);
        super.onViewDetachedFromWindow(holder);
    }

    /**
     1.) Observable is a Class and Observer is an Interface
     2.) Observable class maintain a list of observers
     3.) When an Observable object is updated it invokes the update() method of each of its observers to notify that, it is changed

     * @param observer
     */
    @Override
    public void registerAdapterDataObserver (RecyclerView.AdapterDataObserver observer) {
        Log.v("PlaylistAdapter", "registerAdapterDataObserver");
        super.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver (RecyclerView.AdapterDataObserver observer) {
        Log.v("PlaylistAdapter", "unregisterAdapterDataObserver");
        super.unregisterAdapterDataObserver(observer);
    }

	public static void showItem(Long id) {
        Log.v("PlaylistAdapter", "showItem");
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // only show
														     // one expanded
															 // at the time
		mExpandedItemID.add(id);
	}

	public static int toggleItem(Long id) {
        Log.v("PlaylistAdapter", "toggleItem");
		if (mExpandedItemID.contains(id)) {
            mExpandedItemID.remove(id);
            return TYPE_COLLAPSE;
        } else {
			showItem(id);
            return TYPE_EXPAND;
		}
	}

    // http://stackoverflow.com/questions/5300962/getviewtypecount-and-getitemviewtype-methods-of-arrayadapter
	@Override
    public int getItemViewType(int position) {

        Long id = itemID(position+PLAYLIST_OFFSET); // The recyclervies does not start with item 1 in the playlist
		boolean isExpanded = mExpandedItemID.contains(id);

        Log.v("PlaylistAdapter", "getItemViewType: " + isExpanded);
		return isExpanded ? TYPE_EXPAND : TYPE_COLLAPSE;
	}

	/**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private static Long itemID(int position) {
        Log.v("PlaylistAdapter", "itemID");

        PlayerService ps = MainActivity.sBoundPlayerService;
        if (ps == null)
            return -1L;

        Playlist playlist = ps.getPlaylist();
        IEpisode episode = playlist.getItem(position);

        if (episode == null)
            return -1L;

        String url = episode.getUrl().toString();
        //Long id = UUID.fromString(url).getLeastSignificantBits();
        return (long)url.hashCode();
	}

    @Subscribe
    public void playlistChanged(@NonNull Playlist argPlaylist) {
        mPlaylist = argPlaylist;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId (int position) {
        return itemID(position);
    }

    public static void toggle(PlaylistViewHolder pvh, int pos) {
        Long id = itemID(pos+PLAYLIST_OFFSET);
        //toggleItem(id);
        keepOne.toggle(pvh);
    }

    private void bindDuration(@NonNull PlaylistViewHolder argHolder, @NonNull IEpisode argFeedItem) {

        int visibility = View.INVISIBLE;
        String strDuration = "";

        long duration = argFeedItem.getDuration();
        if (duration > 0) {
            strDuration = StrUtils.formatTime(duration);
            visibility = View.VISIBLE;
        }

        argHolder.mTimeDuration.setText(strDuration);
        argHolder.mTimeDurationIcon.setVisibility(visibility);
    }
}
