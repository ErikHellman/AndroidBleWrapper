package se.hellsoft.androidble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;

import java.util.HashSet;
import java.util.Set;

public final class BleWrapper implements Handler.Callback {
  private static final int MSG_CONNECT = 10;
  private static final int MSG_CONNECTED = 20;
  private static final int MSG_DISCONNECT = 30;
  private static final int MSG_DISCONNECTED = 40;
  private static final int MSG_SERVICES_DISCOVERED = 50;

  private BluetoothDevice bluetoothDevice;
  private Handler mainHandler = new Handler(Looper.getMainLooper(), this);
  private Handler bleHandler;
  private Context context;
  private MyBleCallback myBleCallback = new MyBleCallback();

  private Set<BleCallback> listeners = new HashSet<>();

  public interface BleCallback {
    /**
     * Signals that the BLE device is ready for communication.
     */
    @MainThread
    void onDeviceReady();

    /**
     * Signals that a connection to the device was lost.
     */
    @MainThread
    void onDeviceDisconnected();
  }

  public BleWrapper(Context context, String deviceAddress) {
    this.context = context;
    HandlerThread handlerThread = new HandlerThread("BleThread");
    handlerThread.start();
    bleHandler = new Handler(handlerThread.getLooper(), this);
    bluetoothDevice = getBluetoothDevice(context, deviceAddress);
  }

  @MainThread
  public void addListener(BleCallback bleCallback) {
    listeners.add(bleCallback);
  }

  @MainThread
  public void removeListener(BleCallback bleCallback) {
    listeners.remove(bleCallback);
  }

  private BluetoothDevice getBluetoothDevice(Context context, String deviceAddress) {
    BluetoothManager bluetoothManager = (BluetoothManager) context
        .getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    return bluetoothAdapter.getRemoteDevice(deviceAddress);
  }

  public void connect(boolean autoConncet) {
    bleHandler.obtainMessage(MSG_CONNECT, autoConncet).sendToTarget();
  }

  public void disconnect() {
    bleHandler.obtainMessage(MSG_DISCONNECT, bluetoothDevice).sendToTarget();
  }

  @Override
public boolean handleMessage(Message message) {
  switch (message.what) {
    case MSG_CONNECT:
      doConnect((Boolean) message.obj);
      break;
    case MSG_CONNECTED:
      ((BluetoothGatt) message.obj).discoverServices();
      break;
    case MSG_DISCONNECT:
      ((BluetoothGatt) message.obj).disconnect();
      break;
    case MSG_DISCONNECTED:
      ((BluetoothGatt) message.obj).close();
      break;
    case MSG_SERVICES_DISCOVERED:
      doNotifyReady();
  }
  return true;
}

  @MainThread
  private void doNotifyReady() {
    for (BleCallback listener : listeners) {
      listener.onDeviceReady();
    }
  }

  private void doConnect(boolean autoConnect) {
    bluetoothDevice.connectGatt(context, autoConnect, myBleCallback);
  }

  private class MyBleCallback extends BluetoothGattCallback {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      super.onConnectionStateChange(gatt, status, newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        switch (newState) {
          case BluetoothGatt.STATE_CONNECTED:
            bleHandler.obtainMessage(MSG_CONNECTED, gatt).sendToTarget();
            break;
          case BluetoothGatt.STATE_DISCONNECTED:
            bleHandler.obtainMessage(MSG_DISCONNECTED, gatt).sendToTarget();
            break;
        }
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      super.onServicesDiscovered(gatt, status);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        mainHandler.sendEmptyMessage(MSG_SERVICES_DISCOVERED);
      }
    }
  }
}
