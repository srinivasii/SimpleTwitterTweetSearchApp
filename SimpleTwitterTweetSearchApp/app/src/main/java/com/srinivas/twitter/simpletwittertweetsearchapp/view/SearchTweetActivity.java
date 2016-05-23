package com.srinivas.twitter.simpletwittertweetsearchapp.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;
import com.srinivas.twitter.simpletwittertweetsearchapp.R;
import com.srinivas.twitter.simpletwittertweetsearchapp.util.Constants;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SearchTweetActivity extends AppCompatActivity {

    private String LOG_TAB = "twitter";
    // Progress dialog
    private ProgressDialog pDialog;
    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    private EditText mTXTSearch;
    private ListView mListView;

    List<Status> mLSTTweets;
    List<Status> mLSTTop3Tweets = new ArrayList<Status>();
    ArrayList<String> arryNames = new ArrayList<String>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_tweets_view);

        // Shared Preferences
        mSharedPreferences = getApplicationContext().getSharedPreferences(
                "MyPref", 0);

        mTXTSearch = (EditText)findViewById(R.id.showtweet_TXT_search);
        mListView = (ListView)findViewById(R.id.showtweet_LST_search);

        ((ImageView)findViewById(R.id.showtweet_IMG_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mTXTSearch.getText().toString().length() == 0){

                }
                else {

                    SearchTweets searchTweets =new SearchTweets();
                    searchTweets.execute(mTXTSearch.getText().toString());
                }
            }
        });

        ((Button)findViewById(R.id.showtweet_BTN_top3tweets)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(arryNames.size() > 0){

                   List<Status> list = findTop3Occurences(arryNames);

                    Log.v(LOG_TAB,""+list.size());

                    if(list.size()>0) {

                        TweetsDataAdapter tweetsDataAdapter = new TweetsDataAdapter(SearchTweetActivity.this, list);
                        mListView.setAdapter(tweetsDataAdapter);
                    }
                }

            }
        });

    }

    /**
     * Function to update status
     */
    class SearchTweets extends AsyncTask<String, List<Status>, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(SearchTweetActivity.this);
            pDialog.setMessage("getting tweets...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         */
        protected String doInBackground(String... args) {

            String searchQuery = args[0];
            // Access Token
            String access_token = mSharedPreferences.getString(Constants.PREF_KEY_OAUTH_TOKEN, "");
            // Access Token Secret
            String access_token_secret = mSharedPreferences.getString(Constants.PREF_KEY_OAUTH_SECRET, "");

            Log.v(LOG_TAB,"showTweetActivity access_token "+access_token+" access_token_secret"+access_token_secret);

            try {

                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
                Configuration configuration = builder.build();
                TwitterFactory factory = new TwitterFactory(configuration);

                Twitter twitter = factory.getInstance((new AccessToken(access_token, access_token_secret)));
                Query query = new Query(searchQuery);
                query.setCount(120);
                if (searchQuery != null && !searchQuery.isEmpty()) {

                    QueryResult rslt = twitter.search(query);
                    mLSTTweets = rslt.getTweets();
                }
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI
         **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI

            TweetsDataAdapter tweetsDataAdapter = new TweetsDataAdapter(SearchTweetActivity.this, mLSTTweets);
            mListView.setAdapter(tweetsDataAdapter);

        }
    }


    public class TweetsDataAdapter extends BaseAdapter {
        List<Status> listData = null;
        Context context;


        public TweetsDataAdapter(Context context, List<Status> listData) {
            this.listData = listData;
            this.context = context;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.listview_custom_text, null);
            }

            Status status = listData.get(position);

            SmartImageView imageAvatar =(SmartImageView)convertView.findViewById(R.id.listview_IMG_avatar);
            TextView txtUsername = (TextView) convertView.findViewById(R.id.listview_LBL_username);
            TextView txtTweet = (TextView) convertView.findViewById(R.id.listview_LBL_message);
            String name = status.getUser().getName();
            arryNames.add(name);
            txtUsername.setText(name);
            txtTweet.setText(status.getText());

            String imageData = status.getUser().getProfileImageURL();
            Log.v(LOG_TAB,"Image URL : "+imageData);

            imageAvatar.setImageUrl(imageData);

            return convertView;
        }
    }

    public List<Status> findTop3Occurences(ArrayList<String> list){

        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String temp : list) {
            Integer count = map.get(temp);
            map.put(temp, (count == null) ? 1 : count + 1);
        }

        Map<String, Integer> treeMap = new TreeMap<String, Integer>(map);
        List<Status> top3Tweets = filter(treeMap);

        return top3Tweets;
    }

    public List<Status> filter(Map<String, Integer> map) {

        mLSTTop3Tweets.clear();
        ArrayList<String> sortedArraylist = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            sortedArraylist.add(entry.getKey());
        }
        int j =0;
        while (j<3) {
            for (int i = 0; i < mLSTTweets.size(); i++) {

                if (mLSTTweets.get(i).getUser().getName().equals(sortedArraylist.get(j))) {

                    mLSTTop3Tweets.add(mLSTTweets.get(i));
                }
            }
            j++;
        }

        return mLSTTop3Tweets;
    }
}
