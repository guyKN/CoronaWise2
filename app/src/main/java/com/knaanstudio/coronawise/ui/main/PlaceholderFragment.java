package com.knaanstudio.coronawise.ui.main;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.knaanstudio.coronawise.CoronaHandler;
import com.knaanstudio.coronawise.MainActivity;
import com.knaanstudio.coronawise.R;
import com.knaanstudio.coronawise.bluetooth.BluetoothContactTracer2;
import com.knaanstudio.coronawise.database.ContactTraceDao;
import com.knaanstudio.coronawise.database.ContactTraceRoomDatabase;
import com.knaanstudio.coronawise.database.MacAddressInstance;
import com.knaanstudio.coronawise.internet.DownloadCallback;
import com.knaanstudio.coronawise.internet.DownloadThreadManager;
import com.knaanstudio.coronawise.internet.NetworkFragment;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.knaanstudio.coronawise.CoronaHandler.checkIfKeysExist;
import static com.knaanstudio.coronawise.CoronaHandler.generateKeyPair;
import static com.knaanstudio.coronawise.CoronaHandler.readKeyPair;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    String SICK_INDICATOR_LOCATION = "sick.txt";
    String  TAG = "PlaceholderFragment";
    Context context;
    public static final UUID MY_UUID = UUID.fromString("d2033ac3-6439-4ed6-9bb3-c7a4f2e5801d");
    private static final String DEVICE_BLUETOOTH_NAME = "Corona Tracker";
    private final static int REQUEST_ENABLE_BT = 1;
    public final static String TEMP_SIGNATURE_LOCATION = "temp_signature"; // used only for testing, not for actual
    private final static String URL = "http://hallowdawnlarp.com/covid19.php";
    public final static String PRIVATE_KEY_LOCATION = "privatekey.key";
    public final static String PUBLIC_KEY_LOCATION = "publickey.pub";
    public final static int THREAD_DELAY = 1000;//how long to wait before sending key to other bluetooth device.
    public static final int MIN_RSSI = -70; // the minimum RSSI value for the signal to be considered within 6 feet.
    private static final int CHECK_STATUS_EVERY = 1000; //how often we want to run the function that makes sure that the scan is going OK.


    //private NetworkFragment networkFragment;
    Handler repeatHandler = new Handler();
    private DownloadThreadManager mDownloadThreadManager;
    private BluetoothContactTracer2 mBluetoothContactTracer2;
    public  BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static PlaceholderFragment thisInstance;
    public static PlaceholderFragment getInsance(){return thisInstance;}

    Handler mHandler = new Handler(Looper.getMainLooper()){ //TODO: finish
        @Override
        public void handleMessage(Message msg) {
            int message_code = msg.what;
            if(message_code == BluetoothContactTracer2.MessageConstants.MESSAGE_BLUETOOTH_DONE){
                UIbluetoothSuccsess();
            }else if(message_code == BluetoothContactTracer2.MessageConstants.MESSAGE_BLUETOOTH_ERROR){
                UIbluetoothError();
            }else if(message_code == BluetoothContactTracer2.MessageConstants.WRITE_KEY){
                Log.i(TAG, "wrote key");

            }
        }

    };

    Handler mDownloadHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            int code= msg.what;
            if(code == DownloadThreadManager.MessageConstants.MESSAGE_UPLOAD_DONE){
                UImessageUploadDone();
            }else if(code == DownloadThreadManager.MessageConstants.MESSAGE_DOWNLOAD_DONE){
                UImessageDownloadDone(msg.obj.toString());
            }else if(code == DownloadThreadManager.MessageConstants.MESSAGE_ERROR){
                UImessageError();
            }
        }
    };

    private View root;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private PageViewModel pageViewModel;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        thisInstance = this;
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_main, container, false);
        context = MainActivity.getLocalContext();
        final ConstraintLayout section1 = root.findViewById(R.id.section1);
        final ConstraintLayout section2 = root.findViewById(R.id.section2);
        final ConstraintLayout section3 = root.findViewById(R.id.section3);
        final ConstraintLayout section4 = root.findViewById(R.id.section4);

        pageViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.i(MainActivity.TAG, s);
                if(s.equals("1")){
                    section1.setVisibility(View.VISIBLE);
                    section2.setVisibility(View.GONE);
                    section3.setVisibility(View.GONE);
                    section4.setVisibility(View.GONE);
                }else if(s.equals("2")){
                    section1.setVisibility(View.GONE);
                    section2.setVisibility(View.VISIBLE);
                    section3.setVisibility(View.GONE);
                    section4.setVisibility(View.GONE);
                }else if(s.equals("3")){
                    section1.setVisibility(View.GONE);
                    section2.setVisibility(View.GONE);
                    section3.setVisibility(View.VISIBLE);
                    section4.setVisibility(View.GONE);
                }else if(s.equals("4")){
                    section1.setVisibility(View.GONE);
                    section2.setVisibility(View.GONE);
                    section3.setVisibility(View.GONE);
                    section4.setVisibility(View.VISIBLE);
                }


            }
        });


        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(MainActivity.getMainActivity(),
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION); //TODO: explain to the user why this is needed.


        Button refresh =  (Button) root.findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getInfoFromServer();
            }
        });

        Button reset =  (Button) root.findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Context context = view.getContext();
                ContactTraceRoomDatabase db = ContactTraceRoomDatabase.getDatabase(view.getContext());
                ContactTraceDao dao = db.ContactTraceDao();
                dao.deleteAll();
                File pk = new File(context.getFilesDir(), PUBLIC_KEY_LOCATION);
                if(!pk.delete()){
                    Log.i(TAG, "error deleting file");
                }
                File pvt = new File(context.getFilesDir(), PRIVATE_KEY_LOCATION);
                if(!pvt.delete()){
                    Log.i(TAG, "error deleting file");
                }
                File sick = new File(context.getFilesDir(), SICK_INDICATOR_LOCATION);
                if(!sick.delete()){
                    Log.i(TAG, "error deleting file");
                }
                try {
                    CoronaHandler.writeKeyPair(generateKeyPair(), view.getContext());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(context, "all information has been reset",
                        Toast.LENGTH_SHORT).show();

            }
        });



        Button uploadSignature =  (Button) root.findViewById(R.id.uploadSignature);
        uploadSignature.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try{
                    Context context = view.getContext();
                    Pair<Key, Key> keys = readKeyPair(context);
                    Key pvt = keys.first;
                    Key pub = keys.second;
                    byte[] signature = CoronaHandler.generateSignature(pvt);
                    String signatureString = CoronaHandler.KeyToString(signature);
                    startDownload(formatURLParam(signatureString), "POST");
                    UIstartedUpload();
                    File f =new File(context.getFilesDir(), SICK_INDICATOR_LOCATION);
                    f.createNewFile();
                } catch (NoSuchAlgorithmException | IOException  | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                }
            }
        });








        mDownloadThreadManager = new DownloadThreadManager(URL, mDownloadHandler);
        if(!checkIfKeysExist(context)) { //generate a publickey and privatekey the first time the user opens the app.
            try {
                CoronaHandler.writeKeyPair(generateKeyPair(), context);
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: notify users that there was a problem creating the keys.
            }
        }
        if (mBluetoothAdapter == null) { // If the device doesn't support bluetooth, tell that to the user
            Toast.makeText(context, getString(R.string.no_bluetooth), Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) { // enable bluetooth if it's not already enabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        getInfoFromServer();



        IntentFilter bluetoothUpdateFilter = new IntentFilter();
        bluetoothUpdateFilter.addAction(BluetoothDevice.ACTION_FOUND);
        bluetoothUpdateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothUpdateFilter.addAction(BluetoothDevice.ACTION_UUID);
        context.registerReceiver(mReceiver, bluetoothUpdateFilter);

        mBluetoothContactTracer2 = new BluetoothContactTracer2(mBluetoothAdapter, MY_UUID, DEVICE_BLUETOOTH_NAME, context, false, mHandler);
        mBluetoothContactTracer2.runAcceptThread();

        repeatHandler.postDelayed(new Runnable(){ //constantly starts the discovery if the device isn't discovering.
            public void run(){
                Log.i(TAG, "runnning");
                Log.i(TAG, mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ? "discoverable":"not discoverable");
                Log.i(TAG, mBluetoothAdapter.isDiscovering() ? "discovering":"not discovering");
                Log.i(TAG, mBluetoothContactTracer2.shouldBeScanning() ? "should be discovering" : "should not be discovering");
                if(!mBluetoothAdapter.isDiscovering() && mBluetoothContactTracer2.shouldBeScanning()){
                    Log.i(TAG, "Started Discovery");
                    mBluetoothAdapter.startDiscovery();
                }
                if(mBluetoothAdapter.isDiscovering() && !mBluetoothContactTracer2.shouldBeScanning()){
                    Log.i(TAG, "Ended Discovery");
                    mBluetoothAdapter.cancelDiscovery();
                }
                repeatHandler.postDelayed(this, CHECK_STATUS_EVERY);
            }
        }, CHECK_STATUS_EVERY);
        mBluetoothAdapter.startDiscovery();
        return root;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "imp: received");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try {
                    String macAddress = device.getAddress();
                    Log.i(TAG, "imp: Name: " + device.getName());
                    ContactTraceRoomDatabase db = ContactTraceRoomDatabase.getDatabase(context);
                    ContactTraceDao dao = db.ContactTraceDao();
                    List<String> allAddresses = dao.getMacAddresses();
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    if (!allAddresses.contains(macAddress) && rssi > MIN_RSSI) {
                        Log.i(TAG, "imp: Device has been given access to connections: " + device.getName());
                        dao.insert(new MacAddressInstance(macAddress));
                        mBluetoothContactTracer2.runConnectThread(device, macAddress);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onDestroy(){
        super.onDestroy();
        context.unregisterReceiver(mReceiver);
    }

    public void getInfoFromServer(){
        TextView info = (TextView) root.findViewById(R.id.tab1status);
        if(CoronaHandler.checkIfFileExist(SICK_INDICATOR_LOCATION, context)){
            info.setText("You are sick");
        }else {
            info.setText(R.string.loading);
            startDownload("", "GET");
        }
    }

    public void startDownload(String params, String requestType){
        if(!mDownloadThreadManager.startDownload(params, requestType)){// tries to do a download, but notifies user if a download is already going on.
            UInotifyThatDownloadIsInProgress();
        }

    }

    private String formatURLParam(String URLparam) throws UnsupportedEncodingException {
        return "&key=" + URLEncoder.encode(URLparam, "UTF-8");
    }





    /********************************** Status Updates ***********************************************************************************/

    public void UIbluetoothError(){ // Since bluetooth errors ocour all the time,
        //Toast.makeText(getApplicationContext(), R.string.bluetooth_error,
        //        Toast.LENGTH_LONG).show();
    }

    public void UIbluetoothSuccsess(){
        Toast.makeText(context, R.string.bluetooth_sucsess,
                Toast.LENGTH_LONG).show();
    }

    private void UInotifyThatDownloadIsInProgress(){
        Toast.makeText(context, "download is still in progress. Please wait a few moments.",
                Toast.LENGTH_LONG).show();
    }
    private void UImessageUploadDone(){
        TextView tab3status = root.findViewById(R.id.tab3status);
        tab3status.setText("Done!");
    }
    private void UImessageDownloadDone(String msg){
        TextView info = (TextView) root.findViewById(R.id.tab1status);
        info.setText(msg);
    }
    private void UImessageError(){
        Toast.makeText(context, "Error connectiong to Server. Please check your internet and try again.",
                Toast.LENGTH_LONG).show();
        TextView info = (TextView) root.findViewById(R.id.tab1status);
        info.setText("Error!");
    }

    public void UIstartedUpload() {
        TextView tab3status = root.findViewById(R.id.tab3status);
        tab3status.setText("Loading...");
    }
}