package mahe.sendme;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

public class Client extends AppCompatActivity {

    EditText editTextAddress;
    Button buttonConnect;
    TextView textPort;
    ProgressDialog progress;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static final int SocketServerPORT = 2000;
    static final String SocketServerAddress="192.168.43.1";
    static  boolean found=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        connectToWifi();
        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);
        progress=new ProgressDialog(this);

        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    Client.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
        if(wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(false);
    }

    private class ClientRxThread extends AsyncTask<String,Integer,String> {
        String dstAddress;
        int dstPort;

        ClientRxThread(String address, int port) {
            dstAddress = address;
            dstPort = port;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Client.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.setIndeterminate(false);
                    progress.setMax(100);
                    progress.setCanceledOnTouchOutside(false);
                    progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progress.show();
                }});

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progress.dismiss();

        }

        @Override
        protected String doInBackground(final String... strings) {

            Socket socket = null;
            try {
                socket = new Socket(dstAddress, dstPort);

                final File file=new File(strings[0],strings[1]);


                BufferedInputStream bis;
                    bis = new BufferedInputStream(new FileInputStream(file));
                    int nobytes,tmp=0;

                    float length=file.length(),totprog=0;
                  OutputStream os = socket.getOutputStream();
                    InputStream in=socket.getInputStream();
                os.write(strings[1].getBytes(),0,strings[1].getBytes().length);
                in.read();
                os.write(String.valueOf(file.length()).getBytes(),0,String.valueOf(file.length()).getBytes().length);
                in.read();
                byte bytes[]=new byte[1024];
                    while ((nobytes = bis.read(bytes, 0, bytes.length)) != -1) {
                        os.write(bytes, 0, nobytes);
                        totprog += nobytes;
                         tmp = (int) Math.round((totprog*100)/length);
                        publishProgress(tmp);
                    }
                    final String sentMsg = "File sent to: " + socket.getInetAddress();
                    Client.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(Client.this,
                                    sentMsg,
                                    Toast.LENGTH_LONG).show();
                        }});
                    socket.close();
                } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                Client.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Client.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            }
            catch (Exception e){
                final String eMsg = "Something wrong1: " + e.getMessage();
                Client.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Client.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});
            }
            finally {
                startActivity(new Intent(getApplicationContext(),Home.class));
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            super.onProgressUpdate(values);
            progress.setProgress(values[0]);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 123)
        {
            if (resultCode == RESULT_OK)
            {
                Uri uri = intent.getData();
                String type = intent.getType();
                if (uri != null)
                {
                    try {
                        String path = uri.getPath();
                        ClientRxThread clientRxThread =
                                new ClientRxThread(
                                        SocketServerAddress,
                                        SocketServerPORT);
                        clientRxThread.execute(path.substring(0,path.lastIndexOf("/"))+"/",uri.getLastPathSegment());
                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                        Log.i("Exceptionnn",e.getMessage());
                    }


                }
                else
                    Toast.makeText(getApplicationContext(),"errK",Toast.LENGTH_LONG).show();
            }
        }
        else
        Toast.makeText(getApplicationContext(),"err",Toast.LENGTH_LONG).show();
    }

    void connectToWifi(){

        ProgressDialog progress=new ProgressDialog(this);
        progress.setMessage("waiting for friend....");
        progress.setCancelable(false);
        progress.show();
        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
        if(wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);

        Toast.makeText(getApplicationContext(),"cnncdt0",Toast.LENGTH_LONG).show();
        final WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", WifiAccess.SSID);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        if(wifiManager.reconnect()) {
            progress.dismiss();
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("file/*");
            startActivityForResult(i, 123);
        }
   /*      final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    List<ScanResult> mScanResults = wifiManager.getScanResults();
                    for (ScanResult s:mScanResults){
                        if(s.SSID.equals(WifiAccess.SSID)){
                            found=true;

                        }
                    }
                    if(!found){
                        wifiManager.startScan();
                    }
                }
            }
        };

        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifiManager.startScan();*/



    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert=new AlertDialog.Builder(Client.this).setTitle("Warning!!");
        alert.setMessage("Are you sure you want to leave the page? Transmition Will Fail...")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Client.super.onBackPressed();
                        progress.cancel();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        progress.show();
                    }
                })
                .setCancelable(false)
                .show();

    }
}
