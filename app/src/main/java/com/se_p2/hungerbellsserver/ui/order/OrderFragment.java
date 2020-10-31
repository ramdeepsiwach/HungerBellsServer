package com.se_p2.hungerbellsserver.ui.order;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.se_p2.hungerbellsserver.R;
import com.se_p2.hungerbellsserver.TrackingOrderActivity;
import com.se_p2.hungerbellsserver.adapter.MyOrderAdapter;
import com.se_p2.hungerbellsserver.adapter.MyShipperSelectionAdapter;
import com.se_p2.hungerbellsserver.callback.IShipperLoadCallbackListener;
import com.se_p2.hungerbellsserver.common.BottomSheetOrderFragment;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.common.MySwipeHelper;
import com.se_p2.hungerbellsserver.eventBus.ChangeMenuClick;
import com.se_p2.hungerbellsserver.eventBus.LoadOrderEvent;
import com.se_p2.hungerbellsserver.model.FCMSendData;
import com.se_p2.hungerbellsserver.model.OrderModel;
import com.se_p2.hungerbellsserver.model.ShipperModel;
import com.se_p2.hungerbellsserver.model.ShippingOrderModel;
import com.se_p2.hungerbellsserver.model.TokenModel;
import com.se_p2.hungerbellsserver.remote.IFMCMService;
import com.se_p2.hungerbellsserver.remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrderFragment extends Fragment implements IShipperLoadCallbackListener {
    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    @BindView(R.id.txt_order_filter)
    TextView txt_order_filter;

    RecyclerView recycler_shipper;

    Unbinder unbinder;
    LayoutAnimationController layoutAnimationController;
    MyOrderAdapter adapter;

    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFMCMService ifmcmService;

    private OrderViewModel orderViewModel;
    private MyShipperSelectionAdapter myShipperSelectedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        orderViewModel =
                new ViewModelProvider(this).get(OrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_order, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        orderViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show());
        orderViewModel.getOrderModelMutableLiveData().observe(getViewLifecycleOwner(), orderModels -> {
            if (orderModels != null) {
                adapter = new MyOrderAdapter(getContext(), orderModels);
                recycler_order.setAdapter(adapter);
                recycler_order.setLayoutAnimation(layoutAnimationController);

                updateTextCounter();
            }
        });
        return root;
    }

    private void initViews() {
        ifmcmService = RetrofitFCMClient.getInstance().create(IFMCMService.class);
        shipperLoadCallbackListener = this;

        setHasOptionsMenu(true);

        recycler_order.setHasFixedSize(true);
        recycler_order.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);

        //window size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_order, width / 6) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Directions", 30, 0, Color.parseColor("#9B0000"),
                        pos -> {
                            OrderModel orderModel= ((MyOrderAdapter)recycler_order.getAdapter())
                                    .getItemAtPosition(pos);
                            if(orderModel.getOrderStatus()==1) {//Shipping
                                Common.currentOrderSelected=orderModel;
                                startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                
                            }else{
                                Toast.makeText(getContext(), new StringBuilder("Your order is ")
                                        .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                                        .append(". So you can't track directions."), Toast.LENGTH_SHORT).show();
                            }
                        }));
                buf.add(new MyButton(getContext(), "Call", 30, 0, Color.parseColor("#560027"),
                        pos -> Dexter.withActivity(getActivity()).withPermission(Manifest.permission.CALL_PHONE)
                                .withListener(new PermissionListener() {
                                    @Override
                                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                        OrderModel orderModel = adapter.getItemAtPosition(pos);
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_DIAL);
                                        intent.setData(Uri.parse("tel: " +
                                                orderModel.getUserPhone()));
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                        Toast.makeText(getContext(), "You must accept " + permissionDeniedResponse.getPermissionName(), Toast.LENGTH_SHORT).show();

                                    }

                                    @Override
                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                                    }
                                }).check()));

                buf.add(new MyButton(getContext(), "Remove", 30, 0, Color.parseColor("#12005E"),
                        pos -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete")
                                    .setMessage("Do you really want to delete this order ?")
                                    .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("DELETE", (dialogInterface, i) -> {
                                        OrderModel orderModel = adapter.getItemAtPosition(pos);
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.ORDER_REF)
                                                .child(orderModel.getKey())
                                                .removeValue()
                                                .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                                .addOnSuccessListener(aVoid -> {
                                                    adapter.removeItem(pos);
                                                    adapter.notifyDataSetChanged();
                                                    updateTextCounter();
                                                    dialogInterface.dismiss();
                                                    Toast.makeText(getContext(), "Order Deleted ! ", Toast.LENGTH_SHORT).show();
                                                });
                                    });

                            //create dialog
                            AlertDialog dialog = builder.create();
                            dialog.show();

                            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setTextColor(Color.RED);

                        }));
                buf.add(new MyButton(getContext(), "Edit", 30, 0, Color.parseColor("#336699"),
                        pos -> showEditDialog(adapter.getItemAtPosition(pos), pos)));
            }
        };

    }

    @SuppressLint("InflateParams")
    private void showEditDialog(OrderModel orderModel, int pos) {
        View layout_dialog;
        AlertDialog.Builder builder;
        if (orderModel.getOrderStatus() == 0) {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipping, null);

            recycler_shipper = layout_dialog.findViewById(R.id.recycler_shippers);

            builder = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                    .setView(layout_dialog);
        } else if (orderModel.getOrderStatus() == -1) {//Cancelled
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_cancelled, null);
            builder = new AlertDialog.Builder(requireContext()).setView(layout_dialog);
        } else {//Shipped
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipped, null);
            builder = new AlertDialog.Builder(requireContext()).setView(layout_dialog);
        }
        //View
        Button btn_ok = layout_dialog.findViewById(R.id.btn_ok);
        Button btn_cancel = layout_dialog.findViewById(R.id.btn_cancel);

        RadioButton rdi_shipping = layout_dialog.findViewById(R.id.rdi_shipping);
        RadioButton rdi_shipped = layout_dialog.findViewById(R.id.rdi_shipped);
        RadioButton rdi_cancelled = layout_dialog.findViewById(R.id.rdi_cancelled);
        RadioButton rdi_delete = layout_dialog.findViewById(R.id.rdi_delete);
        RadioButton rdi_restore_placed = layout_dialog.findViewById(R.id.rdi_restore_placed);

        TextView txt_status = layout_dialog.findViewById(R.id.txt_status);
        txt_status.setText(new StringBuilder("Order Status(")
                .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                .append(")"));

        AlertDialog dialog = builder.create();

        if (orderModel.getOrderStatus() == 0) {//Shipping
            loadShipperList(pos, orderModel, dialog, btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled,
                    rdi_delete, rdi_restore_placed);
        } else {
            showDialog(pos, orderModel, dialog, btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled,
                    rdi_delete, rdi_restore_placed);
        }
    }

    private void loadShipperList(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        List<ShipperModel> tempList = new ArrayList<>();
        DatabaseReference shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF);
        Query shipperActive = shipperRef.orderByChild("active").equalTo(true);//load active shippers
        shipperActive.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot shipperSnapshot : snapshot.getChildren()) {
                    ShipperModel shipperModel = shipperSnapshot.getValue(ShipperModel.class);
                    Objects.requireNonNull(shipperModel).setKey(shipperSnapshot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos, orderModel, tempList, dialog,
                        btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shipperLoadCallbackListener.onShipperLoadFailed(error.getMessage());
            }
        });
    }

    private void showDialog(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        dialog.show();

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_cancel.setOnClickListener(view -> dialog.dismiss());
        btn_ok.setOnClickListener(view -> {
            if (rdi_cancelled != null && rdi_cancelled.isChecked()) {
                updateOrder(pos, orderModel, -1);
                dialog.dismiss();
            } else if (rdi_shipping != null && rdi_shipping.isChecked()) {
                //updateOrder(pos, orderModel, 1);
                ShipperModel shipperModel;
                if (myShipperSelectedAdapter != null) {
                    shipperModel = myShipperSelectedAdapter.getSelectedShipper();
                    if (shipperModel != null) {
                        createShippingOrder(shipperModel, orderModel, dialog);
                    } else {
                        Toast.makeText(getContext(), "Please select Shipper", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (rdi_shipped != null && rdi_shipped.isChecked()) {
                updateOrder(pos, orderModel, 2);
                dialog.dismiss();
            } else if (rdi_restore_placed != null && rdi_restore_placed.isChecked()) {
                updateOrder(pos, orderModel, 0);
                dialog.dismiss();
            } else if (rdi_delete != null && rdi_delete.isChecked()) {
                deleteOrder(pos, orderModel);
                dialog.dismiss();
            }
        });

    }

    private void createShippingOrder(ShipperModel shipperModel, OrderModel orderModel, AlertDialog dialog) {
        ShippingOrderModel shippingOrderModel = new ShippingOrderModel();
        shippingOrderModel.setShipperPhone(shipperModel.getPhone());
        shippingOrderModel.setShipperName(shipperModel.getName());
        shippingOrderModel.setOrderModel(orderModel);
        shippingOrderModel.setStartTrip(false);
        shippingOrderModel.setCurrentLat(-1.0);
        shippingOrderModel.setCurrentLng(-1.0);

        int pos = 0;

        FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_ORDER_REF)
                .child(orderModel.getKey())
                .setValue(shippingOrderModel)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dialog.dismiss();
                FirebaseDatabase.getInstance()
                        .getReference(Common.TOKEN_REF)
                        .child(shipperModel.getKey())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    TokenModel tokenModel = snapshot.getValue(TokenModel.class);
                                    Map<String, String> notiData = new HashMap<>();
                                    notiData.put(Common.NOTI_TITLE, "Order available to ship");
                                    notiData.put(Common.NOTI_CONTENT, "You have new order need ship to " + orderModel.getUserPhone());

                                    FCMSendData sendData = new FCMSendData(Objects.requireNonNull(tokenModel).getToken(), notiData);
                                    compositeDisposable.add(ifmcmService.sendNotification(sendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(fcmResponse -> {
                                                dialog.dismiss();
                                                if (fcmResponse.getSuccess() == 1) {
                                                    updateOrder(pos, orderModel, 1);
                                                } else {
                                                    Toast.makeText(getContext(), "Failed to send to shipper ! Order was not updated !", Toast.LENGTH_SHORT).show();
                                                }

                                            }, throwable -> {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                            }));

                                } else {
                                    dialog.dismiss();
                                    Toast.makeText(getContext(), "Token not found ", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                dialog.dismiss();
                                Toast.makeText(getContext(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

    }

    private void deleteOrder(int pos, OrderModel orderModel) {
        if (!TextUtils.isEmpty(orderModel.getKey())) {
            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .removeValue()
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {
                        adapter.removeItem(pos);
                        adapter.notifyDataSetChanged();
                        updateTextCounter();
                        Toast.makeText(getContext(), "Delete order success!", Toast.LENGTH_SHORT).show();
                    });

        } else {
            Toast.makeText(getContext(), "Order number must not be null", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateOrder(int pos, OrderModel orderModel, int status) {
        if (!TextUtils.isEmpty(orderModel.getKey())) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("orderStatus", status);
            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .updateChildren(updateData)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {

                        android.app.AlertDialog dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
                        dialog.show();

                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(orderModel.getUserID())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            TokenModel tokenModel = snapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "Your order was updated");
                                            notiData.put(Common.NOTI_CONTENT, "Your order " + orderModel.getKey() +
                                                    " was updated to " +
                                                    Common.convertStatusToString(status));

                                            FCMSendData sendData = new FCMSendData(Objects.requireNonNull(tokenModel).getToken(), notiData);
                                            compositeDisposable.add(ifmcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if (fcmResponse.getSuccess() == 1) {
                                                            Toast.makeText(getContext(), "Update order success!", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(getContext(), "Update order success but failed to notify!", Toast.LENGTH_SHORT).show();
                                                        }

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }));

                                        } else {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Token not found ", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        adapter.removeItem(pos);
                        adapter.notifyDataSetChanged();
                        updateTextCounter();
                    });

        } else {
            Toast.makeText(getContext(), "Order number must not be null", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTextCounter() {
        txt_order_filter.setText(new StringBuilder("Orders (")
                .append(adapter.getItemCount())
                .append(")"));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.order_filter_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            BottomSheetOrderFragment bottomSheetOrderFragment
                    = BottomSheetOrderFragment.getInstance();
            bottomSheetOrderFragment.show(requireActivity().getSupportFragmentManager(), "OrderFilter");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent.class);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangeMenuClick(false));
        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoadOrderEvent(LoadOrderEvent event) {
        orderViewModel.loadOrderByStatus(event.getStatus());
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {
        //Null
    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        if (recycler_shipper != null) {
            recycler_shipper.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recycler_shipper.setLayoutManager(layoutManager);
            recycler_shipper.addItemDecoration(new DividerItemDecoration(requireContext(), layoutManager.getOrientation()));

            myShipperSelectedAdapter = new MyShipperSelectionAdapter(getContext(), shipperModels);
            recycler_shipper.setAdapter(myShipperSelectedAdapter);
        }
        showDialog(pos, orderModel, dialog, btn_ok, btn_cancel, rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
    }

    @Override
    public void onShipperLoadFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();

    }
}