
package com.reactlibrary;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.printer.sdk.PrinterConstants;
import com.printer.sdk.PrinterInstance;
import com.printer.sdk.usb.USBPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RNPrinterModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
  public static boolean isConnected = false;// 蓝牙连接状态
  private List<UsbDevice> deviceList;
  public static String devicesName = "未知设备";
  public static PrinterInstance myPrinter;
  private Context mContext;
  private static String devicesAddress;
  private HandlerThread mHandlerThread;
  private Handler mHandler;
  private static UsbDevice mUSBDevice;
  public RNPrinterModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    mContext=reactContext;
  }

  @Override
  public String getName() {
    return "RNPrinter";
  }
  @Override
  public void initialize() {
    ReactContext reactContext = getReactApplicationContext();
    mHandlerThread = new HandlerThread("print_module");
    mHandlerThread.start();

    mHandler = new Handler(mHandlerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case PrinterConstants.Connect.SUCCESS:
            isConnected = true;
            GlobalContants.ISCONNECTED = isConnected;
            GlobalContants.DEVICENAME = devicesName;
            break;
          case PrinterConstants.Connect.FAILED:
            isConnected = false;

            // Toast.makeText(mContext,"打印机连接失败", Toast.LENGTH_SHORT).show();
            // XLog.i(TAG, "ZL at SettingActivity Handler() 连接失败!");
            break;
          case PrinterConstants.Connect.CLOSED:
            isConnected = false;
            GlobalContants.ISCONNECTED = isConnected;
            GlobalContants.DEVICENAME = devicesName;
            // Toast.makeText(mContext,"打印机已经关闭", Toast.LENGTH_SHORT).show();
            // XLog.i(TAG, "ZL at SettingActivity Handler() 连接关闭!");
            break;
          case PrinterConstants.Connect.NODEVICE:
            isConnected = false;
            // Toast.makeText(mContext, "没发现打印机", Toast.LENGTH_SHORT).show();
            break;
          default:
            break;
        }

      }

    };
    UsbManager manager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
    usbAutoConn(manager);
  }

  @ReactMethod
  public void printBodyInfo(ReadableMap bodyInfo) {
    XTUtils.printNote(bodyInfo, myPrinter);

  }
  @SuppressLint({ "InlinedApi", "NewApi" })
  public void usbAutoConn(UsbManager manager) {
    doDiscovery(manager);
    if (deviceList.isEmpty()) {
      // Toast.makeText(mContext,"打印机未连接", 0).show();
      return;
    }
    mUSBDevice = deviceList.get(0);
    if (mUSBDevice == null) {
      mHandler.obtainMessage(PrinterConstants.Connect.FAILED).sendToTarget();
      return;
    }
    myPrinter = PrinterInstance.getPrinterInstance(mContext, mUSBDevice, mHandler);
    devicesName = mUSBDevice.getDeviceName();
    devicesAddress = "vid: " + mUSBDevice.getVendorId() + "  pid: " + mUSBDevice.getProductId();
    UsbManager mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    if (mUsbManager.hasPermission(mUSBDevice)) {
      myPrinter.openConnection();
    } else {
      // 没有权限询问用户是否授予权限
      PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
      IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
      filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
      filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
      mContext.registerReceiver(mUsbReceiver, filter);
      mUsbManager.requestPermission(mUSBDevice, pendingIntent); // 该代码执行后，系统弹出一个对话框
    }

  }
  @SuppressLint("NewApi")
  private void doDiscovery(UsbManager manager) {
    HashMap<String, UsbDevice> devices = manager.getDeviceList();
    deviceList = new ArrayList<UsbDevice>();
    for (UsbDevice device : devices.values()) {
      if (USBPort.isUsbPrinter(device)) {
        deviceList.add(device);
      }
    }

  }



  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @SuppressLint("NewApi")
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.w("printer", "receiver action: " + action);
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          mContext.unregisterReceiver(mUsbReceiver);
          UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                  && mUSBDevice.equals(device)) {
            myPrinter.openConnection();
          } else {
            mHandler.obtainMessage(PrinterConstants.Connect.FAILED).sendToTarget();
            Log.e("printer", "permission denied for device " + device);
          }
        }
      }
    }
  };

}