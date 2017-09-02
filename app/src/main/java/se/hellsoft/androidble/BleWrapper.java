package se.hellsoft.androidble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public final class BleWrapper implements Handler.Callback {
  private static final int MSG_CONNECT = 10;
  private static final int MSG_CONNECTED = 20;
  private static final int MSG_DISCONNECT = 30;
  private static final int MSG_DISCONNECTED = 40;
  private static final int MSG_CLOSE = 50;

  private final BluetoothDevice bluetoothDevice;
  private Handler bleHandler;
  private Context context;
  private MyBleCallback myBleCallback = new MyBleCallback();

  public BleWrapper(Context context, String deviceAddress) {
    this.context = context;
    HandlerThread handlerThread = new HandlerThread("BleThread");
    handlerThread.start();
    BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    bleHandler = new Handler(handlerThread.getLooper(), this);
    bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
  }

  public void connect(boolean autoConncet) {
    bleHandler.obtainMessage(MSG_CONNECT, autoConncet).sendToTarget();
  }

  @Override
  public boolean handleMessage(Message message) {
    switch (message.what) {
      case MSG_CONNECT:
        doConnect((Boolean) message.obj);
        break;
      case MSG_CONNECTED:
        doDiscoverServices((BluetoothGatt) message.obj);
        break;
      case MSG_DISCONNECT:
        doDisconnect((BluetoothGatt) message.obj);
        break;
      case MSG_DISCONNECTED:
        doClose((BluetoothGatt) message.obj);
        break;
    }
    return true;
  }

  private void doConnect(boolean autoConnect) {
    bluetoothDevice.connectGatt(context, autoConnect, myBleCallback);
  }

  private void doClose(BluetoothGatt gatt) {
    gatt.close();
  }

  private void doDisconnect(BluetoothGatt gatt) {
    gatt.disconnect();
  }

  private void doDiscoverServices(BluetoothGatt gatt) {
    gatt.discoverServices();
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
  }
}
