package com.ironsource.ironsourcesdkdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.adqualitysdk.sdk.ISAdQualityConfig;
import com.ironsource.adqualitysdk.sdk.ISAdQualityLogLevel;
import com.ironsource.adqualitysdk.sdk.IronSourceAdQuality;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.IronSourceSegment;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.BannerListener;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.ironsource.mediationsdk.sdk.OfferwallListener;
import com.ironsource.mediationsdk.sdk.RewardedVideoListener;
import com.ironsource.mediationsdk.sdk.SegmentListener;
import com.ironsource.mediationsdk.utils.IronSourceUtils;


public class DemoActivity extends Activity implements OfferwallListener, ImpressionDataListener, SegmentListener {

    private final String TAG = "DemoActivity";
    private final String APP_KEY = "1648056c5";
    //private final String APP_KEY = "375e323d-c391-431d-b85e-6cfb1cde3be1";
    //private final String APP_KEY = "dab30209"
    //private final String APP_KEY = "gibberish";

    private final String FALLBACK_USER_ID = "userId";
    private final String TEST_UID = "druTest";
    private final String TEST_UID_NEW = "druTestNew";
    //private final String TEST_SEGMENT = "druTestSegment";
    private Button mVideoButton;
    private Button mOfferwallButton;
    private Button mInterstitialLoadButton;
    private Button mInterstitialShowButton;
    private Button mUIDButton;
    private Button mSegmentButton;
    private Button mTestSuiteButton;

    private Placement mPlacement;

    private FrameLayout mBannerParentLayout;
    private IronSourceBannerLayout mIronSourceBannerLayout;
    private IronSourceSegment mFreeSegment;
    private IronSourceSegment mPayingSegment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        //The integrationHelper is used to validate the integration. Remove the integrationHelper before going live!
        IntegrationHelper.validateIntegration(this);
        initUIElements();
        startIronSourceInitTask();
        IronSource.getAdvertiserId(this);
        //Network Connectivity Status
        IronSource.shouldTrackNetworkState(this, true);

        // Get the Ad Quality appKey from the App's Settings page.
        //String adQualityAppKey = "375e323d-c391-431d-b85e-6cfb1cde3be1";
        // Initialize
        //IronSourceAdQuality.getInstance().initialize(this, APP_KEY);
        ISAdQualityConfig.Builder adQualityConfigBuilder = new ISAdQualityConfig.Builder();
        // There are 5 different log levels:
        // ERROR, WARNING, INFO, DEBUG, VERBOSE
        // The default is INFO
        adQualityConfigBuilder.setLogLevel(ISAdQualityLogLevel.VERBOSE);
        adQualityConfigBuilder.setUserId(TEST_UID);
        // The default user id is Ad Quality internal id.
        // The only allowed characters for user id are: letters, numbers, @, -, :, =, _ and /.
        // The user id cannot be null and must be between 2 and 100 characters, otherwise it will be blocked.
        ISAdQualityConfig adQualityConfig = adQualityConfigBuilder.build();
        IronSourceAdQuality.getInstance().initialize(this, APP_KEY, adQualityConfig);

