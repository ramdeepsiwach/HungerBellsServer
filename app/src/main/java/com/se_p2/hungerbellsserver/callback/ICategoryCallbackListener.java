package com.se_p2.hungerbellsserver.callback;

import com.se_p2.hungerbellsserver.model.CategoryModel;

import java.util.List;

public interface ICategoryCallbackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelList);
    void onCategoryLoadFailed(String message);
}
