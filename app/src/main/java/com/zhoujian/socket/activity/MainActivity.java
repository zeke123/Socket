package com.zhoujian.socket.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zhoujian.socket.R;
import com.zhoujian.socket.service.TcpService;
import com.zhoujian.socket.utils.MyUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 Socket可以实现进程间通信，Socket称为"套接字"，它分为流式套接字和用户数据套接字

 分别对应网络中的TCP和UDP协议

 TCP协议是面向连接的协议，提供稳定的双向通信功能，TCP连接的建立是通过三次握手才能完成，稳定性高

 效率相对UDP较低

 UDP协议是面向无连接的，提供单向通信功能，效率高，不保证数据一定能够正确传输

 Android4.0以后不能在主线程中请求网络，会抛出异常NetworkOnMainThreadException

 */

public class MainActivity extends Activity  {

    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;
    @InjectView(R.id.msg_container)
    TextView mMsgContainer;
    @InjectView(R.id.msg)
    EditText mMsg;
    @InjectView(R.id.send)
    Button mSend;
    private PrintWriter mPrintWriter;
    private Socket mClientSocket;


    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_RECEIVE_NEW_MSG:
                {
                    mMsgContainer.setText(mMsgContainer.getText() + (String) msg.obj);
                    break;
                }
                case MESSAGE_SOCKET_CONNECTED: {
                    mSend.setEnabled(true);
                    break;
                }
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        clickEent();
        Intent service = new Intent(this, TcpService.class);
        startService(service);
        new Thread()
        {
            @Override
            public void run()
            {
                connectTCPServer();
            }
        }.start();
    }

    private void clickEent()
    {
        mSend.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (view== mSend)
                {
                    final String msg = mMsg.getText().toString();
                    if (!TextUtils.isEmpty(msg) && mPrintWriter != null)
                    {
                        mPrintWriter.println(msg);
                        mMsg.setText("");
                        String time = formatDateTime(System.currentTimeMillis());
                        final String showedMsg = "客户端" + time + ":" + msg + "\n";
                        mMsgContainer.setText(mMsgContainer.getText() + showedMsg);
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private String formatDateTime(long time)
    {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectTCPServer()
    {
        Socket socket = null;
        while (socket == null)
        {
            try
            {
                socket = new Socket("localhost", 8088);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())), true);
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
            }
            catch (IOException e)
            {
                SystemClock.sleep(1000);
            }
        }
        try
        {
            // 接收服务器端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing())
            {
                String msg = br.readLine();
                if (msg != null) {
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "服务端" + time + ":" + msg + "\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG, showedMsg).sendToTarget();
                }
            }
            MyUtils.close(mPrintWriter);
            MyUtils.close(br);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
