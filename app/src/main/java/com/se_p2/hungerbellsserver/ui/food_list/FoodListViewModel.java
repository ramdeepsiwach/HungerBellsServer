package com.se_p2.hungerbellsserver.ui.food_list;

import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.FoodModel;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FoodListViewModel extends ViewModel {

    private MutableLiveData<List<FoodModel>> mutableLiveDataFoodList;

    public FoodListViewModel() {

    }

    public MutableLiveData<List<FoodModel>> getMutableLiveDataFoodList() {
        if (mutableLiveDataFoodList==null)
            mutableLiveDataFoodList=new MutableLiveData<>();
        mutableLiveDataFoodList.setValue(Common.categorySelected.getFoods());
        return mutableLiveDataFoodList;
    }
}