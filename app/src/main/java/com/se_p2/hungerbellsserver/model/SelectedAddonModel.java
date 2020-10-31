package com.se_p2.hungerbellsserver.model;

public class SelectedAddonModel {
    private AddonModel addonModel;

    public SelectedAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }

    public AddonModel getAddonModel() {
        return addonModel;
    }

    public void setAddonModel(AddonModel addonModel) {
        this.addonModel = addonModel;
    }
}
