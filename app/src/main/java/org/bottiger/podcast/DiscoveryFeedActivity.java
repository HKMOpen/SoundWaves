package org.bottiger.podcast;

import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import org.bottiger.podcast.adapters.DiscoverySearchAdapter;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PaletteHelper;

import java.net.URL;

/**
 * Created by apl on 23-04-2015.
 */
public class DiscoveryFeedActivity extends FeedActivity {

    private static final String TAG = "DiscoveryFeedActivity";

    protected com.rey.material.widget.Button mSubscribeButton;
    protected View mSubscribeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Starting DiscoveryFeedActivity"); // NoI18N
        super.onCreate(savedInstanceState);
        PaletteHelper.generate(mSubscription.getImageURL(), this, mFloatingButton);

        mSubscribeContainer = findViewById(R.id.feed_subscribe_layout);
        mSubscribeButton = (com.rey.material.widget.Button) findViewById(R.id.feed_subscribe_button);

        mSubscribeContainer.setVisibility(View.VISIBLE);

        mSubscribeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                URL url = mSubscription.getURL();
                Subscription subscription = new Subscription(url.toString());
                subscription.subscribe(DiscoveryFeedActivity.this);

                // if (Build.VERSION.SDK_INT >= 19) {
                //TransitionManager.beginDelayedTransition(mSubscribeContainer);
                mSubscribeContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
