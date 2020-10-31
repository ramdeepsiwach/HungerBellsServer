package com.se_p2.hungerbellsserver.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.FirebaseDatabase;
import com.se_p2.hungerbellsserver.R;
import com.se_p2.hungerbellsserver.model.CategoryModel;
import com.se_p2.hungerbellsserver.model.FoodModel;
import com.se_p2.hungerbellsserver.model.OrderModel;
import com.se_p2.hungerbellsserver.model.ServerUser;
import com.se_p2.hungerbellsserver.model.TokenModel;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;

public class Common {
    public static final String SERVER_REF = "Server";
    public static final String CATEGORY_REF ="Category" ;
    public static final String ORDER_REF ="Order" ;
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT = "content";
    public static final String TOKEN_REF = "Tokens";
    public static final String SHIPPER_REF = "Shipper";
    public static final String SHIPPING_ORDER_REF ="ShippingOrder";
    public static final String IS_OPEN_ORDER_ACTIVITY = "IsOpenOrderActivity";
    public static final String IS_SEND_IMAGE ="IS_SEND_IMAGE" ;
    public static final String IMAGE_URL ="IMAGE_URL" ;

    public static ServerUser currentServerUser;
    public static CategoryModel categorySelected;
    public static FoodModel selectedFood;
    public static OrderModel currentOrderSelected;

    public enum ACTION{
        CREATE,
        UPDATE,
        DELETE
    }

    public static void setSpanStringColor(String s, String format, TextView txt_time, int Color) {
        SpannableStringBuilder builder=new SpannableStringBuilder();
        builder.append(s);
        SpannableString spannableString=new SpannableString(format);
        StyleSpan boldSpan=new StyleSpan(Typeface.BOLD);
        spannableString.setSpan(new ForegroundColorSpan(Color),0,format.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(spannableString);
        txt_time.setText(builder,TextView.BufferType.SPANNABLE);
    }

    public static String convertStatusToString(int orderStatus) {
        switch (orderStatus){
            case 0:
                return "Placed";
            case 1:
                return "Shipping";
            case 2:
                return "Shipped";
            case -1:
                return "Cancelled";
            default:
                return "Error";
        }
    }

    public static String createTopicOrder() {
        return "/topics/new_order";
    }

    public static void updateToken(Context context, String newToken,boolean isServer,boolean isShipper) {
        if(Common.currentServerUser !=null){
            FirebaseDatabase.getInstance()
                    .getReference(Common.TOKEN_REF)
                    .child(Common.currentServerUser.getUid())
                    .setValue(new TokenModel(Common.currentServerUser.getPhone(),newToken,isServer,isShipper))
                    .addOnFailureListener(e -> {
                        Toast.makeText(context,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public static void showNotification(Context context, int id, String title, String content, Intent intent) {
        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        String NOTIFICATION_CHANNEL_ID="hunger_bells";
        NotificationManager notificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel=new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Hunger Bell",NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Hunger Bells");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_logo)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_restaurant_menu_24));
        if (pendingIntent!=null){
            builder.setContentIntent(pendingIntent);
        }
        Notification notification=builder.build();
        notificationManager.notify(id,notification);

    }

    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index = 0, len = encoded.length();

        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    public static float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    public static String getNewsTopic() {
        return new StringBuilder("/topics/news").toString();
    }
}
