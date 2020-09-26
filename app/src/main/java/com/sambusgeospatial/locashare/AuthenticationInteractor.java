package com.sambusgeospatial.locashare;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;

public class AuthenticationInteractor {
    private static AuthenticationInteractor mInstance;
    private static Context mCtx;
    private RequestQueue mRequestQueue;

    private AuthenticationInteractor(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
        VolleyLog.DEBUG = false;
    }

    public static synchronized AuthenticationInteractor getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AuthenticationInteractor(context);
        }
        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public void clearQueue() {
        mRequestQueue.getCache().clear();
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}