        Log.d("ironSource_AQSDKVersion", "Using the following AdQuality version: " + IronSourceAdQuality.getSDKVersion());
    }
    private void startIronSourceInitTask(){
        String advertisingId = IronSource.getAdvertiserId(DemoActivity.this);

        // Regulation Settings
        IronSource.setMetaData("is_test_suite", "enable");

        // we're using an advertisingId as the 'userId'
        initIronSource(APP_KEY, advertisingId);
        Log.d("USERID",advertisingId);
    }

    private void initIronSource(String appKey, String userId) {
        // Be sure to set a listener to each product that is being initiated
        // set the IronSource offerwall listener
        IronSource.setOfferwallListener(this);
        // set client side callbacks for the offerwall
        SupersonicConfig.getConfigObj().setClientSideCallbacks(true);
        // add the Impression Data listener
        IronSource.addImpressionDataListener(this);
        // add the Segment Listener
        IronSource.setSegmentListener(this);
        // Set AdInfo Listeners
        setRVListeners();
        setInterstitialListeners();

        // set the IronSource user id
        IronSource.setUserId(userId);

        // set IronSource segment
        mPayingSegment = new IronSourceSegment();
        mFreeSegment = new IronSourceSegment();
        //mFreeSegment.setSegmentName("NonPayingUser");
        mFreeSegment.setIsPaying(false);
        mPayingSegment.setIsPaying(true);
        IronSource.setSegment(mFreeSegment);
        Log.i("mIronSegment","Initialized as non-paying user");

        // init the IronSource SDK
        IronSource.init(this, appKey);

        updateButtonsState();

        // In order to work with IronSourceBanners you need to add Providers who support banner ad unit and uncomment next line
         createAndloadBanner();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // call the IronSource onResume method
        IronSource.onResume(this);
        updateButtonsState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // call the IronSource onPause method
        IronSource.onPause(this);
        updateButtonsState();
    }


    /**
     * Handle the button state according to the status of the IronSource producs
     */
    private void updateButtonsState() {
            handleVideoButtonState(IronSource.isRewardedVideoAvailable());
            handleOfferwallButtonState(IronSource.isOfferwallAvailable());
            handleLoadInterstitialButtonState(true);
            handleInterstitialShowButtonState(false);

    }



    /**
     * initialize the UI elements of the activity
     */
    private void initUIElements() {
        mSegmentButton = (Button) findViewById(R.id.segment_button);
        mSegmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mPayingSegment.setSegmentName("PayingUser");
                IronSource.setSegment(mPayingSegment);
                Log.i("mIronSegment","Pressed button to change to paying user");
            }
        });

        mUIDButton = (Button) findViewById(R.id.uid_button);
        mUIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IronSourceAdQuality.getInstance().changeUserId(TEST_UID_NEW);
            }
        });

        mVideoButton = (Button) findViewById(R.id.rv_button);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    // check if video is available
                    if (IronSource.isRewardedVideoAvailable())
                        //show rewarded video
                        IronSource.showRewardedVideo("needlive_test");
            }
        });

        mOfferwallButton = (Button) findViewById(R.id.ow_button);
        mOfferwallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    //show the offerwall
                    if (IronSource.isOfferwallAvailable())
                        IronSource.showOfferwall();
            }
        });

        mInterstitialLoadButton = (Button) findViewById(R.id.is_button_1);
        mInterstitialLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IronSource.loadInterstitial();
            }
        });


        mInterstitialShowButton = (Button) findViewById(R.id.is_button_2);
        mInterstitialShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    // check if interstitial is available
                    if (IronSource.isInterstitialReady()) {
                        //show the interstitial
                        IronSource.showInterstitial();
                }
            }
        });

        mTestSuiteButton = (Button) findViewById(R.id.test_suite);
        mTestSuiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IronSource.launchTestSuite(DemoActivity.this);
            }
        });

        TextView versionTV = (TextView) findViewById(R.id.version_txt);
        versionTV.setText(getResources().getString(R.string.version) + " " + IronSourceUtils.getSDKVersion());

        mBannerParentLayout = (FrameLayout) findViewById(R.id.banner_footer);
    }


    /**
     * Creates and loads IronSource Banner
     *
     */
    private void createAndloadBanner() {
        // choose banner size
        ISBannerSize size = ISBannerSize.BANNER;

        // instantiate IronSourceBanner object, using the IronSource.createBanner API
        mIronSourceBannerLayout = IronSource.createBanner(this, size);

        // add IronSourceBanner to your container
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mBannerParentLayout.addView(mIronSourceBannerLayout, 0, layoutParams);

        if (mIronSourceBannerLayout != null) {
            // set the banner listener
            mIronSourceBannerLayout.setLevelPlayBannerListener(new LevelPlayBannerListener() {
                // Invoked each time a banner was loaded. Either on refresh, or manual load.
                //  AdInfo parameter includes information about the loaded ad
                @Override
                public void onAdLoaded(AdInfo adInfo) {
                    Log.d(TAG, "onAdLoaded" + " Network: " + adInfo.getAdNetwork() + " Instance: " + adInfo.getInstanceId());
                    mBannerParentLayout.setVisibility(View.VISIBLE);
                }
                // Invoked when the banner loading process has failed.
                //  This callback will be sent both for manual load and refreshed banner failures.
                @Override
                public void onAdLoadFailed(IronSourceError error) {}
                // Invoked when end user clicks on the banner ad
                @Override
                public void onAdClicked(AdInfo adInfo) {}
                // Notifies the presentation of a full screen content following user click
                @Override
                public void onAdScreenPresented(AdInfo adInfo) {}
                // Notifies the presented screen has been dismissed
                @Override
                public void onAdScreenDismissed(AdInfo adInfo) {}
                //Invoked when the user left the app
                @Override
                public void onAdLeftApplication(AdInfo adInfo) {}

            });

            // load ad into the created banner
            IronSource.loadBanner(mIronSourceBannerLayout);
        } else {
            Toast.makeText(DemoActivity.this, "IronSource.createBanner returned null", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Destroys IronSource Banner and removes it from the container
     *
     */
    private void destroyAndDetachBanner() {
        IronSource.destroyBanner(mIronSourceBannerLayout);
        if (mBannerParentLayout != null) {
            mBannerParentLayout.removeView(mIronSourceBannerLayout);
        }
    }

    /**
     * Set the Rewareded Video button state according to the product's state
     *
     * @param available if the video is available
     */
    public void handleVideoButtonState(final boolean available) {
        final String text;
        final int color;
        if (available) {
            color = Color.BLUE;
            text = getResources().getString(R.string.show) + " " + getResources().getString(R.string.rv);
        } else {
            color = Color.BLACK;
            text = getResources().getString(R.string.initializing) + " " + getResources().getString(R.string.rv);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoButton.setTextColor(color);
                mVideoButton.setText(text);
                mVideoButton.setEnabled(available);

            }
        });
    }

    /**
     * Set the Rewareded Video button state according to the product's state
     *
     * @param available if the offerwall is available
     */
    public void handleOfferwallButtonState(final boolean available) {
        final String text;
        final int color;
        if (available) {
            color = Color.BLUE;
            text = getResources().getString(R.string.show) + " " + getResources().getString(R.string.ow);
        } else {
            color = Color.BLACK;
            text = getResources().getString(R.string.initializing) + " " + getResources().getString(R.string.ow);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOfferwallButton.setTextColor(color);
                mOfferwallButton.setText(text);
                mOfferwallButton.setEnabled(available);

            }
        });

    }

    /**
     * Set the Interstitial button state according to the product's state
     *
     * @param available if the interstitial is available
     */
    public void handleLoadInterstitialButtonState(final boolean available) {
        Log.d(TAG, "handleInterstitialButtonState | available: " + available);
        final String text;
        final int color;
        if (available) {
            color = Color.BLUE;
            text = getResources().getString(R.string.load) + " " + getResources().getString(R.string.is);
        } else {
            color = Color.BLACK;
            text = getResources().getString(R.string.initializing) + " " + getResources().getString(R.string.is);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInterstitialLoadButton.setTextColor(color);
                mInterstitialLoadButton.setText(text);
                mInterstitialLoadButton.setEnabled(available);
            }
        });

    }

    /**
     * Set the Show Interstitial button state according to the product's state
     *
     * @param available if the interstitial is available
     */
    public void handleInterstitialShowButtonState(final boolean available) {
        final int color;
        if (available) {
            color = Color.BLUE;
        } else {
            color = Color.BLACK;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInterstitialShowButton.setTextColor(color);
                mInterstitialShowButton.setEnabled(available);
            }
        });
    }




    // --------- IronSource Segment Listener ---------
    @Override
    public void onSegmentReceived(String segment){
        Log.d(TAG,"Registered the following segment: " + segment);
    }



    // --------- IronSource Rewarded Video Listener ---------
    public void setRVListeners() {
        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
            // Indicates that there's an available ad.
            // The adInfo object includes information about the ad that was loaded successfully
            // Use this callback instead of onRewardedVideoAvailabilityChanged(true)
            @Override
            public void onAdAvailable(AdInfo adInfo) {
                Log.d(TAG, "onAdAvailable" + " Network: " + adInfo.getAdNetwork() + " Instance: " + adInfo.getInstanceId());
                onVideoAvailabilityChanged(true);
            }
            // Indicates that no ads are available to be displayed
            // Use this callback instead of onRewardedVideoAvailabilityChanged(false)
            @Override
            public void onAdUnavailable() {
                onVideoAvailabilityChanged(false);
            }
            // The Rewarded Video ad view has opened. Your activity will loose focus
            @Override
            public void onAdOpened(AdInfo adInfo) {}
            // The Rewarded Video ad view is about to be closed. Your activity will regain its focus
            @Override
            public void onAdClosed(AdInfo adInfo) {}
            // The user completed to watch the video, and should be rewarded.
            // The placement parameter will include the reward data.
            // When using server-to-server callbacks, you may ignore this event and wait for the ironSource server callback
            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                mPlacement = placement;
            }
            // The rewarded video ad was failed to show
            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {}
            // Invoked when the video ad was clicked.
            // This callback is not supported by all networks, and we recommend using it
            // only if it's supported by all networks you included in your build
            @Override
            public void onAdClicked(Placement placement, AdInfo adInfo) {}
        });
    }
    public void onVideoAvailabilityChanged(boolean available){
        // called when the video availbility has changed
        Log.d(TAG, "onRewardedVideoAvailabilityChanged" + " " + available);
        handleVideoButtonState(available);
    }

    // --------- IronSource Offerwall Listener ---------

    @Override
    public void onOfferwallAvailable(boolean available) {
        handleOfferwallButtonState(available);
    }

    @Override
    public void onOfferwallOpened() {
        // called when the offerwall has opened
        Log.d(TAG, "onOfferwallOpened");
    }

    @Override
    public void onOfferwallShowFailed(IronSourceError ironSourceError) {
        // called when the offerwall failed to show
        // you can get the error data by accessing the IronSourceError object
         ironSourceError.getErrorCode();
         ironSourceError.getErrorMessage();
        Log.d(TAG, "onOfferwallShowFailed" + " " + ironSourceError);
    }

    @Override
    public boolean onOfferwallAdCredited(int credits, int totalCredits, boolean totalCreditsFlag) {
        Log.d(TAG, "onOfferwallAdCredited" + " credits:" + credits + " totalCredits:" + totalCredits + " totalCreditsFlag:" + totalCreditsFlag);
        return false;
    }

    @Override
    public void onGetOfferwallCreditsFailed(IronSourceError ironSourceError) {
        // you can get the error data by accessing the IronSourceError object
        // IronSourceError.getErrorCode();
        // IronSourceError.getErrorMessage();
        Log.d(TAG, "onGetOfferwallCreditsFailed" + " " + ironSourceError);
    }

    @Override
    public void onOfferwallClosed() {
        // called when the offerwall has closed
        Log.d(TAG, "onOfferwallClosed");
    }

    // --------- IronSource Interstitial Listener ---------
    public void setInterstitialListeners(){
        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {
            // Invoked when the interstitial ad was loaded successfully.
            // AdInfo parameter includes information about the loaded ad
            @Override
            public void onAdReady(AdInfo adInfo) {
                Log.d(TAG, "onAdReady" + " Network: " + adInfo.getAdNetwork() + " Instance: " + adInfo.getInstanceId());
                handleInterstitialShowButtonState(true);
            }
            // Indicates that the ad failed to be loaded
            @Override
            public void onAdLoadFailed(IronSourceError error) {
                handleInterstitialShowButtonState(false);
            }
            // Invoked when the Interstitial Ad Unit has opened, and user left the application screen.
            // This is the impression indication.
            @Override
            public void onAdOpened(AdInfo adInfo) {}
            // Invoked when the interstitial ad closed and the user went back to the application screen.
            @Override
            public void onAdClosed(AdInfo adInfo) {
                handleInterstitialShowButtonState(false);
            }
            // Invoked when the ad failed to show
            @Override
            public void onAdShowFailed(IronSourceError error, AdInfo adInfo) {
                handleInterstitialShowButtonState(false)  ;
            }
            // Invoked when end user clicked on the interstitial ad
            @Override
            public void onAdClicked(AdInfo adInfo) {}
            // Invoked before the interstitial ad was opened, and before the InterstitialOnAdOpenedEvent is reported.
            // This callback is not supported by all networks, and we recommend using it only if
            // it's supported by all networks you included in your build.
            @Override
            public void onAdShowSucceeded(AdInfo adInfo){}
        });
    }

    // --------- Impression Data Listener ---------
    @Override
    public void onImpressionSuccess(ImpressionData impressionData) {
        // The onImpressionSuccess will be reported when the rewarded video and interstitial ad is opened.
        // For banners, the impression is reported on load success.
        if (impressionData != null) {
            Log.d(TAG, "onImpressionSuccess " + impressionData);
        }
        }

    public void showRewardDialog(Placement placement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DemoActivity.this);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setTitle(getResources().getString(R.string.rewarded_dialog_header));
        builder.setMessage(getResources().getString(R.string.rewarded_dialog_message) + " " + placement.getRewardAmount() + " " + placement.getRewardName());
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }



}
