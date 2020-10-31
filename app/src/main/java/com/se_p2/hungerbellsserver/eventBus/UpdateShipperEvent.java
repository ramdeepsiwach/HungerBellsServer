package com.se_p2.hungerbellsserver.eventBus;

import com.se_p2.hungerbellsserver.model.ShipperModel;

public class UpdateShipperEvent {
    private ShipperModel shipperModel;
    private Boolean active;

    public UpdateShipperEvent(ShipperModel shipperModel, Boolean active) {
        this.shipperModel = shipperModel;
        this.active = active;
    }

    public ShipperModel getShipperModel() {
        return shipperModel;
    }

    public void setShipperModel(ShipperModel shipperModel) {
        this.shipperModel = shipperModel;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
