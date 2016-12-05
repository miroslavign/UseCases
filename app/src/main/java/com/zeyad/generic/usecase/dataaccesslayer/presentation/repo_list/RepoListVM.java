package com.zeyad.generic.usecase.dataaccesslayer.presentation.repo_list;

import android.os.Bundle;

import com.zeyad.generic.usecase.dataaccesslayer.components.mvvm.BaseViewModel;
import com.zeyad.generic.usecase.dataaccesslayer.models.data.UserRealm;
import com.zeyad.generic.usecase.dataaccesslayer.models.ui.UserModel;
import com.zeyad.usecases.data.requests.GetRequest;
import com.zeyad.usecases.domain.interactors.data.DataUseCaseFactory;
import com.zeyad.usecases.domain.interactors.data.IDataUseCase;

import rx.Observable;

/**
 * @author zeyad on 11/1/16.
 */
class RepoListVM extends BaseViewModel implements RepoListView {

    //    private static final String USER_NAME = "userName";
    private final IDataUseCase genericUseCase;
//    private String userName;

    RepoListVM() {
        genericUseCase = DataUseCaseFactory.getInstance();
    }

    @Override
    public Observable getRepoList() {
        return genericUseCase.getList(new GetRequest.GetRequestBuilder(UserRealm.class, true)
                .presentationClass(UserModel.class)
                .url("users/users.json")
                .build());
    }

    @Override
    public Bundle getState() {
        Bundle bundle = new Bundle();
//        bundle.putString(USER_NAME, userName);
        return bundle;
    }

    @Override
    public void restoreState(Bundle state) {
//        userName = state.getString(USER_NAME, "");
    }
}