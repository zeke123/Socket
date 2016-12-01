package com.zhoujian.socket.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.zhoujian.socket.utils.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by zhoujian on 2016/11/30.
 */

public class TcpService extends Service
{
    private boolean mIsServiceDestoryed = false;

    private String[] mDefinedMessages = new String[] {
            "你好?",
            "请问你叫什么名字呀？",
            "吃饭了吗",
            "今天可以一起去看电影啊",
            "你的快递收到了吗",
            "今天北京的天气不错啊",
            "明天周末准备到哪玩啊？"
    };

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestoryed = true;
        super.onDestroy();
    }

    private class TcpServer implements Runnable {

        @SuppressWarnings("resource")
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8088);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            while (!mIsServiceDestoryed) {
                try {
                    // 接受客户端请求
                    final Socket client = serverSocket.accept();
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                    }.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void responseClient(Socket client) throws IOException {
        // 用于接收客户端消息
        BufferedReader in = new BufferedReader(new InputStreamReader(
                client.getInputStream()));
        // 用于向客户端发送消息
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream())), true);
        out.println("欢迎来到聊天室！");
        while (!mIsServiceDestoryed) {
            String str = in.readLine();
            if (str == null) {
                break;
            }
            int i = new Random().nextInt(mDefinedMessages.length);
            String msg = mDefinedMessages[i];
            out.println(msg);
        }
        // 关闭流
        Utils.close(out);
        Utils.close(in);
        client.close();
    }
}
