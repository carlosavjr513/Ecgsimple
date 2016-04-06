package ecg.ecgsimple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class SecondActivity extends AppCompatActivity {

    TextView valor;
    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket;
    BluetoothDevice btDevice;
    Thread workerThread;
    InputStream myInput;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        valor = (TextView) findViewById(R.id.editText);
        try {
            findBT();
            openBT();
        } catch (IOException ex) {
            Log.i("BLUETOOTH", "Erro de Inicialização");
        }
    }

    public void paraPlot (View view) throws IOException {
        closeBT();
        finish();
    }

    void findBT()
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null)
        {
            Toast.makeText(this,"No bluetooth adapter available", Toast.LENGTH_SHORT).show();
        }

        if(!btAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    Log.i("BLUETOOTH",device.getName());
                    btDevice = device;
                    break;
                }
            }
        }
        Toast.makeText(this,"Bluetooth Device Found", Toast.LENGTH_SHORT).show();
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("46ee2234-77e2-44ac-b2fb-6e605db96743B"); //Standard SerialPortService ID
        btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
        btSocket.connect();
        myInput = btSocket.getInputStream();

        beginListenForData();
        Toast.makeText(this,"Bluetooth Opened", Toast.LENGTH_SHORT).show();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler() {
            @Override
            public void close() {

            }

            @Override
            public void flush() {

            }

            @Override
            public void publish(LogRecord record) {

            }
        };
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = myInput.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            myInput.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            myLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        myInput.close();
        btSocket.close();
        Log.i("BLUETOOTH", "BT Encerrado");
        Toast.makeText(this,"Bluetooth Closed", Toast.LENGTH_SHORT).show();
    }
}
