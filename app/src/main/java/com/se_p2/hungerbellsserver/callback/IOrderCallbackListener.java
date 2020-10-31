package com.se_p2.hungerbellsserver.callback;

import com.se_p2.hungerbellsserver.model.OrderModel;

import java.util.List;

public interface IOrderCallbackListener {
    void onOrderLoadSuccess(List<OrderModel> orderModelList);
    void onOrderLoadFailed(String message);
}
