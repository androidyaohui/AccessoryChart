package com.tcl.navigator.accessorychart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

public class OpenAccessoryReceiver extends BroadcastReceiver {

    private OpenAccessoryListener mOpenAccessoryListener;

    public OpenAccessoryReceiver(OpenAccessoryListener openAccessoryListener) {
        mOpenAccessoryListener = openAccessoryListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbAccessory usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (usbAccessory != null) {
                mOpenAccessoryListener.openAccessoryModel(usbAccessory);
            } else {
                mOpenAccessoryListener.openAccessoryError();
            }
        } else {
            mOpenAccessoryListener.openAccessoryError();
        }
    }

    public interface OpenAccessoryListener {
        /**
         * 打开Accessory模式
         *
         * @param usbAccessory
         */
        void openAccessoryModel(UsbAccessory usbAccessory);

        /**
         * 打开设备(手机)失败
         */
        void openAccessoryError();
    }
}
