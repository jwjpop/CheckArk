package com.cowooding.checkark;

import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout layout_main;
    TextView tv_check;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mReference;

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();
    private DatabaseReference mReference_msg;
    private DatabaseReference mReference_server_key;

    private static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    String key;

    String month;
    String day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout_main = (ConstraintLayout)findViewById(R.id.layout_main);
        tv_check = (TextView) findViewById(R.id.tv_check);

        String time = getTime();
        month = time.substring(5,7);
        day = time.substring(8,10);

        mDatabase = FirebaseDatabase.getInstance();
        mReference = mDatabase.getReference("docs/titles"); // 변경값을 확인할 child 이름
        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = FirebaseInstanceId.getInstance().getToken();
                if((token = FirebaseInstanceId.getInstance().getToken())!=null)
                    setToken(token);
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    final String title = messageData.getValue().toString();
                    if(title.contains(month+"월"+" "+day+"일")) {
                        if(title.contains("점검") && title.contains("완료")) {
                            tv_check.setText("점검 완료");
                            layout_main.setBackgroundColor(Color.rgb(34, 164, 69));
                            if ((token = FirebaseInstanceId.getInstance().getToken()) != null && token.equals("eDjOtS6mUJQ:APA91bGDlQa2YCvdRXP3yFCB0lR_d0oZuh_EKt32Fee28mnZF2kZSFSWCeYMuTAZkrlL_X2EerX5CP48bRFkZeLUCQw7v53_hdF0s922JEHHABKssi0soGPwDa-g-4nKpCriaktcG6-3"))
                            {
                                sendPostToFCM();
                            }
                            break;
                        }
                        else if(title.contains("점검")){
                            tv_check.setText("점검 중");
                            layout_main.setBackgroundColor(Color.rgb(212,42,35));
                            break;
                        }
                    }else{
                        tv_check.setText("프로그램이 동작 전이거나\n 오늘 자 점검이 없습니다.");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getTime() {
        long mNow;
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    private void setToken(String token){
        databaseReference.child("tokens").child(token).setValue(token);
    }

    private void sendPostToFCM() {
        //서버키 받기
        mReference_server_key = mDatabase.getReference("serverkey");
        mReference_server_key.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    key = messageData.getValue().toString();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //토큰들 참조하여 메세지 보냄
        mReference_msg = mDatabase.getReference("tokens");
        mReference_msg.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    final String user_token = messageData.getValue().toString();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // FMC 메시지 생성 start
                                JSONObject root = new JSONObject();
                                JSONObject notification = new JSONObject();
                                notification.put("body", "점검이 완료되었습니다.");
                                notification.put("title", getString(R.string.app_name));
                                root.put("notification", notification);
                                root.put("to", user_token);
                                // FMC 메시지 생성 end

                                URL Url = new URL(FCM_MESSAGE_URL);
                                HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setDoOutput(true);
                                conn.setDoInput(true);
                                conn.addRequestProperty("Authorization", "key=" + key);
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setRequestProperty("Content-type", "application/json");
                                OutputStream os = conn.getOutputStream();
                                os.write(root.toString().getBytes("utf-8"));
                                os.flush();
                                conn.getResponseCode();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}