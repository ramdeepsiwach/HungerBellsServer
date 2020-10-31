package com.se_p2.hungerbellsserver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.se_p2.hungerbellsserver.eventBus.CategoryClick;
import com.se_p2.hungerbellsserver.eventBus.ChangeMenuClick;
import com.se_p2.hungerbellsserver.eventBus.ToastEvent;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.FCMResponse;
import com.se_p2.hungerbellsserver.model.FCMSendData;
import com.se_p2.hungerbellsserver.remote.IFMCMService;
import com.se_p2.hungerbellsserver.remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST =3002 ;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavController navController;
    private int menuClick = -1;

    private ImageView img_upload;
    private final CompositeDisposable compositeDisposable=new CompositeDisposable();
    private IFMCMService ifmcmService;
    private Uri imgUri=null;

    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ifmcmService= RetrofitFCMClient.getInstance().create(IFMCMService.class);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference= storage.getReference();

        updateToken();

        subscribeToTopic(Common.createTopicOrder());
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_category, R.id.nav_food_list
                , R.id.nav_order, R.id.nav_shipper)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = headerView.findViewById(R.id.currentUserName);
        txt_user.setText(String.format("Hey, %s", Common.currentServerUser.getName()));

        menuClick = R.id.nav_category;//Default

        checkIsOpenOrderActivity();

    }

    private void checkIsOpenOrderActivity() {
        boolean isOpenOrderActivity = getIntent().getBooleanExtra(Common.IS_OPEN_ORDER_ACTIVITY, false);
        if (isOpenOrderActivity) {
            navController.popBackStack();
            navController.navigate(R.id.nav_order);
            menuClick = R.id.nav_order;
        }
    }

    private void updateToken() {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(instanceIdResult -> {
                    Common.updateToken(HomeActivity.this, instanceIdResult.getToken(), true, false);
                    Log.d("TOKEN", instanceIdResult.getId());
                });

    }

    private void subscribeToTopic(String topicOrder) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topicOrder)
                .addOnFailureListener(e -> Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(this, "Failed" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategoryClick(CategoryClick event) {
        if (event.isSuccess())
            if (menuClick != R.id.nav_food_list) {
                navController.navigate(R.id.nav_food_list);
                menuClick = R.id.nav_food_list;
            }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onToastEvent(ToastEvent event) {
        if (event.getAction() == Common.ACTION.CREATE) {
            Toast.makeText(this, "Create Success", Toast.LENGTH_SHORT).show();
        }else if (event.getAction() == Common.ACTION.UPDATE) {
            Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Delete Success", Toast.LENGTH_SHORT).show();
        }
        EventBus.getDefault().postSticky(new ChangeMenuClick(event.isFromFoodList()));

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onChangeMenuClick(ChangeMenuClick event) {
        if (event.isFromFoodList()) {
            navController.popBackStack(R.id.nav_category, true);
            navController.navigate(R.id.nav_category);
        } else {
            navController.popBackStack(R.id.nav_food_list, true);
            navController.navigate(R.id.nav_food_list);
        }
        menuClick = -1;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        drawer.closeDrawers();
        switch (item.getItemId()) {
            case R.id.nav_category:
                if (item.getItemId() != menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_category);
                }
                break;

            case R.id.nav_news_feed:
                showNewsDialog();
                break;

            case R.id.nav_order:
                if (item.getItemId() != menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_order);
                }
                break;
            case R.id.nav_shipper:
                if (item.getItemId() != menuClick) {
                    navController.popBackStack();
                    navController.navigate(R.id.nav_shipper);
                }
                break;
            case R.id.nav_sign_out:
                signOut();
                break;
            default:
                menuClick = -1;
                break;
        }
        menuClick = item.getItemId();
        return true;
    }

    private void showNewsDialog() {
        android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(this);
        builder.setTitle("News Feed");
        builder.setMessage("Send news notification to users");

        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_news_feed,null);

        EditText edt_title=itemView.findViewById(R.id.edt_title);
        EditText edt_content=itemView.findViewById(R.id.edt_content);
        EditText edt_link=itemView.findViewById(R.id.edt_link);

        img_upload=itemView.findViewById(R.id.img_upload);
        RadioButton rdi_none=itemView.findViewById(R.id.rdi_none);
        RadioButton rdi_link=itemView.findViewById(R.id.rdi_link);
        RadioButton rdi_upload=itemView.findViewById(R.id.rdi_image);

        rdi_none.setOnClickListener(v -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_link.setOnClickListener(v -> {
            edt_link.setVisibility(View.VISIBLE);
            img_upload.setVisibility(View.GONE);
        });
        rdi_upload.setOnClickListener(v -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.VISIBLE);
        });

        img_upload.setOnClickListener(v -> {
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
        });

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("SEND", (dialog, which) -> {
            if(rdi_none.isChecked()){
                sendNews(edt_title.getText().toString(),edt_content.getText().toString());
            }else if(rdi_link.isChecked()){
                sendNews(edt_title.getText().toString(),edt_content.getText().toString(),edt_link.getText().toString());
            }else if(rdi_upload.isChecked()){
                if(imgUri !=null){
                    AlertDialog dial=new AlertDialog.Builder(this).setMessage("Uploading...").create();
                    dial.show();

                    String file_name= UUID.randomUUID().toString();
                    StorageReference newsImages=storageReference.child("news/"+file_name);
                    newsImages.putFile(imgUri)
                            .addOnFailureListener(e -> {
                                dial.dismiss();
                                Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }).addOnSuccessListener(taskSnapshot -> {
                                dial.dismiss();
                                newsImages.getDownloadUrl().addOnSuccessListener(uri -> sendNews(edt_title.getText().toString(),edt_content.getText().toString(),uri.toString()));
                            }).addOnProgressListener(snapshot -> {
                                double progress=Math.round(100.0 * snapshot.getBytesTransferred()/snapshot.getTotalByteCount());
                                dial.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));

                            });
                }
            }
        });

        android.app.AlertDialog dialog=builder.create();
        dialog.show();

    }

    private void sendNews(String title, String content, String urlLink) {
        Map<String,String> notificationData=new HashMap<>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT,content);
        notificationData.put(Common.IS_SEND_IMAGE,"true");
        notificationData.put(Common.IMAGE_URL,urlLink);

        FCMSendData fcmSendData=new FCMSendData(Common.getNewsTopic(),notificationData);

        AlertDialog dialog=new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifmcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if(fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this, "News Posted !", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "News post failed !", Toast.LENGTH_SHORT).show();
                },throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void sendNews(String title, String content) {
        Map<String,String> notificationData=new HashMap<>();
        notificationData.put(Common.NOTI_TITLE,title);
        notificationData.put(Common.NOTI_CONTENT,content);
        notificationData.put(Common.IS_SEND_IMAGE,"false");

        FCMSendData fcmSendData=new FCMSendData(Common.getNewsTopic(),notificationData);

        AlertDialog dialog=new AlertDialog.Builder(this).setMessage("Waiting...").create();
        dialog.show();

        compositeDisposable.add(ifmcmService.sendNotification(fcmSendData)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(fcmResponse -> {
            dialog.dismiss();
            if(fcmResponse.getMessage_id() != 0)
                Toast.makeText(this, "News Posted !", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "News post failed !", Toast.LENGTH_SHORT).show();
        },throwable -> {
            dialog.dismiss();
            Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void signOut() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log out").setMessage("Do you really want to sign out ?")
                .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("OK", (dialogInterface, i) -> {

                    Common.selectedFood = null;
                    Common.categorySelected = null;
                    Common.currentServerUser = null;
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK){
            if(data!=null && data.getData()!=null){
                imgUri=data.getData();
                img_upload.setImageURI(imgUri);
            }
        }
    }
}