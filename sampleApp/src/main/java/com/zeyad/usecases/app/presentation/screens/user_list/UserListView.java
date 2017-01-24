package com.zeyad.usecases.app.presentation.screens.user_list;

import rx.Observable;

/**
 * @author zeyad on 11/1/16.
 */
interface UserListView {

    Observable<UserListModel> getUserList();

    Observable updateItemByItem();

    Observable writePeriodic();

    void incrementPage();

    void setYScroll(int yScroll);
}
