package com.se_p2.hungerbellsserver.callback;

import com.se_p2.hungerbellsserver.model.ShippingOrderModel;

public interface ISingleShippingOrderCallbackListener {
    void onSingleShippingOrderLoadSuccess(ShippingOrderModel shippingOrderModel);

}
