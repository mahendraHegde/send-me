package mahe.sendme;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class Server extends AppCompatActivity {
   ProgressDialog progress,progress1;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static final int SocketServerPORT = 2000;
    ServerSocket serverSocket;

    ServerSocketThread serverSocketThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WifiAccess.setWifiApState(getApplicationContext(), true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        progress=new ProgressDialog(this);
        progress1=new ProgressDialog(this);

        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    Server.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        progress1.setMessage("waiting for friend....");
        progress1.setCanceledOnTouchOutside(false);
        progress1.show();
        serverSocketThread = new ServerSocketThread();
        serverSocketThread.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiAccess.setWifiApState(getApplicationContext(), false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    public class ServerSocketThread extends Thread {
        String filePath=null;
        ServerSocketThread(){
        }

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);

                while (true) {
                    socket = serverSocket.accept();
                    if(socket!=null) {
                        final Socket finalSocket = socket;
                        Server.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FileTxThread fileTxThread=new FileTxThread(finalSocket);
                                fileTxThread.execute(filePath);
                            }});
                    }



                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }



    public class FileTxThread extends AsyncTask<String,Integer,String> {
        Socket socket;

        FileTxThread(Socket socket){
            this.socket= socket;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                Server.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress1.dismiss();
                        progress.setIndeterminate(false);
                        progress.setMax(100);
                        progress.setCanceledOnTouchOutside(false);
                        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progress.show();
                    }});

            }catch (Exception e){
                final  String msg=e.getMessage();
                Server.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"exce : "+msg,Toast.LENGTH_LONG).show();
                    }});
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progress.dismiss();

        }

        @Override
        protected String doInBackground(String... strings) {

            try {


                byte[] bytes = new byte[1024];
                final byte b[]=new byte[1024];

                InputStream is = socket.getInputStream();
                OutputStream out=socket.getOutputStream();



                int bytesRead;
                long length,totprog=0;
                final File file;

                bytesRead=is.read(b,0,b.length);
                file=new File(Environment.getExternalStorageDirectory(),new String(b,0,bytesRead));
                out.write("s".getBytes());
                bytesRead=is.read(b,0,b.length);
                length=Long.parseLong(new String(b,0,bytesRead));
                out.write("s".getBytes());
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                while ((bytesRead= is.read(bytes, 0, bytes.length))!=-1){
                    bos.write(bytes,0,bytesRead);
                    totprog += bytesRead;
                   int tmp = (int) Math.round((totprog*100)/length);
                    publishProgress(tmp);
                }
                Server.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Server.this,
                                "File Received",
                                Toast.LENGTH_LONG).show();
                    }});
                bos.close();
                socket.close();

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }catch (Exception e){
                final String eMsg = "Something wrong1: " + e.getLocalizedMessage();
                Server.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Server.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});
            }
            finally {
                startActivity(new Intent(getApplicationContext(),Home.class));
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            super.onProgressUpdate(values);
            progress.setProgress(values[0]);

        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert=new AlertDialog.Builder(Server.this).setTitle("Warning!!");
        alert.setMessage("Are you sure you want to leave the page? Transmition Will Fail...")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Server.super.onBackPressed();
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

