package com.zeyad.usecases.app;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.zeyad.usecases.app.presentation.models.AutoMap_DAOMapperFactory;
import com.zeyad.usecases.domain.interactors.data.DataUseCaseConfig;
import com.zeyad.usecases.domain.interactors.data.DataUseCaseFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.flowup.FlowUp;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.rx.RealmObservableFactory;
import jp.wasabeef.takt.Takt;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static com.zeyad.usecases.app.utils.Constants.URLS.API_BASE_URL;

/**
 * @author by ZIaDo on 9/24/16.
 */
public class GenericApplication extends Application {

    private static final int TIME_OUT = 15, CACHE_EXPIRY = 3;
    private static GenericApplication sInstance;

    public static GenericApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
//        initializeStrickMode();
        super.onCreate();
        sInstance = this;
        if (BuildConfig.DEBUG)
            Takt.stock(this).play();
        initializeRealm();
        DataUseCaseFactory.init(new DataUseCaseConfig.Builder(this)
                .baseUrl(API_BASE_URL)
                .withCache(CACHE_EXPIRY, TimeUnit.MINUTES)
                .withRealm()
                .entityMapper(new AutoMap_DAOMapperFactory())
                .okHttpBuilder(provideOkHttpClientBuilder())
//                .okhttpCache(provideCache())
                .build());
        initializeStetho();
        initializeFlowUp();
    }

    private void initializeStrickMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private OkHttpClient.Builder provideOkHttpClientBuilder() {
        return new OkHttpClient.Builder()
                .addInterceptor(provideHttpLoggingInterceptor())
//                .addInterceptor(provideCacheInterceptor())
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT, TimeUnit.SECONDS);
    }

    private HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        return new HttpLoggingInterceptor(message -> Log.d("NetworkInfo", message))
                .setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    private Cache provideCache() {
        Cache cache = null;
        try {
            cache = new Cache(new File(getCacheDir(), "http-cache"), 10 * 1024 * 1024); // 10 MB
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cache;
    }

    private Interceptor provideCacheInterceptor() {
        return chain -> {
            Response response = chain.proceed(chain.request());
            // re-write response header to force use of cache
            CacheControl cacheControl = new CacheControl.Builder()
                    .maxAge(CACHE_EXPIRY, TimeUnit.MINUTES)
                    .build();
            return response.newBuilder()
                    .header("Cache-Control", cacheControl.toString())
                    .build();
        };
    }

    private void initializeRealm() {
        Realm.init(this);
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder()
                .name("app.realm")
                .modules(Realm.getDefaultModule(), new LibraryModule())
                .rxFactory(new RealmObservableFactory())
                .deleteRealmIfMigrationNeeded()
                .build());
    }

    private void initializeStetho() {
//        Stetho.initialize(Stetho.newInitializerBuilder(this)
//                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
//                .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
//                .build());
//        RealmInspectorModulesProvider.builder(this)
//                .withFolder(getCacheDir())
////                .withEncryptionKey("encrypted.realm", key)
//                .withMetaTables()
//                .withDescendingOrder()
//                .withLimit(1000)
//                .databaseNamePattern(Pattern.compile(".+\\.realm"))
//                .build();
    }

    private void initializeFlowUp() {
        FlowUp.Builder.with(this)
                .apiKey(getString(R.string.flow_up_api_key))
                .forceReports(BuildConfig.DEBUG)
                .start();
    }
}
