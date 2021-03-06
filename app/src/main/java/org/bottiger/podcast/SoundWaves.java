package org.bottiger.podcast;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;


import com.facebook.drawee.backends.pipeline.Fresco;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.CrashReporterFactory;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;

//Acra debugging
@ReportsCrashes(
        // not used
        formUri = "https://acra.bottiger.org/acra-soundwaves/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = ApplicationConfiguration.formUriBasicAuthLogin, // optional
        formUriBasicAuthPassword = ApplicationConfiguration.formUriBasicAuthPassword, // optional
        disableSSLCertValidation = true,
        mode = ReportingInteractionMode.DIALOG,
        forceCloseDialogAfterToast=true,
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        socketTimeout = 10000)
public class SoundWaves extends Application {

    private static Context context;
    // Global constants
    private Boolean mFirstRun = null;

    public static IAnalytics sAnalytics;
    public static SubscriptionRefreshManager sSubscriptionRefreshManager;

    private static Bus sBus = new Bus(ThreadEnforcer.MAIN);


    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG) { //  || System.currentTimeMillis() > 0
            // ACRA - crash reporter
            CrashReporterFactory.startReporter(this);

            // ANR
            //new ANRWatchDog(10000 /*timeout*/).start();
        }

        Fresco.initialize(this);

        sAnalytics = AnalyticsFactory.getAnalytics(this);
        sAnalytics.startTracking();

        context = getApplicationContext();

        sSubscriptionRefreshManager = new SubscriptionRefreshManager(context);

        firstRun(context);
    }

    public static Context getAppContext() {
        return context;
    }

    private void firstRun(@NonNull Context argContext) {
        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String key = getString(R.string.preference_first_run_key);
        boolean firstRun = sharedPref.getBoolean(key, true);
        if (firstRun) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, false);
            editor.commit();
        }
        mFirstRun = firstRun;
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun.booleanValue();
    }

    public static Bus getBus() {
        return sBus;
    }
}
