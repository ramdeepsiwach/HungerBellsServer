package com.se_p2.hungerbellsserver.eventBus;

import com.se_p2.hungerbellsserver.model.AddonModel;

import java.util.List;

public class UpdateAddonModel {
    private List<AddonModel> addonModels;

    public UpdateAddonModel() {
    }

    public List<AddonModel> getAddonModels() {
        return addonModels;
    }

    public void setAddonModels(List<AddonModel> addonModels) {
        this.addonModels = addonModels;
    }
}
