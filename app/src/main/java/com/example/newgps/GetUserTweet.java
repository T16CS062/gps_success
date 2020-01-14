package com.example.newgps;
import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

public class GetUserTweet extends AsyncTaskLoader<String>{

    // Twitterオブジェクト
    private Twitter twitter;

    public GetUserTweet(Context context, Twitter _twitter) {
        super(context);
        this.twitter = _twitter;
    }

    @Override
    public String loadInBackground() { // （1）

        try {

            User user = twitter.showUser("@balloon_chase");
            long id = user.getId();
            List tweetList = twitter.getUserTimeline(id);
            Status tweet = (Status) tweetList.get(0);
            return tweet.getText();

        } catch (TwitterException e) {
            Log.d("twitter", e.getMessage());
        }

        return null;
    }


}