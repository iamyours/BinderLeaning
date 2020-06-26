package com.iamyours.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import com.iamyours.bean.Student;
import com.iamyours.interfaces.ISayHello;
import com.iamyours.interfaces.ISayHelloStub;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                query();
            }
        });
        bindServices();
    }

    private ScoreProxy scoreProxy;

    private void bindServices() {
        Intent intent = new Intent();
        intent.setAction("com.iamyours.score");
        intent.setPackage("com.iamyours.server");//Server端applicationId
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.e("Client", "onServiceConnected");
                scoreProxy = new ScoreProxy(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e("Client", "onServiceDisconnected:");

            }
        }, BIND_AUTO_CREATE);

    }

    private void query() {
        Student s = new Student("张三");
        int result = scoreProxy.query(s);
        Log.e("client", "result:" + result);
        s = new Student("李四");
        result = scoreProxy.query(s);
        Log.e("client", "result:" + result);
        s = new Student("马云");
        result = scoreProxy.query(s);
        Log.e("client", "result:" + result);
    }
}
