package tsl.measurements;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tsl.measurements.ble.BeaconStats;
import tsl.measurements.bluetooth.BTStats;
import tsl.measurements.magnetic.MagneticCalibrated;
import tsl.measurements.magnetic.MagneticUncalibrated;
import tsl.measurements.wifi.WifiStats;


public class MeasurementPerformer extends Activity {

    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("ALL", null, null, null);
    BeaconManager beaconManager;

    ArrayList<BeaconStats> beaconsStats = new ArrayList<BeaconStats>();
    ArrayList<WifiStats> wifiStats = new ArrayList<WifiStats>();
    ArrayList<BTStats> btStats = new ArrayList<BTStats>();
    ArrayList<MagneticCalibrated> magneticCalibratedStats = new ArrayList<MagneticCalibrated>();
    ArrayList<MagneticUncalibrated> magneticUncalibratedStats = new ArrayList<MagneticUncalibrated>();
    EditText areaText;
    String area;
    EditText wifiMeasurementsCountText;
    String wifiMeasurementsCount;
    EditText btMeasurementsCountText;
    String btMeasurementsCount;
    EditText magneticMeasurementsCountText;
    String magneticMeasurementsCount;
    EditText bleMeasurementsCountText;
    String bleMeasurementsCount;
    EditText serverIpText;
    EditText serverPortText;
    ProgressDialog barProgressDialog;
    ProgressDialog barProgressDialog2;
    ProgressDialog barProgressDialog3;
    ProgressDialog barProgressDialog4;
    ProgressDialog ringProgressDialog;
    CheckBox checkBoxManually;
    CheckBox checkBoxScreen;
    Button measureButton;
    WifiManager wifiManager;
    WifiManager.WifiLock wifiLock;
    int btCount = 0;
    int wifiCount = 0;
    int bleCountInProgress = 0;
    Chronometer chronometer;
    SensorManager sensorManager;
    CountDownTimer magneticCountDownTimer;
    String jsonWifiPost;
    String jsonBTPost;
    String jsonMagneticCalibratedPost;
    String jsonMagneticUncalibratedPost;
    String jsonBlePost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_performer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Set wifi on for measurements");
        wifiLock.acquire();

        chronometer = (Chronometer) findViewById(R.id.chronometer);

        areaText = (EditText) findViewById(R.id.areaIdEditText);
        areaText.setEnabled(false);
        wifiMeasurementsCountText = (EditText) findViewById(R.id.NoOfWiFiMeasurementsEditText);
        wifiMeasurementsCountText.setEnabled(false);
        serverIpText = (EditText) findViewById(R.id.serverIpEditText);
        serverPortText = (EditText) findViewById(R.id.serverPortEditText);
        //btMeasurementsCountText = (EditText) findViewById(R.id.NoOfBTMeasurementsEditText);
        //btMeasurementsCountText.setEnabled(false);
        magneticMeasurementsCountText = (EditText) findViewById(R.id.NoOfMagneticMeasurementsEditText);
        magneticMeasurementsCountText.setEnabled(false);
        bleMeasurementsCountText = (EditText) findViewById(R.id.NoOfBleMeasurementsEditText);
        bleMeasurementsCountText.setEnabled(false);

        measureButton = (Button) findViewById(R.id.measureButton);
        measureButton.setEnabled(false);

        checkBoxManually = (CheckBox) findViewById(R.id.manuallyMeasurementsCheckBox);
        checkBoxScreen = (CheckBox) findViewById(R.id.screenOnCheckBox);

        UDPClient udpClient = new UDPClient();
        new Thread(udpClient).start();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        serverIpText.setText("192.168.1.101");
        serverPortText.setText("8080");

