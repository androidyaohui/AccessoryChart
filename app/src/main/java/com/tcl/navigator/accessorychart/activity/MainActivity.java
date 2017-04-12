package com.tcl.navigator.accessorychart.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.tcl.navigator.accessorychart.R;
import com.tcl.navigator.accessorychart.receiver.OpenAccessoryReceiver;
import com.tcl.navigator.accessorychart.receiver.UsbDetachedReceiver;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OpenAccessoryReceiver.OpenAccessoryListener, View.OnClickListener, UsbDetachedReceiver.UsbDetachedListener {

    private static final int    SEND_MESSAGE_SUCCESS     = 0;
    private static final int    RECEIVER_MESSAGE_SUCCESS = 1;
    private static final String USB_ACTION               = "com.tcl.navigator.accessorychart";
    private TextView              mLog;
    private EditText              mMessage;
    private Button                mSend;
    private UsbManager            mUsbManager;
    private OpenAccessoryReceiver mOpenAccessoryReceiver;
    private ParcelFileDescriptor  mParcelFileDescriptor;
    private FileInputStream       mFileInputStream;
    private FileOutputStream      mFileOutputStream;
    private ExecutorService       mThreadPool;
    private byte[]       mBytes        = new byte[1024];
    private StringBuffer mStringBuffer = new StringBuffer();
    private UsbDetachedReceiver mUsbDetachedReceiver;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_MESSAGE_SUCCESS:
                    mMessage.setText("");
                    mMessage.clearComposingText();
                    break;

                case RECEIVER_MESSAGE_SUCCESS:
                    mLog.setText(mStringBuffer.toString());
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mLog = (TextView) findViewById(R.id.log);
        mMessage = (EditText) findViewById(R.id.message);
        mSend = (Button) findViewById(R.id.send);
    }

    private void initListener() {
        mSend.setOnClickListener(this);
    }

    private void initData() {
        mSend.setEnabled(false);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mThreadPool = Executors.newFixedThreadPool(3);

        mUsbDetachedReceiver = new UsbDetachedReceiver(this);
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbDetachedReceiver, filter);

        mOpenAccessoryReceiver = new OpenAccessoryReceiver(this);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(USB_ACTION), 0);
        IntentFilter intentFilter = new IntentFilter(USB_ACTION);
        registerReceiver(mOpenAccessoryReceiver, intentFilter);

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory usbAccessory = (accessories == null ? null : accessories[0]);
        if (usbAccessory != null) {
            if (mUsbManager.hasPermission(usbAccessory)) {
                openAccessory(usbAccessory);
            } else {
                mUsbManager.requestPermission(usbAccessory, pendingIntent);
            }
        }
    }

    /**
     * 打开Accessory模式
     *
     * @param usbAccessory
     */
    private void openAccessory(UsbAccessory usbAccessory) {
        mParcelFileDescriptor = mUsbManager.openAccessory(usbAccessory);
        if (mParcelFileDescriptor != null) {
            FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();
            mFileInputStream = new FileInputStream(fileDescriptor);
            mFileOutputStream = new FileOutputStream(fileDescriptor);
            mSend.setEnabled(true);

            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    int i = 0;
                    while (i >= 0) {
                        try {
                            i = mFileInputStream.read(mBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                        if (i > 0) {
                            mStringBuffer.append(new String(mBytes, 0, i) + "\n");
                            mHandler.sendEmptyMessage(RECEIVER_MESSAGE_SUCCESS);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void openAccessoryModel(UsbAccessory usbAccessory) {
        openAccessory(usbAccessory);
    }

    @Override
    public void openAccessoryError() {

    }

    @Override
    public void onClick(View v) {
        final String mMessageContent = mMessage.getText().toString();
        if (!TextUtils.isEmpty(mMessageContent)) {
            mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mFileOutputStream.write(mMessageContent.getBytes());
                        mHandler.sendEmptyMessage(SEND_MESSAGE_SUCCESS);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void usbDetached() {
        finish();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();

        unregisterReceiver(mOpenAccessoryReceiver);
        unregisterReceiver(mUsbDetachedReceiver);
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileInputStream != null) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
