package com.zeyad.usecases.data.requests;

import android.support.annotation.NonNull;

import com.zeyad.usecases.Config;
import com.zeyad.usecases.data.repository.DataRepository;

/**
 * @author zeyad on 7/29/16.
 */
public class GetRequest {

    private String url;
    private Class dataClass, presentationClass;
    private boolean persist;
    private String idColumnName;
    private int itemId;
    private boolean shouldCache;

    public GetRequest(@NonNull GetRequestBuilder getRequestBuilder) {
        url = getRequestBuilder.mUrl;
        dataClass = getRequestBuilder.mDataClass;
        presentationClass = getRequestBuilder.mPresentationClass;
        persist = getRequestBuilder.mPersist;
        idColumnName = getRequestBuilder.mIdColumnName;
        itemId = getRequestBuilder.mItemId;
        shouldCache = getRequestBuilder.mShouldCache;
    }

    public GetRequest(String url, String idColumnName, int itemId, @NonNull Class presentationClass,
                      Class dataClass, boolean persist, boolean shouldCache) {
        this.url = url;
        this.idColumnName = idColumnName;
        this.itemId = itemId;
        this.presentationClass = presentationClass;
        this.dataClass = dataClass;
        this.persist = persist;
        this.shouldCache = shouldCache;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public Class getDataClass() {
        return dataClass;
    }

    public Class getPresentationClass() {
        return presentationClass != null ? presentationClass : dataClass;
    }

    public boolean isPersist() {
        return persist;
    }

    public boolean isShouldCache() {
        return shouldCache;
    }

    public String getIdColumnName() {
        return idColumnName != null ? idColumnName : DataRepository.DEFAULT_ID_KEY;
    }

    public int getItemId() {
        return itemId;
    }

    public static class GetRequestBuilder {
        private boolean mShouldCache;
        private String mIdColumnName;
        private int mItemId;
        private String mUrl;
        private Class mDataClass, mPresentationClass;
        private boolean mPersist;

        public GetRequestBuilder(Class dataClass, boolean persist) {
            mDataClass = dataClass;
            mPersist = persist;
        }

        @NonNull
        public GetRequestBuilder url(String url) {
            mUrl = Config.getBaseURL() + url;
            return this;
        }

        @NonNull
        public GetRequestBuilder fullUrl(String url) {
            mUrl = url;
            return this;
        }

        @NonNull
        public GetRequestBuilder presentationClass(Class presentationClass) {
            mPresentationClass = presentationClass;
            return this;
        }

        @NonNull
        public GetRequestBuilder shouldCache(boolean shouldCache) {
            mShouldCache = shouldCache;
            return this;
        }

        @NonNull
        public GetRequestBuilder idColumnName(String idColumnName) {
            mIdColumnName = idColumnName;
            return this;
        }

        @NonNull
        public GetRequestBuilder id(int id) {
            mItemId = id;
            return this;
        }

        @NonNull
        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}