package com.bayapps.android.robophish.model;

import android.support.v4.media.MediaMetadataCompat;
import com.bayapps.android.robophish.utils.LogHelper;
import com.loopj.android.http.*;
import org.json.*;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by mikevoyt on 7/27/16.
 */

public class PhishProviderSource implements MusicProviderSource  {

    private static final String TAG = LogHelper.makeLogTag(PhishProviderSource.class);

    @Override
    public ArrayList<String> years() {

        final ArrayList<String> years = new ArrayList<>();

        HttpClient.get("years.json", null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ArrayList<String> yearStrings = ParseUtils.parseYears(response);

                if (yearStrings != null) {
                    for (String year : yearStrings) {

                        LogHelper.d(TAG, "year: ", year);

                        years.add(year);
                    }
                }
            }
        });

        return years;
    }

    public Iterable<MediaMetadataCompat> showsInYear(String year) {
        final ArrayList<MediaMetadataCompat> shows = new ArrayList<>();

        HttpClient.get("years/" + year + ".json", null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                ArrayList<Show> showsArray = ParseUtils.parseShows(response);

                if (showsArray != null) {
                    for (Show show: showsArray) {
                        LogHelper.w(TAG, "date: ", show.getDateSimple());
                        LogHelper.w(TAG, "venue: ", show.getVenueName());

                        String id = String.valueOf(show.getId());

                        shows.add(new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                                .putString(MediaMetadataCompat.METADATA_KEY_DATE, show.getDateSimple())
                                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, show.getVenueName())
                                .build());
                    }
                }
            }
        });

        return shows;
    }


    public Iterable<MediaMetadataCompat> tracksInShow(final String showId) {

        final ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        HttpClient.get("shows/" + showId + ".json", null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Show show = ParseUtils.parseShow(response);

                if (show != null) {
                    for (Track track: show.getTracks()) {
                        LogHelper.d(TAG, "name: ", track.getTitle());

                        String id = String.valueOf(track.getId());

                        // Adding the music source to the MediaMetadata (and consequently using it in the
                        // mediaSession.setMetadata) is not a good idea for a real world music app, because
                        // the session metadata can be accessed by notification listeners. This is done in this
                        // sample for convenience only.
                        //noinspection ResourceType
                        tracks.add(new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, track.getUrl())
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getDuration())
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
                                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, show.getVenueName())
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getDurationString())
                                .putString(MediaMetadataCompat.METADATA_KEY_COMPILATION, showId)
                                .build());
                    }
                }
            }
        });

        return tracks;
    }

}