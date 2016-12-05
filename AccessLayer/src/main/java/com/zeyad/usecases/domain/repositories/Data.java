package com.zeyad.usecases.domain.repositories;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import io.realm.RealmQuery;
import rx.Observable;

/**
 * Interface that represents a Data for getting Objects from the data layer.
 */
public interface Data {

    /**
     * Get an {@link Observable} which will emit a collectionFromDisk of Items.
     */
    @NonNull
    Observable<List> getListDynamically(String url, Class domainClass, Class dataClass, boolean persist,
                                        boolean shouldCache);

    /**
     * Get an {@link Observable} which will emit an Item.
     *
     * @param itemId The user id used to retrieve getDynamicallyById data.
     */
    @NonNull
    Observable<?> getObjectDynamicallyById(String url, String idColumnName, int itemId, Class domainClass,
                                           Class dataClass, boolean persist, boolean shouldCache);

    @NonNull
    Observable<?> postObjectDynamically(String url, String idColumnName, JSONObject keyValuePairs,
                                        Class domainClass, Class dataClass, boolean persist, boolean queuable);

    @NonNull
    Observable<?> postListDynamically(String url, String idColumnName, JSONArray jsonArray,
                                      Class domainClass, Class dataClass, boolean persist, boolean queuable);

    @NonNull
    Observable<?> deleteListDynamically(String url, JSONArray jsonArray, Class domainClass,
                                        Class dataClass, boolean persist, boolean queuable);

    @NonNull
    Observable<?> putObjectDynamically(String url, String idColumnName, JSONObject keyValuePairs,
                                       Class domainClass, Class dataClass, boolean persist, boolean queuable);

    @NonNull
    Observable<?> putListDynamically(String url, String idColumnName, JSONArray jsonArray,
                                     Class domainClass, Class dataClass, boolean persist, boolean queuable);

    @NonNull
    Observable<Boolean> deleteAllDynamically(String url, Class dataClass, boolean persist);

    @NonNull
    Observable<?> searchDisk(String query, String column, Class domainClass, Class dataClass);

    @NonNull
    Observable<?> searchDisk(RealmQuery query, Class domainClass);
}