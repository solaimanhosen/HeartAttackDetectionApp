package com.example.panda.heartattackdetectionapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.LegendAlign;

import org.w3c.dom.Text;


public class MainActivity extends Activity { //AppCompatActivity
    TextView txtArduino, txtString, txtStringLength, sensorView0;
    Handler bluetoothIn;

    boolean fullView;
    boolean bConnectClick;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    // new code

    static LinearLayout GraphView;
    static GraphView graphView;
    static GraphViewSeries Series;

    private static double graph2LastXValue = 0;
    private static int Xview=10;
    Button bConnect, bDisconnect;

    //toggle Button
    static boolean Lock;//whether lock the x-axis to 0-5
    static boolean AutoScrollX;//auto scroll to the last x value
    static boolean Stream;//Start or stop streaming
    boolean tbstreamStatus;
    //Button init
    Button bXminus;
    Button bXplus;
    ToggleButton tbLock;
    ToggleButton tbScroll;
    ToggleButton tbStream;

    //TExt view
    TextView HeartRate;
    TextView HStatus;

    private String phoneNo;
    private String message;

    private double[] ecg = new double[600];
    private int dataCount;
    private int index;
    private boolean dataFull;
    private boolean msgStatus;
    public static final int M = 5;
    public static final int N = 30;
    public static final int winSize = 250;
    public static final float HP_CONSTANT = (float) 1/M;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        // new code
        /*LinearLayout background = (LinearLayout)findViewById(R.id.bg);
        background.setBackgroundColor(Color.BLACK);*/
        TextViewInit();
        dataCount = 0;
        index = 0;
        dataFull = false;
        msgStatus = false;

