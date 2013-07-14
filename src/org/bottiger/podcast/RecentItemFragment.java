package org.bottiger.podcast;

import java.util.HashMap;

import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.adapters.viewholders.InlinePlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.PlaylistCursorLoader;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.ExpandAnimation;
import org.bottiger.podcast.utils.StrUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class RecentItemFragment extends DSLVFragment {

	private static boolean updateProgressbar = true;
	
	private static int CONTEXT_MENU = 0;
	private static Fragment CONTEXT_FRAGMENT = null;

	public final static int PLAYLIST_CONTEXT_MENU = 0;
	public final static int SUBSCRIPTION_CONTEXT_MENU = 1;

	public static HashMap<Integer, Integer> mKeepIconMap;

	private Playlist mPlaylist;

	/** ID of the current expanded episode */
	private long mExpandedEpisodeId = -1;
	private String mExpandedEpisodeKey = "currentExpanded";
	
	boolean mDualPane;

	public static RecentItemFragment newInstance(int headers, int footers) {
		RecentItemFragment f = new RecentItemFragment();

		Bundle args = new Bundle();
		args.putInt("headers", headers);
		args.putInt("footers", footers);
		f.setArguments(args);

		return f;
	}

	// Read here:
	// http://developer.android.com/reference/android/app/Fragment.html#Layout
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mPlaylist = new Playlist(getActivity());
		mAdapter = this.getAdapter(mCursor);
		new PlaylistCursorLoader(mPlaylist, this, mAdapter);
		setListAdapter(mAdapter);

		if (savedInstanceState != null) {
			// Restore last state for checked position.
			mExpandedEpisodeId = savedInstanceState.getLong(mExpandedEpisodeKey, mExpandedEpisodeId);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(mExpandedEpisodeKey, mExpandedEpisodeId);
	}

	public void setAdapter(CursorAdapter adapter) {
		mAdapter = adapter;
	}

	public CursorAdapter getAdapter() {
		return mAdapter;
	}

	public CursorAdapter getAdapter(Cursor cursor) {
		if (mAdapter != null)
			return mAdapter;

		CursorAdapter adapter = createAdapter(this.getActivity(), this, cursor);
		setAdapter(adapter);
		return adapter;
	}

	public static CursorAdapter createAdapter(Context context,
			PodcastBaseFragment fragment, Cursor cursor) {
		return new ItemCursorAdapter(context, fragment, R.layout.episode_list,
				cursor, new String[] { ItemColumns.TITLE,
						ItemColumns.SUB_TITLE, ItemColumns.DURATION,
						ItemColumns.IMAGE_URL }, new int[] { R.id.title,
						R.id.podcast, R.id.duration, R.id.list_image });
	}

	@Override
	public void onResume() {
		super.onResume();
		
		queueNextRefresh();
		
		ControlButtons.fragment = this;

		if (MainActivity.SHOW_PULL_TO_REFRESH) {
			// Delegate OnTouch calls to both libraries that want to receive
			// them
			getListView().setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					if (!mController.onTouch(view, motionEvent)) {
						// Only allow pull to refresh if not swiping list item
						getPullToRefreshAttacher().onTouch(view, motionEvent);
					}
					return false;
				}
			});
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor item = (Cursor) l.getItemAtPosition(position); // https://github.com/chrisbanes/Android-PullToRefresh/issues/99

		this.togglePlayer(l, item);

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.podcast_context, menu);
		RecentItemFragment.setContextMenu(PLAYLIST_CONTEXT_MENU, this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (!AdapterView.AdapterContextMenuInfo.class.isInstance(item
				.getMenuInfo()))
			return false;

		if (CONTEXT_MENU == PLAYLIST_CONTEXT_MENU) {
			return playlistContextMenu(item);
		} else if (CONTEXT_MENU == SUBSCRIPTION_CONTEXT_MENU) {
			if (CONTEXT_FRAGMENT != null)
				return ((SubscriptionsFragment) CONTEXT_FRAGMENT)
						.subscriptionContextMenu(item);
		}

		return false;

	}

	public boolean playlistContextMenu(MenuItem item) {
		AdapterView.AdapterContextMenuInfo cmi = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(cmi.position);
		FeedItem episode = FeedItem.getByCursor(cursor);
		Subscription subscription = Subscription.getById(getActivity()
				.getContentResolver(), episode.sub_id);

		switch (item.getItemId()) {
		case R.id.unsubscribe:
			subscription.unsubscribe(getActivity());
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * Toggles the inline player for an episode when clicked on
	 * 
	 * @param list
	 * @param item
	 */
	private void togglePlayer(ListView list, Cursor item) {
		int start = list.getFirstVisiblePosition();
		boolean setListners = false;

		((ItemCursorAdapter) mAdapter).toggleItem(item);
		long id = item.getLong(item.getColumnIndex(BaseColumns._ID));
		String duration = item.getString(item
				.getColumnIndex(ItemColumns.DURATION));
		int position = item.getPosition();

		View view = list.getChildAt(position - start);
		// mAdapter.notifyDataSetChanged();

		InlinePlayer viewHolder = (position == 0) ? InlinePlayer
				.getCurrentEpisodePlayerViewHolder() : InlinePlayer
				.getSecondaryEpisodePlayerViewHolder();

		ViewStub stub = (ViewStub) view.findViewById(R.id.stub);
		if (stub != null) {
			stub.inflate();
			ExpandAnimation expandAni = new ExpandAnimation(stub, 5000);
			stub.startAnimation(expandAni);

			setListners = true;
		} else {
			View player = view.findViewById(R.id.stub_player);
			if (player.getVisibility() == View.VISIBLE) {
				player.setVisibility(View.GONE);
				mExpandedEpisodeId = -1;
			} else {
				player.setVisibility(View.VISIBLE);
				setListners = true;
			}
		}

		// Is this even possible to be true?
		if (viewHolder == null)
			viewHolder = InlinePlayer.getViewHolder(view);

		if (viewHolder.duration != null)
			viewHolder.duration.setText(duration);

		ControlButtons.setPlayerListeners(viewHolder, this, id);
		updatePlayingView();
	}

	@Override
	public void setListAdapter() {
		// TODO Auto-generated method stub
		CursorAdapter sca = getAdapter(getCursor());
		setListAdapter(sca);
	}

	@Override
	View getPullView() {
		return getListView();
	}

	public static void setContextMenu(int menu, Fragment fragment) {
		CONTEXT_MENU = menu;
		CONTEXT_FRAGMENT = fragment;
	}

	public static int getContextMenu() {
		return CONTEXT_MENU;
	}
	
	public static boolean isUpdateProgressbar() {
		return updateProgressbar;
	}

	public static void setUpdateProgressbar(boolean updateProgressbar) {
		RecentItemFragment.updateProgressbar = updateProgressbar;
	}

	/**
	 * Handler for updating the UI
	 */
	/*
	 * abstract public void startInit();
	 */
	public static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				long next = refreshUI();
				queueNextRefresh();
				break;
			}
		}
	};

	public static void queueNextRefresh() {
		long delay = 3;
		Message msg = mHandler.obtainMessage(REFRESH);
		mHandler.removeMessages(REFRESH);
		mHandler.sendMessageDelayed(msg, delay);
	}
	
	protected static long refreshUI() {
		long refresh_time = 500;

		if (mPlayerServiceBinder == null)
			return refresh_time;

		if (mPlayerServiceBinder.isPlaying()) {
			updatePlayingView();
		}

		return refresh_time;
	}
	
	protected static void updatePlayingView() {
		InlinePlayer player = InlinePlayer
		.getCurrentEpisodePlayerViewHolder();
		
		
		//TextView firstVisibleCurrentTime = getListView()
		
		if (player.currentTime != null) {
			long pos = mPlayerServiceBinder.position();
			long duration = mPlayerServiceBinder.duration();
			
			String timeCounter = StrUtils.formatTime(pos);
			String durationString = StrUtils.formatTime(duration);
			player.currentTime.setText(timeCounter);
			if (player.duration != null)
				player.duration.setText(durationString);

			if (RecentItemFragment.isUpdateProgressbar() && player.seekbar != null)
				setProgressBar(player.seekbar,
						mPlayerServiceBinder);
		}
	}
	
	private static void setProgressBar(SeekBar progressBar, PlayerService playerService) {
		FeedItem item = playerService.getCurrentItem();
		long duration = playerService.duration();
		long position = playerService != null ? playerService.position() : 0;
		long secondary;

		// FIXME - just added this check to avoid a crash
		if (item != null) {
			if (item.isDownloaded())
				secondary = item.getCurrentFileSize();
			else
				secondary = (playerService.bufferProgress() * duration) / 100;
			setProgressBar(progressBar, duration, position, secondary);
		}
	}

	@Deprecated
	private static void setProgressBar(SeekBar progressBar, FeedItem item) {
		if (item.getCurrentFileSize() == 0)
			return;
		long secondary = item.isDownloaded() ? item.getCurrentFileSize()
				: (item.chunkFilesize / item.filesize);
		long duration = item.getDuration();
		setProgressBar(progressBar, duration, item.offset, secondary);
	}

	/**
	 * duration, progress, secondary should all be in units of ms
	 */
	public static void setProgressBar(SeekBar progressBar, long duration,
			long progress, long secondary) {
		if (duration == 0)
			return;
		int progressMax = progressBar.getMax();
		int primaryProgress = (int) ((progressMax * progress) / duration);
		int secondaryProgress = (int) ((progressMax * secondary) / duration);
		progressBar.setProgress(primaryProgress);
		progressBar.setSecondaryProgress(secondaryProgress);
	}
}
