package com.zeyad.usecases.data.network;

import android.support.annotation.NonNull;

import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;

class RestApiTestRobot {
    public static IApiConnection createMockedApiConnection() {
        final IApiConnection apiConnection = Mockito.mock(IApiConnection.class);
        Mockito.when(apiConnection.dynamicDownload(Mockito.anyString()))
                .thenReturn(getResponseBodyObservable());
        Mockito.when(apiConnection.dynamicGetObject(Mockito.anyString()))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.dynamicGetObject(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.dynamicGetList(Mockito.anyString()))
                .thenReturn(getListObservable());
        Mockito.when(apiConnection.dynamicGetList(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(getListObservable());
        Mockito.when(apiConnection.dynamicPost(Mockito.anyString(), Mockito.any()))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.dynamicPut(Mockito.anyString(), Mockito.any()))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.upload(Mockito.anyString(), Mockito.any(Map.class),
                Mockito.any(MultipartBody.Part.class)))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.dynamicDelete(Mockito.anyString(), Mockito.any()))
                .thenReturn(getObjectObservable());
        Mockito.when(apiConnection.dynamicDelete(Mockito.anyString(), Mockito.any(RequestBody.class)))
                .thenReturn(getObjectObservable());
        return apiConnection;
    }

    @NonNull
    private static Observable<List> getListObservable() {
        return Observable.create(
                new Observable.OnSubscribe<List>() {
                    @Override
                    public void call(@NonNull Subscriber<? super List> subscriber) {
                        subscriber.onNext(Collections.singletonList(""));
                    }
                });
    }

    @NonNull
    private static Observable<Object> getObjectObservable() {
        return Observable.create(
                subscriber -> {
                    subscriber.onNext("");
                });
    }

    @NonNull
    private static Observable<ResponseBody> getResponseBodyObservable() {
        return Observable.create(
                subscriber -> {
                    subscriber.onNext(Mockito.mock(ResponseBody.class));
                });
    }
}