        init();
        ButtonInit();
        // previous code
        //Link the buttons and textViews to respective views
        //txtString = (TextView) findViewById(R.id.txtString);
        //txtStringLength = (TextView) findViewById(R.id.testView1);
       // sensorView0 = (TextView) findViewById(R.id.sensorView0);
        graphClick();
        showGraphView();
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        allButtonClickLiesner();
    }

    private void allButtonClickLiesner() {
        bConnect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // create intent for paired devices.
                bConnectClick = true;
                Intent i = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivity(i);
            }
        });

        bDisconnect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // write code for disconnect Bluetooth
            }
        });

        bXplus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(Xview < 30)
                    Xview++;
            }
        });

        bXminus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if(Xview > 1)
                    Xview--;
            }
        });

        tbStream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tbstreamStatus = isChecked;
                if (isChecked) {
                    mConnectedThread.write("S");    // Send "1" via Bluetooth
                    Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
                } else {
                    mConnectedThread.write("Q");    // Send "0" via Bluetooth
                    Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tbLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Lock = true;
                }else{
                    Lock = false;
                }
            }
        });

        tbScroll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AutoScrollX = true;
                }else{
                    AutoScrollX = false;
                }
            }
        });
    }

    private void graphClick() {
        GraphView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                setContentView(R.layout.activity_main);
                //TextViewInit();
                init();
                ButtonInit();
                allButtonClickLiesner();
                tbStream.setChecked(tbstreamStatus);
                fullView = true;
                showGraphView();
            }
        });
    }

    private void showGraphView() {
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //txtString.setText("Data Received = " + dataInPrint);
                        int dataLength = dataInPrint.length();                          //get length of data received
                        //txtStringLength.setText("String Length = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {
                            String sensor0 = recDataString.substring(1, 5);            //get sensor value from string between indices 1-5

                            // new code
                            //HeartRate.setText(sensor0);


                            if (isFloatNumber(sensor0)){
                                //new code
                                ecg[index] = Double.parseDouble(sensor0);
                                index = (index+1)%600;
                                if(dataCount <= 600){
                                    dataCount++;
                                }else{
                                    dataFull = true;
                                    int QRS[] = detect(ecg);
                                    int hr = 0;
                                    for(int kk = 0; kk <QRS.length; kk++){
                                        if(QRS[kk] == 1)
                                            hr++;
                                    }
                                    HeartRate.setText(hr+" BPM");
                                    if((hr >= 0 && hr <= 5) && !msgStatus){
                                        HStatus.setText("Heart Beat too Slow");
                                        msgStatus = true;
                                        message = "We are about to loose Patient";
                                        sendSMSMessage();
                                    }
                                    else if((hr <= 40 || hr >= 100) && !msgStatus){
                                        HStatus.setText("Irregular Heart Beats");
                                        msgStatus = true;
                                        message = "Patient's Heart Beats is not Normal";
                                        sendSMSMessage();
                                    }
                                    else if(hr > 40 && hr < 100){
                                        HStatus.setText("Safe & Healthy Heart Beats");
                                        msgStatus = false;
                                        message = "Patient's Heart Beats is Normal";
                                        sendSMSMessage();
                                    }
                                }



                                Series.appendData(new GraphView.GraphViewData(graph2LastXValue,Double.parseDouble(sensor0)),AutoScrollX);

                                //X-axis control
                                if (graph2LastXValue >= Xview && Lock == true){
                                    Series.resetData(new GraphView.GraphViewData[] {});
                                    graph2LastXValue = 0;
                                }else graph2LastXValue += 0.1;

                                if(Lock == true)
                                    graphView.setViewPort(0, Xview);
                                else
                                    graphView.setViewPort(graph2LastXValue-Xview, Xview);

                                //refresh
                                GraphView.removeView(graphView);
                                GraphView.addView(graphView);

                            }
                            //old code
                            //sensorView0.setText(" Sensor 0 Voltage = " + sensor0 + "V");    //update the textviews with sensor values
                        }
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        // strIncom =" ";
                        dataInPrint = " ";
                    }
                }
            }
            public boolean isFloatNumber(String num){
                //Log.d("checkfloatNum", num);
                try{
                    Double.parseDouble(num);
                } catch(NumberFormatException nfe) {
                    return false;
                }
                return true;
            }
        };
    }

    private static int[] detect(double[] ecg) {
        // circular buffer for input ecg signal
        // we need to keep a history of M + 1 samples for HP filter
        double[] ecg_circ_buff = new double[M + 1];
        int ecg_circ_WR_idx = 0;
        int ecg_circ_RD_idx = 0;

        // circular buffer for input ecg signal
        // we need to keep a history of N+1 samples for LP filter
        double[] hp_circ_buff = new double[N+1];
        int hp_circ_WR_idx = 0;
        int hp_circ_RD_idx = 0;

        // LP filter outputs a single point for every input point
        // This goes straight to adaptive filtering for eval
        double next_eval_pt = 0;

        // output
        int[] QRS = new int[ecg.length];

        // running sums for HP and LP filters, values shifted in FILO
        double hp_sum = 0;
        double lp_sum = 0;

        // parameters for adaptive thresholding
        double treshold = 0;
        boolean triggered = false;
        int trig_time = 0;
        double win_max = 0;
        int win_idx = 0;

        for(int i = 0; i < ecg.length; i++){
            ecg_circ_buff[ecg_circ_WR_idx++] = ecg[i];
            ecg_circ_WR_idx %= (M+1);

				/* High pass filtering */
            if(i < M){
                // first fill buffer with enough points for HP filter
                hp_sum += ecg_circ_buff[ecg_circ_RD_idx];
                hp_circ_buff[hp_circ_WR_idx] = 0;
            }
            else{
                hp_sum += ecg_circ_buff[ecg_circ_RD_idx];

                int tmp = ecg_circ_RD_idx - M;
                if(tmp < 0){
                    tmp += M + 1;
                }
                hp_sum -= ecg_circ_buff[tmp];

                double y1 = 0;
                double y2 = 0;

                tmp = (ecg_circ_RD_idx - ((M+1)/2));
                if(tmp < 0){
                    tmp += M + 1;
                }
                y2 = ecg_circ_buff[tmp];

                y1 = HP_CONSTANT * hp_sum;

                hp_circ_buff[hp_circ_WR_idx] = y2 - y1;
            }

            ecg_circ_RD_idx++;
            ecg_circ_RD_idx %= (M+1);

            hp_circ_WR_idx++;
            hp_circ_WR_idx %= (N+1);

				/* Low pass filtering */

            // shift in new sample from high pass filter
            lp_sum += hp_circ_buff[hp_circ_RD_idx] * hp_circ_buff[hp_circ_RD_idx];

            if(i < N){
                // first fill buffer with enough points for LP filter
                next_eval_pt = 0;

            }
            else{
                // shift out oldest data point
                int tmp = hp_circ_RD_idx - N;
                if(tmp < 0){
                    tmp += N+1;
                }
                lp_sum -= hp_circ_buff[tmp] * hp_circ_buff[tmp];

                next_eval_pt = lp_sum;
            }

            hp_circ_RD_idx++;
            hp_circ_RD_idx %= (N+1);

				/* Adapative thresholding beat detection */
            // set initial threshold
            if(i < winSize) {
                if(next_eval_pt > treshold) {
                    treshold = next_eval_pt;
                }
            }

            // check if detection hold off period has passed
            if(triggered){
                trig_time++;

                if(trig_time >= 100){
                    triggered = false;
                    trig_time = 0;
                }
            }

            // find if we have a new max
            if(next_eval_pt > win_max) win_max = next_eval_pt;

            // find if we are above adaptive threshold
            if(next_eval_pt > treshold && !triggered) {
                QRS[i] = 1;

                triggered = true;
            }
            else {
                QRS[i] = 0;
            }

            // adjust adaptive threshold using max of signal found
            // in previous window
            if(++win_idx > winSize){
                // weighting factor for determining the contribution of
                // the current peak value to the threshold adjustment
                double gamma = 0.175;

                // forgetting factor -
                // rate at which we forget old observations
                double alpha = 0.01 + (Math.random() * ((0.1 - 0.01)));

                treshold = alpha * gamma * win_max + (1 - alpha) * treshold;

                // reset current window ind
                win_idx = 0;
                win_max = -10000000;
            }
        }

        return QRS;
    }

    // new code
    protected void sendSMSMessage() {
        phoneNo = "01745107288";
        //message = msg;

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) this,
                        Manifest.permission.SEND_SMS)) {
                    ActivityCompat.requestPermissions((Activity) this,
                            new String[]{Manifest.permission.SEND_SMS},
                            1);
                } else {
                    ActivityCompat.requestPermissions((Activity) this,
                            new String[]{Manifest.permission.SEND_SMS}, 1);
                }
                return;

            }
        }

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNo, null, message, null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSMSMessage();

                }
                return;
            }


        }
    }

    void TextViewInit(){

        HeartRate = (TextView)findViewById(R.id.HR);
        HStatus = (TextView)findViewById(R.id.HStatus);
    }
    void init(){
        //Bluetooth.gethandler(mHandler);

        //init graphview
        GraphView = (LinearLayout) findViewById(R.id.Graph);
        // init example series data-------------------
        Series = new GraphViewSeries("Signal",
                new GraphViewSeries.GraphViewStyle(Color.BLACK, 2),//color and thickness of the line
                new GraphView.GraphViewData[] {new GraphView.GraphViewData(0, 0)});
        graphView = new LineGraphView(
                this // context
                , "Graph" // heading
        );
        graphView.setViewPort(0, Xview);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setShowLegend(true);
        //graphView.setLegendAlign(com.jjoe64.graphview.GraphView.LegendAlign.BOTTOM);
        graphView.setLegendAlign(LegendAlign.BOTTOM);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(5, 0);
        graphView.addSeries(Series); // data
        GraphView.addView(graphView);

    }

    void ButtonInit(){
        bConnect = (Button)findViewById(R.id.bConnect);
        bDisconnect = (Button)findViewById(R.id.bDisconnect);
        //X-axis control button
        bXminus = (Button)findViewById(R.id.bXminus);
        bXplus = (Button)findViewById(R.id.bXplus);
        //
        tbLock = (ToggleButton)findViewById(R.id.tbLock);
        tbScroll = (ToggleButton)findViewById(R.id.tbScroll);
        tbStream = (ToggleButton)findViewById(R.id.tbStream);
        //init toggleButton
        Lock=true;
        AutoScrollX=true;
        Stream=true;
    }

