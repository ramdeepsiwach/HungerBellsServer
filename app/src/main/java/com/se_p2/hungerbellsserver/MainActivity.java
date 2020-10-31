package com.se_p2.hungerbellsserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.se_p2.hungerbellsserver.common.Common;
import com.se_p2.hungerbellsserver.model.ServerUser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE=7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference serverRef;
    private List<AuthUI.IdpConfig> providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        providers= Collections.singletonList(new AuthUI.IdpConfig.PhoneBuilder()
                .setDefaultCountryIso("IN")
                .build());
        serverRef= FirebaseDatabase.getInstance().getReference(Common.SERVER_REF);
        firebaseAuth=FirebaseAuth.getInstance();
        dialog=new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        listener=firebaseAuthLocal->{
            FirebaseUser user=firebaseAuthLocal.getCurrentUser();
            if(user!=null){
                //Check user from firebase
                checkServerUserFromFirebase(user);
            }else{
                phoneLogin();
            }
        };
    }

    private void checkServerUserFromFirebase(FirebaseUser user) {
        dialog.show();serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            ServerUser user=snapshot.getValue(ServerUser.class);
                            if(Objects.requireNonNull(user).isActive()){
                                goToHomeActivity(user);
                            }else{
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this,"You are not allowed for this app yet !",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            //User not exist
                            dialog.dismiss();
                            showRegisterDialog(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this,""+error.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information \n Admin will accept your account later");

        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_register,null);
        EditText edt_name=itemView.findViewById(R.id.edt_name);
        EditText edt_phone=itemView.findViewById(R.id.edt_phone);

        //Set data
        edt_phone.setText(user.getPhoneNumber());
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton("REGISTER", (dialogInterface, i) -> {
                    if(TextUtils.isEmpty(edt_name.getText().toString())){
                        Toast.makeText(this,"Please enter your name",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ServerUser serverUser=new ServerUser();
                    serverUser.setUid(user.getUid());
                    serverUser.setName(edt_name.getText().toString());
                    serverUser.setPhone(edt_phone.getText().toString());
                    serverUser.setActive(false); //Default

                    dialog.show();

                    serverRef.child(serverUser.getUid())
                            .setValue(serverUser)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                            }).addOnCompleteListener(task -> {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this,"Register successful ",Toast.LENGTH_SHORT).show();
                                //goToHomeActivity(serverUser);
                            });
                });

        builder.setView(itemView);

        AlertDialog registerDialog=builder.create();
        registerDialog.show();
    }

    private void goToHomeActivity(ServerUser serverUser) {

        dialog.dismiss();
        Common.currentServerUser=serverUser;
        Intent intent=new Intent(this,HomeActivity.class);
        intent.putExtra(Common.IS_OPEN_ORDER_ACTIVITY,getIntent().getBooleanExtra(Common.IS_OPEN_ORDER_ACTIVITY,false));
        startActivity(intent);
        finish();
    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .build(),APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==APP_REQUEST_CODE){
            IdpResponse response=IdpResponse.fromResultIntent(data);
            if(resultCode==RESULT_OK){
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
            }else{
                Toast.makeText(this,"Failed to sign in",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if(listener !=null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }
}