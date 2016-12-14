package com.zeyad.generic.usecase.dataaccesslayer.components.mvp;

import com.zeyad.generic.usecase.dataaccesslayer.components.mvvm.LoadDataView;

/**
 * @author by zeyad on 16/05/16.
 */
public interface GenericDetailView<M> extends LoadDataView {
    /**
     * Render an item in the UI.
     *
     * @param item The {@link M} that will be shown.
     */
    void renderItem(M item);
}