package com.se_p2.hungerbellsserver.ui.shipper;

import android.widget.Button;
import android.widget.RadioButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.se_p2.hungerbellsserver.callback.IShipperLoadCallbackListener;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.OrderModel;
import com.se_p2.hungerbellsserver.model.ShipperModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ShipperViewModel extends ViewModel implements IShipperLoadCallbackListener {
    private MutableLiveData<String> messageError=new MutableLiveData<>();
    private MutableLiveData<List<ShipperModel>> shipperMutableList;
    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    public ShipperViewModel() {
        shipperLoadCallbackListener=this;
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    public MutableLiveData<List<ShipperModel>> getShipperMutableList() {
        if(shipperMutableList ==null){
            shipperMutableList=new MutableLiveData<>();
            loadShipper();
        }
        return shipperMutableList;

    }

    private void loadShipper() {
        List<ShipperModel> tempList=new ArrayList<>();
        DatabaseReference shipperRef= FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF);
        shipperRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot shipperSnapshot:snapshot.getChildren()){
                    ShipperModel shipperModel=shipperSnapshot.getValue(ShipperModel.class);
                    assert shipperModel != null;
                    shipperModel.setKey(shipperSnapshot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shipperLoadCallbackListener.onShipperLoadFailed(error.getMessage());
            }
        });
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {
        if(shipperModelList!=null)
            shipperMutableList.setValue(shipperModelList);
    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        //null
    }

    @Override
    public void onShipperLoadFailed(String message) {
        messageError.setValue(message);
    }
}