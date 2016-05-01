package ecg.ecgsimple;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class SecondActivity extends AppCompatActivity {

    TextView tv=null;
    BluetoothAdapter btAdapter=null;
    BluetoothSocket btSocket=null;
    BluetoothDevice btDevice=null;
    private SampleDynamicXYDatasource th = null;
    private ProgressDialog progress;
    private boolean estaConectado=false;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//SPP depois te explico  melhor
    //eu havia te falado pra gerar um número aleatório, mas no nosso caso não poderemos utilizar
    //esse é um número especial e alguns dispositivos só funcionam com esse UUID SPP (caso do nosso hc-05)

    final int MESSAGE_READ = 9999;
    StringBuilder sb = null;

    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
            }

            @Override
            public void update(Observable o, Object arg) {
                plot.redraw();
            }
        }

    private XYPlot ecgPlot;
    private MyPlotUpdater plotUpdater;
    SampleDynamicXYDatasource data; // valores do sbprint devem ser jogados aqui via classe SampleDynamicXYDatasource
    private Thread myThread;

    private byte[] mByte;
    dou

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        tv = (TextView) findViewById(R.id.textView);

            if (findBT()) {
                sb = new StringBuilder();
                Connect();
            }

        ecgPlot = (XYPlot) findViewById(R.id.ecgXYplot); // declaração do plot
        plotUpdater = new MyPlotUpdater(ecgPlot); // declaração do atualizador do plot
        ecgPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0")); // formatação do tipo do valor dos eixos

        data = new SampleDynamicXYDatasource(); // declaraçao da serie
        SampleDynamicSeries ecgSerie = new SampleDynamicSeries(data, 0, ""); // alimentaçao da serie

        LineAndPointFormatter formatacao = new LineAndPointFormatter(Color.rgb(0,0,255), null, null, null); // Formatação da serie plotada
        ecgPlot.addSeries(ecgSerie, formatacao); // inserção da serie e sua formatação no Plot
        data.addObserver(plotUpdater); //Atualização dos dados conforme recebimento

        ecgPlot.setRangeValueFormat(new DecimalFormat("##.#")); // Formato dos valores
        ecgPlot.setRangeBoundaries(20, 40, BoundaryMode.FIXED); // Formatação do eixo Y

        // Efeito Visual no Grafico
        DashPathEffect dashFx = new DashPathEffect(new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        ecgPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(dashFx);
        ecgPlot.getGraphWidget().getRangeGridLinePaint().setPathEffect(dashFx);
    }

    @Override
    public void onResume() {
        // kick off the data generating thread:
        myThread = new Thread(data);
        myThread.start();
        super.onResume();
    }

    @Override
    public void onPause() {
        data.stopThread();
        super.onPause();
    }

    // Geração dos valores que a serie recebe, dar um jeito de jogar o sbprint aqui
    class SampleDynamicXYDatasource implements Runnable {

        // encapsulates management of the observers watching this datasource for update events:
        class MyObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final android.os.Handler mhandler;
        private static final int SAMPLE_SIZE = 30;
        private MyObservable notifier;
        private boolean keepRunning = false;

        {
            notifier = new MyObservable();
        }

        public void stopThread() {
            keepRunning = false;
        }

        public SampleDynamicXYDatasource(BluetoothSocket socket, android.os.Handler mHandler) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.mhandler = mHandler;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();

                tmpOut = socket.getOutputStream();
                Log.i("MSG","conseguiu pegar stream");
            } catch (IOException e) {Log.i("MSG","EXCECAO GETINPUTSTREAM"); }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //@Override
        public void run() {
            try {
                int bytes; // bytes returned from read()
                int available=0;
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        available = mmInStream.available();
                        if (available>0) {
                            byte[] buffer = new byte[available];  // buffer store for the stream

                            bytes = mmInStream.read(buffer);

                            //envia a mensagem através do Handler
                            if (bytes > 0)
                            // mhandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int getItemCount(int series) {
            return SAMPLE_SIZE;
        }

        public Number getX(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            return index;
        }

        public Number getY(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }

            byte[] data = (byte[]) mByte;
            String str = ((byte[]) mByte).toString();//new String(data,0,msg.arg1);
            sb.append(str);
            int endOfLineIndex = sb.indexOf("\r\n");
            if (endOfLineIndex > 0) {
                String sbprint = sb.substring(0, endOfLineIndex);
                sb.delete(0, sb.length());
                aux = Double.parseDouble(sbprint);
        }

        return aux;

        public void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
        }
    }

    // Overrides de metodos inerentes a construção de series para o AndroidPlot
    class SampleDynamicSeries implements XYSeries {
        private SampleDynamicXYDatasource datasource;
        private int seriesIndex;
        private String title;

        public SampleDynamicSeries(SampleDynamicXYDatasource datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }

    boolean findBT() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null)
        {
            Toast.makeText(this,"Não existe bluetooth disponível no equipamento", Toast.LENGTH_SHORT).show();
        }

        if(!btAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Log.i("BLUETOOTH", "antes pairedDevices");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    Log.i("BLUETOOTH",device.getName());
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
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(SecondActivity.this, "Conectando", "Aguarde");
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if (btSocket == null || !estaConectado)
                {
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection

                    btSocket.connect();//start connection
                    estaConectado=true;
                }
            }
            catch (IOException e)
            {
                conectou = false;//if the try failed, you can check the exception here
                estaConectado=false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
           super.onPostExecute(result);
           if (conectou) {
                Log.i("BLUETOOTH","Connected.");
              //  isBtConnected = true;
                btAdapter.cancelDiscovery();
               th = new SampleDynamicXYDatasource(btSocket,mHandler);
               th.start();
               estaConectado=true;
            }
            progress.dismiss();
        }
    }

    void Connect()
    {
        ConnectBT bt = new ConnectBT();
        bt.execute((Void) null);
    }

   private final android.os.Handler mHandler = new android.os.Handler() {

        @Override
        public void handleMessage(Message msg) {
            //switch (msg.what) {
                //case MESSAGE_READ: {
                    byte[] data = (byte[]) msg.obj;
                    String str = new String(data,0,msg.arg1);
                    sb.append(str);
                    //String str = msg.toString();
                    int endOfLineIndex = sb.indexOf("\r\n");
                    if (endOfLineIndex > 0) {
                        String sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        /*
                        A variável sbprint nesse momento possui o valor recebido pela porta serial através do Bluetooth
                        Deve-se utilizar os valores recebidos aqui e plotá-los em um gráfico
                         */
                       // Log.i("BLUETOOTH",sbprint);
                        tv.setText(sbprint); //Lugar onde o valor recebido pelo Bluetooth é apresentado no TextView
                    }
                    //break;
               // }
            //}
        }
    };
}
