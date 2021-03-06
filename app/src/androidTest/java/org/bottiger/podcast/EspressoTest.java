package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.test.ActivityInstrumentationTestCase2;

import org.hamcrest.Matchers;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;

/**
 * Created by apl on 20-05-2015.
 */
public class EspressoTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity mActivity;

    public EspressoTest() {
        super("org.bottiger.podcast", MainActivity.class);
    }

    public EspressoTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();
    }

    // https://github.com/googlesamples/android-testing/blob/master/downloads/espresso-cheat-sheet-2.1.0.pdf
    public void testWelcomeScreen() {
        // Validate initial state
        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.radioNone)).check(matches(isDisplayed()));
        onView(withId(R.id.radioAll)).check(matches(isDisplayed()));

        onView(withId(R.id.radioNone)).check(matches(isChecked()));
        onView(withId(R.id.radioAll)).check(matches(isNotChecked()));

        // Validate button click
        onView(withId(R.id.radioAll)).perform(click());

        onView(withId(R.id.radioNone)).check(matches(isNotChecked()));
        onView(withId(R.id.radioAll)).check(matches(isChecked()));
    }

    /**
     * Based on https://code.google.com/p/android-test-kit/source/browse/espresso/libtests/src/main/java/com/google/android/apps/common/testing/ui/espresso/action/SwipeActionIntegrationTest.java?r=c4e4da01ca8d0fab31129c87f525f6e9ba1ecc02
     */
    public void testSwipeToSubscriptionFragment() {
        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.playlist_empty_header))));

        onView(withId(R.id.playlist_welcome_screen))
                .perform(swipeLeft());

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.subscription_empty))));

        onView(withId(R.id.subscription_empty))
                .perform(swipeLeft());

        onView(withId(R.id.app_content))
                .check(matches(hasDescendant(withId(R.id.discovery_search_container))));

        // Go back to start
        onView(withId(R.id.app_content))
                .perform(swipeRight())
                .perform(swipeRight());

        onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));
    }


    public void testSubscribing() {
        //onView(withId(R.id.playlist_empty_header)).check(matches(isDisplayed()));

        // Go to the discovery fragmnt
        onView(withId(R.id.app_content))
                .perform(swipeRight())
                .perform(swipeRight());

        // Subscribe to a podcast

        /*
        onView(withId(R.id.search_result_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, click()));
        */
        //onView(withId(R.id.app_content)).check(matches(hasDescendant(withId(R.id.discovery_search_container))));
        //onView(withId(R.id.discovery_search_container)).check(matches(isDisplayed()));

        //anyOf(withId(R.id.discovery_search_container), withId(R.id.result_subscribe_switch))).perform(click());

        // Open the feed
        /*
        onView(withId(R.id.app_content))
                .perform(swipeLeft());

        // Verify the podcast is there
        onView(withId(R.id.grid_title)).check(matches(isDisplayed()));

        // open the activity
        onView(withId(R.id.grid_title)).perform(click());
        */
    }

}
