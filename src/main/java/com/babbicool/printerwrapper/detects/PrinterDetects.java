package com.babbicool.printerwrapper.detects;

import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.babbicool.lib.printerwrapper.Printer;
import com.babbicool.lib.printerwrapper.utils.BluetoothHandler;
import com.babbicool.lib.printerwrapper.utils.BluetoothService;
import com.datecs.api.printer.ProtocolAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PrinterDetects extends Printer{


    private BluetoothService bluetoothService;
    private BluetoothHandler handler;
    private BluetoothDevice bluetoothDevice;
    private boolean isPrinterConnected = false;
    private com.datecs.api.printer.Printer mPrinterDatecs;
    private ProtocolAdapter mProtocolAdapter;
    private ProtocolAdapter.Channel mPrinterChannel;

    @Override
    public boolean isConnected() {
        return isPrinterConnected;
    }

    @Override
    public void connect(String address) {

        if(isPrinterConnected)
            return ;

        if(bluetoothService==null){
            handler = new BluetoothHandler(getContext(),listener);
            bluetoothService = new BluetoothService(getContext(),handler);
        }

        bluetoothDevice = bluetoothService.getDevByMac(address);
        bluetoothService.connect(bluetoothDevice);
    }

    @Override
    public void connectByName(String name) {

        if(isPrinterConnected)
            return ;

        if(bluetoothService==null){
            handler = new BluetoothHandler(getContext(),listener);
            bluetoothService = new BluetoothService(getContext(),handler);
        }

        bluetoothDevice = bluetoothService.getDevByName(name);
        bluetoothService.connect(bluetoothDevice);
    }

    private BluetoothHandler.HandlerInterfce listener = new BluetoothHandler.HandlerInterfce() {
        @Override
        public void onDeviceConnected() {
            isPrinterConnected = true;
            try {
                initPrinter(
                        bluetoothService.getConnectedThread().getInStream(),
                        bluetoothService.getConnectedThread().getOutStream()
                );
                getPrinterConnectedListener().onPrinterConnected(bluetoothDevice.getAddress());
            } catch (IOException e) {
                e.printStackTrace();

                bluetoothService.stop();

                if(getPrinterConnectedListener()!=null)
                    getPrinterConnectedListener().onError();
                isPrinterConnected = false;
            }
        }

        @Override
        public void onDeviceConnecting() {
            Log.d("PrinterDetects", "onDeviceConnecting");
        }

        @Override
        public void onDeviceConnectionLost() {
            Log.d("PrinterDetects", "onDeviceConnectionLost");
            isPrinterConnected = false;
        }

        @Override
        public void onDeviceUnableToConnect() {
            Log.d("PrinterDetects", "onDeviceUnableToConnect");
            if(getPrinterConnectedListener()!=null)
                getPrinterConnectedListener().onError();

            isPrinterConnected = false;

        }
    };

    private void initPrinter(InputStream inputStream, OutputStream outputStream) throws IOException {
        mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
        if (mProtocolAdapter.isProtocolEnabled()) {
            Log.d("PrinterDetects", "Protocol mode is enabled");
            mProtocolAdapter.setPrinterListener(new ProtocolAdapter.PrinterListener() {
                @Override
                public void onThermalHeadStateChanged(boolean overheated) {

                }

                @Override
                public void onPaperStateChanged(boolean hasPaper) {

                }

                @Override
                public void onBatteryStateChanged(boolean lowBattery) {

                }
            });
            // Get printer instance
            mPrinterChannel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
            mPrinterDatecs = new com.datecs.api.printer.Printer(mPrinterChannel.getInputStream(), mPrinterChannel.getOutputStream());


        } else {
            Log.d("PrinterDetects", "Protocol mode is disabled");
            // Protocol mode it not enables, so we should use the row streams.
            mPrinterDatecs = new com.datecs.api.printer.Printer(mProtocolAdapter.getRawInputStream(),
                    mProtocolAdapter.getRawOutputStream());
        }
    }



    @Override
    public void disconnect() {
        mPrinterDatecs = null;
        bluetoothService.stop();
    }

    @Override
    public void printString(String text) {
        try {
            mPrinterDatecs.printTaggedText("{reset}{center}" + text.toString() + "{reset}{center}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printLine() {
        try {
            mPrinterDatecs.feedPaper(15);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printImage(Bitmap bitmap) {

    }

    @Override
    public void reset() {
        try {
            mPrinterDatecs.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
