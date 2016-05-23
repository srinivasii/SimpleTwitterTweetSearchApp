package com.srinivas.twitter.simpletwittertweetsearchapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.srinivas.twitter.simpletwittertweetsearchapp.R;
import com.srinivas.twitter.simpletwittertweetsearchapp.util.ConnectionDetector;
import com.srinivas.twitter.simpletwittertweetsearchapp.util.Constants;
import com.srinivas.twitter.simpletwittertweetsearchapp.util.Dialog;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginIntoTwitterActivity extends AppCompatActivity {


    private String LOG_TAB = "twitter";
    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";

    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    // Login button
    Button mBTNLoginTwitter;
    // Update status button

    // Twitter
    private static TwitterFactory factory;

    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Internet Connection detector
    private ConnectionDetector cd;

    // Alert Dialog Manager
    Dialog alert = new Dialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_twitter_view);

        Log.v(LOG_TAB,"onCreate");
        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(LoginIntoTwitterActivity.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // Check if twitter keys are set
        if(Constants.TWITTER_CONSUMER_KEY.trim().length() == 0 || Constants.TWITTER_CONSUMER_SECRET.trim().length() == 0){
            // Internet Connection is not present
            alert.showAlertDialog(LoginIntoTwitterActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
            // stop executing code by return
            return;
        }

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();
        factory = new TwitterFactory(configuration);

        // All UI elements
        mBTNLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);

        /**
         * Twitter login button click event will call loginToTwitter() function
         * */
        mBTNLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login twitter function
                loginToTwitter();
            }
        });

        /** This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */

        if ( !isTwitterLoggedInAlready() ) {
            Uri uri = getIntent().getData();

            if ( uri != null && uri.toString().startsWith( TWITTER_CALLBACK_URL ) ) {
                String verifier = uri.getQueryParameter( URL_TWITTER_OAUTH_VERIFIER );

                AccessTokenAsyncTask accessTokenAsyncTask = new AccessTokenAsyncTask( LoginIntoTwitterActivity.this );
                accessTokenAsyncTask.execute( verifier );
            }
        } else {

            finish();
            startActivity( new Intent( getApplicationContext(), SearchTweetActivity.class ) );
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
    }

    protected void loginToTwitter() {
        LoginAsyncTask loginAsyncTask = new LoginAsyncTask(LoginIntoTwitterActivity.this);
        loginAsyncTask.execute();
    }


    // LoginAsyncTask
    private static class LoginAsyncTask extends AsyncTask<Void, Void, RequestToken> {
        private LoginIntoTwitterActivity mActivity;

        public LoginAsyncTask(LoginIntoTwitterActivity activity) {
            attach(activity);
        }

        public void detach() {
            mActivity = null;
        }

        public void attach(LoginIntoTwitterActivity activity) {
            mActivity = activity;
        }

        protected RequestToken doInBackground(Void... param) {

            Log.v(mActivity.LOG_TAB,"doInBackground "+param);

            Twitter twitter = factory.getInstance();

            RequestToken requestToken = null;
            try {
                requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
            } catch (TwitterException e) {

                Log.v(mActivity.LOG_TAB,"exception "+ e.toString());
                e.printStackTrace();
            }

            return requestToken;
        }


        protected void onPostExecute(RequestToken result) {

            Log.v(mActivity.LOG_TAB,"onPostExecute "+result);

            if (result != null) {
                requestToken = result;

                mActivity.startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( requestToken.getAuthenticationURL() ) ) );
            }
        }

    }

    // AccessTokenAsyncTask
    private static class AccessTokenAsyncTask extends AsyncTask<String, Void, AccessToken> {
        private LoginIntoTwitterActivity mActivity;

        public AccessTokenAsyncTask(LoginIntoTwitterActivity activity) {
            attach(activity);
        }

        public void detach() {
            mActivity = null;
        }

        public void attach(LoginIntoTwitterActivity activity) {
            mActivity = activity;
        }

        protected AccessToken doInBackground(String... param) {
            AccessToken accessToken = null;

            Twitter twitter = factory.getInstance();

            try {
                accessToken = twitter.getOAuthAccessToken( requestToken, param[0] );
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return accessToken;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        protected void onPostExecute(AccessToken result) {

            Log.v(mActivity.LOG_TAB,"Login onPostExecute "+result);

            if (result != null) {
                AccessToken accessToken = result;

                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putString(Constants.PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                e.putString(Constants.PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
                e.commit();

                mActivity.finish();
                mActivity.startActivity( new Intent( mActivity.getApplicationContext(), SearchTweetActivity.class ) );
            }
        }

    }


    protected void logoutToTwitter() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.remove(PREF_KEY_TWITTER_LOGIN);
        editor.remove(Constants.PREF_KEY_OAUTH_TOKEN);
        editor.remove(Constants.PREF_KEY_OAUTH_SECRET);

        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        logoutToTwitter();

        super.onDestroy();
    }
}
