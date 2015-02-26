package org.bottiger.podcast.utils.navdrawer;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.TransitionUtils;

import java.util.LinkedList;

/**
 * Created by apl on 25-02-2015.
 */
public class NavigationDrawerMenuGenerator {

    private final LinkedList<NavItem> mItems = new LinkedList<>();

    private Activity mActivity;

    public NavigationDrawerMenuGenerator(@NonNull Activity argContext) {
        mActivity = argContext;

        mItems.add(new NavItem());
        mItems.add(new NavItem(R.string.menu_settings, R.drawable.ic_settings_grey, new INavOnClick() {
            @Override
            public void onClick() {
                TransitionUtils.openSettings(mActivity);
            }
        }));
        mItems.add(new NavItem());
        mItems.add(new NavItem(R.string.menu_feedback, R.drawable.ic_messenger_grey, new INavOnClick() {
            @Override
            public void onClick() {
                return;
            }
        }));
        mItems.add(new NavItem());
    }

    public void generate(@NonNull ViewGroup argContainer) {
        for(NavItem item : mItems)
            generateView(item, argContainer);
    }

    private void generateView(@NonNull NavItem argItem, @NonNull ViewGroup argContainer) {
        int layoutResource = argItem.getLayout();
        View view = mActivity.getLayoutInflater().inflate(layoutResource, argContainer, false);

        argItem.bindToView(view);

        argContainer.addView(view);
    }
}