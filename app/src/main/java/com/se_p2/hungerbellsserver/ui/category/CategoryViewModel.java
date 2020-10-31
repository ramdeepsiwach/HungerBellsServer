package com.se_p2.hungerbellsserver.ui.category;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.se_p2.hungerbellsserver.callback.ICategoryCallbackListener;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.CategoryModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CategoryViewModel extends ViewModel implements ICategoryCallbackListener {

    private MutableLiveData<List<CategoryModel>> categoryListMutable;
    private MutableLiveData<String> messageError=new MutableLiveData<>();
    private ICategoryCallbackListener categoryCallbackListener;
    private MutableLiveData<String> mText;

    public CategoryViewModel() {
        categoryCallbackListener = this;
    }

    public MutableLiveData<List<CategoryModel>> getCategoryListMutable() {
        if(categoryListMutable==null){
            categoryListMutable=new MutableLiveData<>();
            messageError=new MutableLiveData<>();
            loadCategories();
        }
        return categoryListMutable;
    }

    public void loadCategories() {
        final List<CategoryModel> tempList=new ArrayList<>();
        DatabaseReference categoryRef= FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF);
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot itemSnapShot:snapshot.getChildren()){
                    CategoryModel categoryModel=itemSnapShot.getValue(CategoryModel.class);
                    assert categoryModel != null;
                    categoryModel.setMenu_id(itemSnapShot.getKey());
                    tempList.add(categoryModel);
                }
                categoryCallbackListener.onCategoryLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                categoryCallbackListener.onCategoryLoadFailed(error.getMessage());

            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }


    @Override
    public void onCategoryLoadSuccess(List<CategoryModel> categoryModelList) {
        categoryListMutable.setValue(categoryModelList);
    }

    @Override
    public void onCategoryLoadFailed(String message) {
        messageError.setValue(message);
    }
}