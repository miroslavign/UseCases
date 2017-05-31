package com.zeyad.usecases.services;

import android.os.Bundle;

import com.zeyad.usecases.requests.PostRequest;
import com.zeyad.usecases.stores.CloudDataStore;
import com.zeyad.usecases.utils.Utils;

import org.json.JSONArray;
import org.junit.Test;

import io.reactivex.observers.TestObserver;

import static com.zeyad.usecases.requests.PostRequest.POST;
import static com.zeyad.usecases.services.GenericJobService.PAYLOAD;
import static org.mockito.Mockito.mock;

/**
 * @author by ZIaDo on 5/20/17.
 */
public class GenericJobServiceLogicTest {
    @Test
    public void startJob() throws Exception {
        GenericJobServiceLogic genericJobServiceLogic = new GenericJobServiceLogic();
        TestObserver testSubscriber = new TestObserver();

        Bundle extras = new Bundle(2);
        extras.putString(GenericJobService.JOB_TYPE, GenericJobService.POST);
        extras.putParcelable(PAYLOAD, new PostRequest.Builder(null, true)
                .idColumnName("id")
                .payLoad(new JSONArray())
                .url("")
                .method(POST)
                .build());
        genericJobServiceLogic.startJob(extras, mock(CloudDataStore.class), Utils.getInstance(), "")
                .subscribe(new TestObserver<>());
        testSubscriber.assertNoErrors();
//        testSubscriber.assertComplete();
    }
}