package ecg.ecgsimple;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SecondActivity extends AppCompatActivity {

    TextView tv = null;
    BluetoothAdapter btAdapter = null;
    BluetoothSocket btSocket = null;
    BluetoothDevice btDevice = null;
    private ConnectedThread th = null;
    private ProgressDialog progress;
    private boolean estaConectado = false;
    public long plotar = 1;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//SPP depois te explico  melhor
    //eu havia te falado pra gerar um número aleatório, mas no nosso caso não poderemos utilizar
    //esse é um número especial e alguns dispositivos só funcionam com esse UUID SPP (caso do nosso hc-05)

    final int MESSAGE_READ = 9999;
    StringBuilder sb = null;

    private static final int HISTORY_SIZE = 60; // Tamanho da Amostra
    private XYPlot ecgplot = null; // Declara o plot Vazio
    private SimpleXYSeries ecgserie = null; // Declara a Serie Vazia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_second);

        if (findBT()) {
            sb = new StringBuilder();
            Log.i("CONNECT","ANTES");
            Connect();
            Log.i("CONNECT","DEPOIS");
        }

        ecgplot = (XYPlot) findViewById(R.id.ecgplot);
        ecgserie = new SimpleXYSeries("ecgserie"); // Nome da Serie
        ecgplot.addSeries(ecgserie, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null)); // Formatação da Linha
        ecgplot.setRangeBoundaries(0, 100, BoundaryMode.FIXED); // Define os valores fixos do Eixo Y
        ecgserie.useImplicitXVals();
        ecgplot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (th.mhandler != null) {
            th.cancel();
        }
    }

    public void play (View view) {
        try {
            Intent it = new Intent(this, SecondActivity.class);
            finish();
            startActivity(it);
        } catch (Exception e){
            Log.i("ERRO!", "NO PLAY");
        }
    }
    public void pause (View view) {
        try {
            th.mmInStream.close();
        } catch (Exception e){
            Log.i("ERRO!", "NO PAUSE");
        }
    }

    boolean findBT() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Não existe bluetooth disponível no equipamento", Toast.LENGTH_SHORT).show();
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Log.i("BLUETOOTH", "antes pairedDevices");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) {
                    Log.i("BLUETOOTH", device.getName());
                    btDevice = device;
                    return true;
                }
            }
        }
        //Toast.makeText(this,"Bluetooth Device Found", Toast.LENGTH_SHORT).show();
        return false;
    }

    //Uma AsyncTask é semelhante a uma Thread (te explico melhor na quarta)
    //O Android aconselha a colocar todos os métodos que fazem acesso à rede em uma thread ou AsyncTask
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean conectou = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(SecondActivity.this, "Conectando", "Aguarde");
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !estaConectado) {

                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection

                    btSocket.connect();//start connection
                    estaConectado = true;
                }
            } catch (IOException e) {
                Log.i("CONNECT","ERRO CONEXAO");
                conectou = false;//if the try failed, you can check the exception here
                estaConectado = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (conectou) {
                Log.i("BLUETOOTH", "Connected.");
                //  isBtConnected = true;
                btAdapter.cancelDiscovery();
                th = new ConnectedThread(btSocket, mHandler);
                th.start();

            }
            progress.dismiss();
        }
    }

    void Connect() {
        ConnectBT bt = new ConnectBT();
        bt.execute((Void) null);
    }

    //código retirado e adaptado do tutorial do Bluetooth no próprio site do Android SDK
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final android.os.Handler mhandler;

        public ConnectedThread(BluetoothSocket socket, android.os.Handler mHandler) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.mhandler = mHandler;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();

                tmpOut = socket.getOutputStream();
                //Log.i("MSG", "conseguiu pegear stream");
            } catch (IOException e) {
                //Log.i("MSG", "EXCECAO GETINPUTSTREAM");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            // bytes returned from read()
            int available = 0;
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    available = mmInStream.available();
                    if (available > 0) {
                        byte[] buffer = new byte[available];  // buffer store for the stream
                        int bytes = mmInStream.read(buffer);

                        String str = new String(buffer);
                        sb.append(str);

                        int endOfLineIndex = sb.indexOf("~");
                        if (endOfLineIndex==0)
                            sb.delete(0,1);

                        while ((endOfLineIndex=sb.indexOf("~")) > 0) {
                            String sbprint = sb.substring(0, endOfLineIndex);
                            sb.delete(0,endOfLineIndex);
                        /*
                        A variável sbprint nesse momento possui o valor recebido pela porta serial através do Bluetooth
                        Deve-se utilizar os valores recebidos aqui e plotá-los em um gráfico
                         */
                            // Log.i("BLUETOOTH",sbprint);
                            //tv.setText(sbprint); //Lugar onde o valor recebido pelo Bluetooth é apresentado no TextView


                            Log.i("BYTES", "ENTROU");
                            //envia a mensagem através do Handler
                            if (bytes > 0)
                                mhandler.obtainMessage(MESSAGE_READ, sbprint.length(), -1, sbprint).sendToTarget();
                        }
                    }
                } catch (IOException e) {

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private final android.os.Handler mHandler = new android.os.Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ: {
                    //byte[] data = (byte[]) msg.obj;
                    //String str = new String(data, 0, msg.arg1);
                    String str = (String) msg.obj;
                    //String str = new String (data);
                    //Log.i("NOVO",str);
                    //sb.append(str);
                    //Log.i("VALOR2","ENTROU");
                    //String str = msg.toString();

                    /*if (plotar = false) {
                        Log.i("recebeu pause", "plotar = false");
                        ecgplot.willNotDraw();
                        //ecgplot.wait(?);
                    } else {
                        Log.i("recebeu play", "plotar = true");*/
                        if (ecgserie.size() > HISTORY_SIZE) {
                            ecgserie.removeFirst();
                        }
                        //Log.i("VALOR",sbprint);
                        ecgserie.addLast(null, Float.parseFloat(str));
                        ecgplot.redraw();
                    //}
                    break;
                }
            }
        }
    };

}

