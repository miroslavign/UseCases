package com.zeyad.usecases.app.presentation.user_detail;

import android.os.Bundle;

import com.zeyad.usecases.app.components.mvvm.BaseViewModel;
import com.zeyad.usecases.app.models.AutoMap_UserModel;
import com.zeyad.usecases.app.models.UserModel;
import com.zeyad.usecases.data.requests.GetRequest;
import com.zeyad.usecases.domain.interactors.data.DataUseCaseFactory;
import com.zeyad.usecases.domain.interactors.data.IDataUseCase;

import org.parceler.Parcels;

import rx.Observable;

/**
 * @author zeyad on 11/29/16.
 */

public class UserDetailVM extends BaseViewModel implements UserDetailView {

    private final IDataUseCase dataUseCase;

    UserDetailVM() {
        dataUseCase = DataUseCaseFactory.getInstance();
    }

    @Override
    public Observable getUserByLogin(String login) {
        return dataUseCase.getList(new GetRequest
                .GetRequestBuilder(AutoMap_UserModel.class, true)
                .presentationClass(UserModel.class)
                .url("users/" + login)
                .build());
    }

    @Override
    public Bundle getState() {
        Bundle outState = new Bundle(1);
        outState.putParcelable(UserDetailFragment.ARG_USER, Parcels.wrap(null));
        return outState;
    }

    @Override
    public void restoreState(Bundle state) {
        ((UserDetailFragment) getView()).setUserModel(state.getParcelable(UserDetailFragment.ARG_USER));
    }
}
