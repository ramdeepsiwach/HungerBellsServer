package com.se_p2.hungerbellsserver.services;

import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.se_p2.hungerbellsserver.MainActivity;
import com.se_p2.hungerbellsserver.common.Common;

import java.util.Map;
import java.util.Random;

import androidx.annotation.NonNull;

public class MyFCMServices extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String,String> dataRecv=remoteMessage.getData();
        if(dataRecv!=null){
            if(dataRecv.get(Common.NOTI_TITLE).equals("New Order")){
                Intent intent=new Intent(this, MainActivity.class);
                intent.putExtra(Common.IS_OPEN_ORDER_ACTIVITY,true);
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        intent);
            }else {
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this,s,true,false);
    }
}
