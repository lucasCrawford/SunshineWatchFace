package com.example.hercules.wearable.tasks;

/**
 * Created by lcrawford on 4/3/16.
 */

import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Simple AsyncTask that fetches the connected nodes to the wearable, and sends
 * back the list in a callback
 */
public class GetNodesTask extends AsyncTask<Void, Void, List<Node>> {
    private OnNodesLoadedListener mNodesListener;
    private GoogleApiClient mGoogleApiClient;

    public GetNodesTask(GoogleApiClient client, OnNodesLoadedListener listener){
        mNodesListener = listener;
        mGoogleApiClient = client;
    }

    @Override
    protected List<Node> doInBackground(Void... params) {
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        return nodes.getNodes();
    }

    @Override
    protected void onPostExecute(List<Node> nodes) {
        mNodesListener.onNodesFound(nodes);
    }

    /**
     * Callback for handling when the nodes are loaded that are connected to the wearable
     */
    public interface OnNodesLoadedListener{
        public void onNodesFound(List<Node> nodes);
    }
}