        checkBoxManually.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    areaText.setEnabled(true);
                    wifiMeasurementsCountText.setEnabled(true);
                    //btMeasurementsCountText.setEnabled(true);
                    magneticMeasurementsCountText.setEnabled(true);
                    bleMeasurementsCountText.setEnabled(true);
                    measureButton.setEnabled(true);
                } else {
                    areaText.setEnabled(false);
                    wifiMeasurementsCountText.setEnabled(false);
                    //btMeasurementsCountText.setEnabled(false);
                    magneticMeasurementsCountText.setEnabled(false);
                    bleMeasurementsCountText.setEnabled(false);
                    measureButton.setEnabled(false);
                }
            }
        });

        checkBoxScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                }
            }
        });

        measureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                areaText = (EditText) findViewById(R.id.areaIdEditText);
                area = areaText.getText().toString();
                wifiMeasurementsCountText = (EditText) findViewById(R.id.NoOfWiFiMeasurementsEditText);
                wifiMeasurementsCount = wifiMeasurementsCountText.getText().toString();
                serverIpText = (EditText) findViewById(R.id.serverIpEditText);
                serverPortText = (EditText) findViewById(R.id.serverPortEditText);
                //btMeasurementsCountText = (EditText) findViewById(R.id.NoOfBTMeasurementsEditText);
                //btMeasurementsCount = btMeasurementsCountText.getText().toString();
                magneticMeasurementsCountText = (EditText) findViewById(R.id.NoOfMagneticMeasurementsEditText);
                magneticMeasurementsCount = magneticMeasurementsCountText.getText().toString();
                bleMeasurementsCountText = (EditText) findViewById(R.id.NoOfBleMeasurementsEditText);
                bleMeasurementsCount = bleMeasurementsCountText.getText().toString();

                btCount = 0;
                wifiCount = 0;
                wifiStats = new ArrayList<WifiStats>();
                //btStats = new ArrayList<BTStats>();
                magneticCalibratedStats = new ArrayList<MagneticCalibrated>();
                magneticUncalibratedStats = new ArrayList<MagneticUncalibrated>();
                beaconsStats = new ArrayList<BeaconStats>();


                if (areaText.getText() != null && wifiMeasurementsCountText.getText() != null && serverIpText.getText() != null && serverPortText.getText() != null && magneticMeasurementsCountText.getText() != null && bleMeasurementsCountText.getText() != null) {
                    if (!areaText.getText().toString().equals("") && !wifiMeasurementsCountText.getText().toString().equals("") && !serverIpText.getText().toString().equals("") && !serverPortText.getText().toString().equals("") && !magneticMeasurementsCountText.getText().toString().equals("") && !bleMeasurementsCountText.getText().toString().equals("")) {
                        System.out.println(areaText.getText().toString());
                        System.out.println(wifiMeasurementsCountText.getText().toString());
                        System.out.println(serverIpText.getText().toString());
                        System.out.println(serverPortText.getText().toString());
                        //System.out.println(btMeasurementsCountText.getText().toString());
                        System.out.println(magneticMeasurementsCountText.getText().toString());
                        System.out.println(bleMeasurementsCountText.getText().toString());

                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.setFormat("Elapsed time: %s");
                        chronometer.start();

                        barProgressDialog = new ProgressDialog(MeasurementPerformer.this);
                        barProgressDialog.setTitle("WiFi Measuring ...");
                        barProgressDialog.setMessage("WiFi Measurement in Progress...");
                        barProgressDialog.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
                        barProgressDialog.setProgress(0);
                        barProgressDialog.setCancelable(false);
                        barProgressDialog.setMax(Integer.parseInt(wifiMeasurementsCount));
                        barProgressDialog.show();

                        new MeasureWifi().execute();
                    } else {
                        Toast.makeText(getApplicationContext(), "No available input parameters", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No available input parameters", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private SensorEventListener magneticListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {


            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);

                Log.i("INFO", String.valueOf(event.values[0]));
                Log.i("INFO", String.valueOf(event.values[1]));
                Log.i("INFO", String.valueOf(event.values[2]));

                MagneticCalibrated magneticCalibrated = new MagneticCalibrated();

                magneticCalibrated.setArea(area);
                magneticCalibrated.setxValue(String.valueOf(event.values[0]));
                magneticCalibrated.setyValue(String.valueOf(event.values[1]));
                magneticCalibrated.setzValue(String.valueOf(event.values[2]));
                magneticCalibrated.setTime(time);

                magneticCalibratedStats.add(magneticCalibrated);

                sensorManager.unregisterListener(magneticListener);
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private SensorEventListener magneticUncalibratedListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);

                Log.i("INFO", String.valueOf(event.values[0]));
                Log.i("INFO", String.valueOf(event.values[1]));
                Log.i("INFO", String.valueOf(event.values[2]));
                Log.i("INFO", String.valueOf(event.values[3]));
                Log.i("INFO", String.valueOf(event.values[4]));
                Log.i("INFO", String.valueOf(event.values[5]));

                MagneticUncalibrated magneticUncalibrated = new MagneticUncalibrated();

                magneticUncalibrated.setArea(area);
                magneticUncalibrated.setxValueUncalib(String.valueOf(event.values[0]));
                magneticUncalibrated.setyValueUncalib(String.valueOf(event.values[1]));
                magneticUncalibrated.setzValueUncalib(String.valueOf(event.values[2]));
                magneticUncalibrated.setxBias(String.valueOf(event.values[3]));
                magneticUncalibrated.setyBias(String.valueOf(event.values[4]));
                magneticUncalibrated.setzBias(String.valueOf(event.values[5]));
                magneticUncalibrated.setTime(time);

                magneticUncalibratedStats.add(magneticUncalibrated);

                sensorManager.unregisterListener(magneticUncalibratedListener);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_measurement_performer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up measureButton, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private class MeasureWifi extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            final WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wManager.startScan();
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent intent) {

            wifiCount = wifiCount + 1;

            if (wifiCount > Integer.parseInt(wifiMeasurementsCount)) {

                System.out.println("WiFi Measurement Finished!");

                barProgressDialog.dismiss();

                CountDownTimer magneticCountDownTimer = new CountDownTimer((Integer.valueOf(magneticMeasurementsCount) + 1) * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        barProgressDialog3.incrementProgressBy(1);
                        sensorManager.registerListener(magneticListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
                        // Uncomment the line below in order to enable non calibrated magnetic field measurements as well
                        // sensorManager.registerListener(magneticUncalibratedListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_NORMAL);
                    }

                    @Override
                    public void onFinish() {

                        barProgressDialog3.dismiss();

                        /*ringProgressDialog = ProgressDialog.show(MeasurementPerformer.this, "Please wait...", "Uploading to Server...", true);
                        ringProgressDialog.setCancelable(false);
                        ringProgressDialog.show();

                        Gson gson = new Gson();

                        jsonWifiPost = gson.toJson(wifiStats);
                        jsonBTPost = "{}";//gson.toJson(btStats);
                        jsonMagneticCalibratedPost = gson.toJson(magneticCalibratedStats);
                        jsonMagneticUncalibratedPost = gson.toJson(magneticUncalibratedStats);

                        String url;
                        try {
                            url = "http://" + serverIpText.getText().toString() + ":" + serverPortText.getText().toString() + "/IndoorPositioningServer/monitor/measurementService";//?WiFiMeasurements=" + URLEncoder.encode(jsonWifiPost, "UTF-8") + "&BTMeasurements=" + URLEncoder.encode(jsonBTPost, "UTF-8") + "&MagneticCalibratedMeasurements=" + URLEncoder.encode(jsonMagneticCalibratedPost, "UTF-8") + "&MagneticUncalibratedMeasurements=" + URLEncoder.encode(jsonMagneticUncalibratedPost, "UTF-8");
                            System.out.println(url);
                            new RestClient().execute(url);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }*/
                    }
                };

                barProgressDialog3 = new ProgressDialog(MeasurementPerformer.this);
                barProgressDialog3.setTitle("Magnetic Field Measuring ...");
                barProgressDialog3.setMessage("Magnetic Field Measurement in Progress...");
                barProgressDialog3.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog3.setProgress(0);
                barProgressDialog3.setCancelable(false);
                barProgressDialog3.setMax(Integer.parseInt(magneticMeasurementsCount));
                barProgressDialog3.show();

                magneticCountDownTimer.start();

                /*barProgressDialog2 = new ProgressDialog(MeasurementPerformer.this);
                barProgressDialog2.setTitle("BT Measuring ...");
                barProgressDialog2.setMessage("BT Measurement in Progress...");
                barProgressDialog2.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog2.setProgress(0);
                barProgressDialog2.setCancelable(false);
                barProgressDialog2.setMax(Integer.parseInt(btMeasurementsCount));
                barProgressDialog2.show();

                new MeasureBT().execute();*/

            } else {

                System.out.println("WiFi Measurement: " + wifiCount + "...");

                WifiManager wManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);


                List<ScanResult> wifiList = wManager.getScanResults();
                for (int i = 0; i < wifiList.size(); i++) {

                    ScanResult wifi = wManager.getScanResults().get(i);

                    WifiStats wifiStat = new WifiStats();
                    wifiStat.setArea(area);
                    wifiStat.setBssid(wifi.BSSID);
                    wifiStat.setSsid(wifi.SSID);
                    wifiStat.setRssi(String.valueOf(wifi.level));
                    wifiStat.setTime(time);

                    wifiStats.add(wifiStat);

                    String outputInfo = "BSSID: " + wifi.BSSID + " " + "Level: " + wifi.level;
                    System.out.println(outputInfo);

                }
                barProgressDialog.incrementProgressBy(1);
                new MeasureWifi().execute();
            }
        }
    };


    private class MeasureBT extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                System.out.println("Not BT support");
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                    System.out.println("BT was disabled");
                }
                System.out.println("BT enabled");

                mBluetoothAdapter.startDiscovery();
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(btReceiver, filter);

            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }


    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equalsIgnoreCase("android.bluetooth.device.action.FOUND")) {
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
                Date date = new Date();
                String time = dateFormat.format(date);

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                BTStats btStat = new BTStats();
                btStat.setBssid(device.getAddress());
                btStat.setSsid(device.getName());
                btStat.setArea(area);
                btStat.setRssi(String.valueOf(rssi));
                btStat.setTime(time);

                btStats.add(btStat);

                System.out.println((device.getName() + " " + device.getAddress()) + " RSSI: " + rssi);
            }
            if (intent.getAction().equalsIgnoreCase("android.bluetooth.adapter.action.DISCOVERY_STARTED")) {
                System.out.println("BT Measurement STARTED");
                barProgressDialog2.incrementProgressBy(1);
                btCount = btCount + 1;
                System.out.println("BT Measurement: " + btCount + "...");
            }
            if (intent.getAction().equalsIgnoreCase("android.bluetooth.adapter.action.DISCOVERY_FINISHED")) {
                System.out.println("BT Measurement FINISHED");

                if (btCount < Integer.parseInt(btMeasurementsCount)) {
                    new MeasureBT().execute();
                } else {

                    barProgressDialog2.dismiss();

                    /*CountDownTimer magneticCountDownTimer = new CountDownTimer((Integer.valueOf(magneticMeasurementsCount) + 1) * 1000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            barProgressDialog3.incrementProgressBy(1);
                            sensorManager.registerListener(magneticListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
                            // Uncomment the line below in order to enable non calibrated magnetic field measurements as well
                            // sensorManager.registerListener(magneticUncalibratedListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SensorManager.SENSOR_DELAY_NORMAL);
                        }

                        @Override
                        public void onFinish() {

                            barProgressDialog3.dismiss();

                            ringProgressDialog = ProgressDialog.show(MeasurementPerformer.this, "Please wait...", "Uploading to Server...", true);
                            ringProgressDialog.setCancelable(false);
                            ringProgressDialog.show();

                            Gson gson = new Gson();

                            jsonWifiPost = gson.toJson(wifiStats);
                            jsonBTPost = gson.toJson(btStats);
                            jsonMagneticCalibratedPost = gson.toJson(magneticCalibratedStats);
                            jsonMagneticUncalibratedPost = gson.toJson(magneticUncalibratedStats);

                            String url;
                            try {
                                url = "http://" + serverIpText.getText().toString() + ":" + serverPortText.getText().toString() + "/IndoorPositioningServer/monitor/measurementService";//?WiFiMeasurements=" + URLEncoder.encode(jsonWifiPost, "UTF-8") + "&BTMeasurements=" + URLEncoder.encode(jsonBTPost, "UTF-8") + "&MagneticCalibratedMeasurements=" + URLEncoder.encode(jsonMagneticCalibratedPost, "UTF-8") + "&MagneticUncalibratedMeasurements=" + URLEncoder.encode(jsonMagneticUncalibratedPost, "UTF-8");
                                System.out.println(url);
                                new RestClient().execute(url);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    barProgressDialog3 = new ProgressDialog(MeasurementPerformer.this);
                    barProgressDialog3.setTitle("Magnetic Field Measuring ...");
                    barProgressDialog3.setMessage("Magnetic Field Measurement in Progress...");
                    barProgressDialog3.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
                    barProgressDialog3.setProgress(0);
                    barProgressDialog3.setCancelable(false);
                    barProgressDialog3.setMax(Integer.parseInt(magneticMeasurementsCount));
                    barProgressDialog3.show();

                    magneticCountDownTimer.start();*/

                }
            }

        }
    };


    private void retrieveBLEStats() {

        barProgressDialog4 = new ProgressDialog(MeasurementPerformer.this);
        barProgressDialog4.setTitle("BLE Measuring ...");
        barProgressDialog4.setMessage("BLE Measurement in Progress...");
        barProgressDialog4.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
        barProgressDialog4.setProgress(0);
        barProgressDialog4.setCancelable(false);
        barProgressDialog4.setMax(Integer.parseInt(bleMeasurementsCount));
        barProgressDialog4.show();

        beaconManager = new BeaconManager(this);
        if (beaconManager.hasBluetooth() && beaconManager.isBluetoothEnabled()) {

            beaconManager.setRangingListener(new BeaconManager.RangingListener() {
                @Override
                public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                    // Note that results are not delivered on UI thread.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (bleCountInProgress < Integer.parseInt(bleMeasurementsCount)) {

                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
                                Date date = new Date();
                                String time = dateFormat.format(date);

                                barProgressDialog4.incrementProgressBy(1);

                                for (Beacon beacon : beacons) {

                                    BeaconStats beaconStats = new BeaconStats();

                                    beaconStats.setMac(beacon.getMacAddress());
                                    beaconStats.setMajor(String.valueOf(beacon.getMajor()));
                                    beaconStats.setMinor(String.valueOf(beacon.getMinor()));
                                    beaconStats.setProximityUUID(beacon.getProximityUUID());
                                    beaconStats.setTxPower(String.valueOf(beacon.getMeasuredPower()));
                                    beaconStats.setRssi(String.valueOf(beacon.getRssi()));
                                    beaconStats.setEstimatedDistance(String.valueOf(Utils.computeAccuracy(beacon)));
                                    beaconStats.setTime(time);
                                    beaconStats.setArea(area);

                                    Log.i("INFO", "Found beacons: " + beaconStats.getMac() + " " + beaconStats.getMajor() + " " + beaconStats.getMinor() + " " + beaconStats.getProximityUUID() + " " + beaconStats.getTxPower() + " " + beaconStats.getRssi() + " " + beaconStats.getEstimatedDistance() + "m");

                                    beaconsStats.add(beaconStats);

                                }

                                bleCountInProgress++;

                            } else {

                                barProgressDialog4.dismiss();

                                ringProgressDialog = ProgressDialog.show(MeasurementPerformer.this, "Please wait...", "Uploading to Server...", true);
                                ringProgressDialog.setCancelable(false);
                                ringProgressDialog.show();

                                Gson gson = new Gson();

                                jsonWifiPost = gson.toJson(wifiStats);
                                jsonBTPost = "{}";//gson.toJson(btStats);
                                jsonMagneticCalibratedPost = gson.toJson(magneticCalibratedStats);
                                jsonMagneticUncalibratedPost = gson.toJson(magneticUncalibratedStats);
                                jsonBlePost = gson.toJson(beaconsStats);

                                String url;
                                try {
                                    url = "http://" + serverIpText.getText().toString() + ":" + serverPortText.getText().toString() + "/IndoorPositioningServer/monitor/measurementService";//?WiFiMeasurements=" + URLEncoder.encode(jsonWifiPost, "UTF-8") + "&BTMeasurements=" + URLEncoder.encode(jsonBTPost, "UTF-8") + "&MagneticCalibratedMeasurements=" + URLEncoder.encode(jsonMagneticCalibratedPost, "UTF-8") + "&MagneticUncalibratedMeasurements=" + URLEncoder.encode(jsonMagneticUncalibratedPost, "UTF-8");
                                    System.out.println(url);
                                    new RestClient().execute(url);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                try {
                                    Log.i("INFO", "Stopping BLE Ranging...");
                                    beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
                                } catch (RemoteException e) {
                                    Log.d("Error", "Error while stopping ranging", e);
                                }
                            }
                        }
                    });
                }
            });

            beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                @Override
                public void onServiceReady() {
                    try {
                        beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                    } catch (RemoteException e) {
                        Log.e("ERROR", "Cannot start ranging", e);
                    }
                }
            });
        }

    }


    @Override
    public void onDestroy() {

        unregisterReceiver(btReceiver);
        unregisterReceiver(wifiReceiver);
        wifiLock.release();

        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
            Log.d("Error", "Error while stopping ranging", e);
        }

    }


    class RestClient extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://" + serverIpText.getText().toString() + ":" + serverPortText.getText().toString() + "/IndoorPositioningServer/monitor/measurementService");
            HttpResponse response;
            String responseString = null;
            try {

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                nameValuePairs.add(new BasicNameValuePair("WiFiMeasurements", jsonWifiPost));
                nameValuePairs.add(new BasicNameValuePair("BTMeasurements", jsonBTPost));
                nameValuePairs.add(new BasicNameValuePair("MagneticCalibratedMeasurements", jsonMagneticCalibratedPost));
                nameValuePairs.add(new BasicNameValuePair("MagneticUncalibratedMeasurements", jsonMagneticUncalibratedPost));
                nameValuePairs.add(new BasicNameValuePair("BleMeasurements", jsonBlePost));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //httppost.setHeader("Accept", "application/json");
                httppost.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF8");

                response = httpclient.execute(httppost);
                /*StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }*/
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..
            System.out.println("INFO: " + result);
            ringProgressDialog.dismiss();
            //unregisterReceiver(btReceiver);
            unregisterReceiver(wifiReceiver);

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();

            Toast.makeText(getApplicationContext(), "Measurements Finished!", Toast.LENGTH_LONG).show();

            chronometer.stop();

        }
    }


    public class UDPClient extends Application implements Runnable {
        private final static int LISTENING_PORT = 37766;

        @Override
        public void run() {
            try {
                //Opening listening socket
                Log.e("UDP Receiver", "Opening listening socket on port " + LISTENING_PORT + "...");
                DatagramSocket socket = new DatagramSocket(LISTENING_PORT);
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                Log.e("UDP Receiver", "Listening...");
                while (true) {
                    //Listening on socket
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String message = new String(packet.getData()).trim();
                    Log.e("UDP Receiver", "UDP Packet Received:" + message);
                    message.replaceAll("[\\~\\`\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\_\\-\\+\\{\\}\\[\\]\\;\"\\|\\\\,\\.\\/\\<\\>\\?]", "");
                    if (message.contains("measure")) {

                        Log.e("UDP", message);

                        String[] input = message.split("=");
                        String[] values = input[1].split(",");
                        area = values[0];
                        wifiMeasurementsCount = values[1];
                        btMeasurementsCount = values[2];
                        magneticMeasurementsCount = values[3];
                        bleMeasurementsCount = values[4];

                        btCount = 0;
                        wifiCount = 0;
                        wifiStats = new ArrayList<WifiStats>();
                        btStats = new ArrayList<BTStats>();
                        magneticCalibratedStats = new ArrayList<MagneticCalibrated>();
                        magneticUncalibratedStats = new ArrayList<MagneticUncalibrated>();
                        beaconsStats = new ArrayList<BeaconStats>();

                        if (area != null && wifiMeasurementsCount != null && serverIpText.getText() != null && serverPortText.getText() != null && btMeasurementsCount != null && magneticMeasurementsCount != null && bleMeasurementsCount != null) {
                            if (!area.equals("") && !wifiMeasurementsCount.equals("") && !serverIpText.getText().toString().equals("") && !serverPortText.getText().toString().equals("") && !btMeasurementsCount.equals("") && !magneticMeasurementsCount.equals("") && !bleMeasurementsCount.equals("")) {

                                System.out.println(area);
                                System.out.println(wifiMeasurementsCount);
                                System.out.println(serverIpText.getText().toString());
                                System.out.println(serverPortText.getText().toString());
                                System.out.println(btMeasurementsCount);
                                System.out.println(magneticMeasurementsCount);
                                System.out.println(bleMeasurementsCount);

                                handler.sendMessage(new Message());

                                new MeasureWifi().execute();
                            }
                        }

                    } else {
                        Log.e("UDP Receiver", "Unknown Button");
                    }
                }
            } catch (Exception e) {
                Log.e("UDP", "Receiver error", e);
            }
        }
    }

    public Handler handler = new Handler() {

        // @Override
        public void handleMessage(Message msg) {
            //Do something with the message
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.setFormat("Elapsed time: %s");
            chronometer.start();
            new InitBar().initBar();
        }
    };

    public class InitBar {

        public void initBar() {

            barProgressDialog = new ProgressDialog(MeasurementPerformer.this);
            barProgressDialog.setTitle("WiFi Measuring ...");
            barProgressDialog.setMessage("WiFi Measurement in Progress...");
            barProgressDialog.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
            barProgressDialog.setProgress(0);
            barProgressDialog.setCancelable(false);
            barProgressDialog.setMax(Integer.parseInt(wifiMeasurementsCount));
            barProgressDialog.show();

        }

    }


}