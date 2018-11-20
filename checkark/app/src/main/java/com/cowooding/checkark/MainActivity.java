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
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ConstraintLayout layout_main;
    TextView tv_check;

    private FirebaseDatabase mDatabase;

    private static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    String key;
    ArrayList<String> server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance();

        layout_main = (ConstraintLayout)findViewById(R.id.layout_main);
        tv_check = (TextView) findViewById(R.id.tv_check);

        //오래 걸리는 작업인지 2개를 하거나 위치를 옮기면 문제가 발생
        getCheckList();

        DatabaseReference mReference = mDatabase.getReference("docs/titles"); // 변경값을 확인할 child 이름
        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String token = FirebaseInstanceId.getInstance().getToken();

                if((token = FirebaseInstanceId.getInstance().getToken())!=null) {
                    setToken(token);
                }

                String time = getTime();
                String month = time.substring(5,7);
                String day = time.substring(8,10);

                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    final String title = messageData.getValue().toString();
                    //오늘 날짜의 점검이 있는지 체크
                    if(title.contains(month+"월"+" "+day+"일") && title.contains("점검")) {
                        //완료되었으면
                        if(title.contains("완료")) {
                            tv_check.setText("점검 완료");
                            layout_main.setBackgroundColor(Color.rgb(34, 164, 69));
                            //관리자 폰일 때 메세지 전송
                            if ((token = FirebaseInstanceId.getInstance().getToken()) != null && token.equals("eDjOtS6mUJQ:APA91bGDlQa2YCvdRXP3yFCB0lR_d0oZuh_EKt32Fee28mnZF2kZSFSWCeYMuTAZkrlL_X2EerX5CP48bRFkZeLUCQw7v53_hdF0s922JEHHABKssi0soGPwDa-g-4nKpCriaktcG6-3"))
                            {
                                NotiCheck(title);
                            }
                            //점검이 완료되면 더이상 db를 검사하지 않음
                            break;
                        }
                        //완료되지 않았으면
                        else{
                            tv_check.setText("점검중");
                            layout_main.setBackgroundColor(Color.rgb(212,42,35));
                            //점검중이면 더이상 db를 검사하지 않음
                            break;
                        }
                    }
                    //오늘 날짜의 점검은 없지만 어제 날짜의 점검이 있는 경우
                    else if((title.contains(month+"월"+" "+(Integer.valueOf(day)-1)+"일"))&& title.contains("점검")){
                        //어제 점검이 오늘 완료 되었으면
                        if(title.contains("완료"))
                        {
                            tv_check.setText("점검 완료");
                            layout_main.setBackgroundColor(Color.rgb(34, 164, 69));
                            //관리자 폰일 때 메세지 전송
                            if ((token = FirebaseInstanceId.getInstance().getToken()) != null && token.equals("eDjOtS6mUJQ:APA91bGDlQa2YCvdRXP3yFCB0lR_d0oZuh_EKt32Fee28mnZF2kZSFSWCeYMuTAZkrlL_X2EerX5CP48bRFkZeLUCQw7v53_hdF0s922JEHHABKssi0soGPwDa-g-4nKpCriaktcG6-3"))
                            {
                                NotiCheck(title);
                            }
                            //점검이 완료되면 더이상 db를 검사하지 않음
                            break;
                        }
                        //완료되지 않았으면
                        else {
                            tv_check.setText("점검중");
                        }
                    }
                    //크롤러로 공지를 최신화하지 않았거나 오늘도 어제도 점검이 없는 경우
                    else{
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
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference();

        databaseReference.child("tokens").child(token).setValue(token);
    }

    private void getServerKey(){
        //서버키 받기
        DatabaseReference mReference_server_key = mDatabase.getReference("serverkey");
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
    }

    private  void getCheckList(){
        server = new ArrayList<String>();
        //서버리스트 받기
        DatabaseReference mReference_server = mDatabase.getReference("server");
        mReference_server.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                server.clear();
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    String serverlist = messageData.getValue().toString();
                    server.add(serverlist);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private  void NotiCheck(String title){
        String server_name="",check_name="";
        int div=0;
        //서버가 포함되어있는지 체크
        for(int i=0;i<server.size();i++){
            if(server.get(i).equals("정기")) { //정기로 서버 이름과 점검 유형을 구분하므로 db에서 정기가 항상 앞에 있어야한다
                div = i;
                break;
            }
            if(title.contains(server.get(i))){ //서버 이름이 여러개 호명 될 경우 가장 앞에 언급된 서버를 언급
                server_name = server.get(i);
                break;
            }
        }
        //점검 제목이 포함되어있는지 체크
        for(int i=div;i<server.size();i++){ //동적으로 대응하도록 작성
            if(title.contains(server.get(i))){
                check_name = server.get(i);
            }
        }
        //서버와 점검 유형을 넘겨줌
        sendPostToFCM(server_name,check_name);
    }

    private void sendPostToFCM(final String server_name, final String check_name) {
        getServerKey();
        //토큰들 참조하여 메세지 보냄
        DatabaseReference mReference_msg = mDatabase.getReference("tokens");
        mReference_msg.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    //final String user_token = "eDjOtS6mUJQ:APA91bGDlQa2YCvdRXP3yFCB0lR_d0oZuh_EKt32Fee28mnZF2kZSFSWCeYMuTAZkrlL_X2EerX5CP48bRFkZeLUCQw7v53_hdF0s922JEHHABKssi0soGPwDa-g-4nKpCriaktcG6-3"; //테스트
                    final String user_token = messageData.getValue().toString();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // FMC 메시지 생성 start
                                JSONObject root = new JSONObject();
                                JSONObject notification = new JSONObject();
                                notification.put("body", server_name + " " + check_name + " " + "점검이 완료되었습니다.");
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