/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.touchsample;

import android.app.Activity;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset to
 * the paired wearable.
 */
public class MainActivity extends Activity implements
        MessageApi.MessageListener, NodeApi.NodeListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    /**
     * Request code for launching the Intent to resolve Google Play services errors.
     */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String TOUCH_POSITION_PATH = "/touch-position";
    private static final String NOTIFY_RECEIPT_PATH = "/notify-receipt";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private ListView mMessageList;
    private View mStartActivityBtn;

    private ArrayAdapter<String> mMessageListAdapter;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        LOGD(TAG, "onCreate");
        setContentView(R.layout.main_activity);

        mMessageList = (ListView) findViewById(R.id.message_list);
        mStartActivityBtn = findViewById(R.id.start_wearable_activity);

        mMessageListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mMessageList.setAdapter(mMessageListAdapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;
        mStartActivityBtn.setEnabled(true);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
        mStartActivityBtn.setEnabled(false);
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            mStartActivityBtn.setEnabled(false);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(TOUCH_POSITION_PATH)) {
            try {
                final String s = new String(messageEvent.getData(), "UTF-8");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMessageListAdapter.add(s);
                    }
                });
                new NotifyReceiptTask().execute();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);
    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    private class NotifyReceiptTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, node, NOTIFY_RECEIPT_PATH, new byte[0]);
            }
            return null;
        }
    }

    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        LOGD(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

}