//    public void onClick(View v)
//    {
//        switch(v.getId()){
//            case R.id.bConnect:
//                //startActivity(new Intent("android.intent.action.BT1"));
//                break;
//            case R.id.bDisconnect:
//                //Bluetooth.disconnect();
//                break;
//            case R.id.bXminus:
//                if (Xview>1) Xview--;
//                break;
//            case R.id.bXplus:
//                if (Xview<30) Xview++;
//                break;
//            case R.id.tbLock:
//                if (tbLock.isChecked()){
//                    Lock = true;
//                }else{
//                    Lock = false;
//                }
//                break;
//            case R.id.tbScroll:
//                if (tbScroll.isChecked()){
//                    AutoScrollX = true;
//                }else{
//                    AutoScrollX = false;
//                }
//                break;
//            case R.id.tbStream:
//                if (tbStream.isChecked()){
//                    mConnectedThread.write("1");    // Send "1" via Bluetooth
//                    Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
//                }else{
//                    mConnectedThread.write("0");    // Send "0" via Bluetooth
//                    Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
//                }
//                break;
//        }
//    }
//    // previous code

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //if(bConnectClick){
            //Get MAC address from DeviceListActivity via intent
            Intent intent = getIntent();

            //Get the MAC address from the DeviceListActivty via EXTRA
            address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try
            {
                btSocket.connect();
            } catch (IOException e) {
                try
                {
                    btSocket.close();
                } catch (IOException e2)
                {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            mConnectedThread.write("x");
            //mConnectedThread.run();
        //}
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    @Override
    public void onBackPressed() {
        if(fullView){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            setContentView(R.layout.home_layout);

            TextViewInit();
            init();
            ButtonInit();
            tbStream.setChecked(tbstreamStatus);
            fullView = false;
            showGraphView();
            allButtonClickLiesner();
            graphClick();
            return;
        }

        super.onBackPressed();

    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

}
