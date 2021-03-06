package com.zeyad.usecases.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.zeyad.usecases.Config;
import com.zeyad.usecases.requests.FileIORequest;
import com.zeyad.usecases.requests.PostRequest;
import com.zeyad.usecases.services.GenericJobService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import retrofit2.HttpException;
import rx.Observable;

public class Utils {

    private static Utils instance;

    private Utils() {
    }

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    public boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connectivityManager.getAllNetworks();
            for (Network network : networks) {
                if (connectivityManager
                        .getNetworkInfo(network)
                        .getState()
                        .equals(NetworkInfo.State.CONNECTED)) {
                    return true;
                }
            }
        } else if (connectivityManager != null) {
            NetworkInfo[] info = connectivityManager.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo anInfo : info) {
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void queuePostCore(@NonNull FirebaseJobDispatcher dispatcher, @NonNull PostRequest postRequest) {
        Bundle extras = new Bundle(2);
        extras.putString(GenericJobService.JOB_TYPE, GenericJobService.POST);
        extras.putParcelable(GenericJobService.PAYLOAD, postRequest);
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
                .setService(GenericJobService.class)
                .setTag(GenericJobService.POST)
                .setRecurring(false)
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                .setTrigger(Trigger.executionWindow(0, 60))
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK, Constraint.DEVICE_CHARGING)
                .setExtras(extras)
                .build());
        Log.d("FBJD", postRequest.getMethod() + " request is queued successfully!");
    }

    public void queueFileIOCore(@NonNull FirebaseJobDispatcher dispatcher, boolean isDownload,
                                @NonNull FileIORequest fileIORequest) {
        Bundle extras = new Bundle(2);
        extras.putString(GenericJobService.JOB_TYPE, isDownload ?
                GenericJobService.DOWNLOAD_FILE : GenericJobService.UPLOAD_FILE);
        extras.putParcelable(GenericJobService.PAYLOAD, fileIORequest);
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
                .setService(GenericJobService.class)
                .setTag(isDownload ? GenericJobService.DOWNLOAD_FILE : GenericJobService.UPLOAD_FILE)
                .setRecurring(false)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(fileIORequest.onWifi() ? Constraint.ON_UNMETERED_NETWORK : Constraint.ON_ANY_NETWORK,
                        fileIORequest.isWhileCharging() ? Constraint.DEVICE_CHARGING : 0)
                .setExtras(extras)
                .build());
        Log.d("FBJD", String.format("%s file request is queued successfully!", isDownload ?
                "Download" : "Upload"));
    }

    public List<Long> convertToListOfId(@Nullable JSONArray jsonArray) {
        List<Long> idList = new ArrayList<>();
        if (jsonArray != null && jsonArray.length() > 0) {
            try {
                jsonArray.getLong(0);
            } catch (JSONException e) {
                return null;
            }
            idList = new ArrayList<>(jsonArray.length());
            int length = jsonArray.length();
            for (int i = 0; i < length; i++) {
                try {
                    idList.add(jsonArray.getLong(i));
                } catch (JSONException e) {
                    Log.e("Utils", "convertToListOfId", e);
                }
            }
        }
        return idList;
    }

    public <T> Flowable<T> toFlowable(Observable<T> source) {
        if (source == null) {
            throw new IllegalArgumentException("Source observable is null");
        } else {
            return new ObservableV1ToFlowableV2<>(source);
        }
    }

    @NonNull
    public JSONObject getErrorJsonObject(HttpException exception) throws JSONException, IOException {
        return new JSONObject(exception.response().errorBody().string());
    }

    public boolean withDisk(boolean shouldPersist) {
        return Config.isWithDisk() && shouldPersist;
    }

    public boolean withCache(boolean shouldCache) {
        return Config.isWithCache() && shouldCache;
    }
}
