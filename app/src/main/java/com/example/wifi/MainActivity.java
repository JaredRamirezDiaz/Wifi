package com.example.wifi;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.InetAddresses;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Console;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnEstado, btnEnviar,btnDiscover;
    EditText editMensaje, editEscribir;
    ListView lista;
    TextView titulo;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver broadcastReceiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> listaPeers = new ArrayList<>();
    String []arregloNombreDispocitivo;
    WifiP2pDevice[] arregloDispocitivos;
    ServerClass serverClass;
    Clientclass clientclass;
    EnviarMEnsaje enviarMEnsaje;

    static  final int LEER_MENSAJE=1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDiscover = findViewById(R.id.BtnDiscoveri);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnEstado = findViewById(R.id.BtnOfOn);
        editEscribir = findViewById(R.id.editEnviar);
        editMensaje = findViewById(R.id.editMensaje);
        lista = findViewById(R.id.ListView);
        titulo= findViewById(R.id.txtTitulo);

        //INICIALIZAMOS EL WIFI MANAGER
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getApplicationContext().getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);

        //INSTANCIA ASIA LA CLASE DEL BROADCAST
        broadcastReceiver = new WiFiDirectBroadcastReceiver(mManager,mChannel,this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        //VALIDAMOS QUE EL WIFI ESTE ENSENDIDO(el boton del programa que simula el estado del wifi)

        EstadoWIfi();

    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case LEER_MENSAJE:
                    byte[] bufferLeer=(byte[]) msg.obj;
                    String mensajeTemp=new String(bufferLeer,0,msg.arg1);
                    editMensaje.setText(mensajeTemp);
                    Toast.makeText(MainActivity.this, mensajeTemp, Toast.LENGTH_SHORT).show();
                    break;
            }

            return true;
        }
    });

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            if (!peerList.getDeviceList().equals(listaPeers)) {
                listaPeers.clear();
                listaPeers.addAll(peerList.getDeviceList());

                arregloNombreDispocitivo = new String[peerList.getDeviceList().size()];
                arregloDispocitivos = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    arregloNombreDispocitivo[index] = device.deviceName;
                    arregloDispocitivos[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, arregloNombreDispocitivo);
                lista.setAdapter(adapter);
            }
                    if (listaPeers.size() == 0 ){
                        Toast.makeText(MainActivity.this, "No encontrado el dispositivo",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
    };

    private void EstadoWIfi() {
        //
        btnEstado.setOnClickListener(new View.OnClickListener() {
            //SI EL WIFI DE NUESTRO DISPOSITIVO ESTA ENCENDIDO NO PASARA NADA, SI NO NOS PEDIRA QUE LO ACTIVEMOS
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnEstado.setText("ON");
                }else{
                    wifiManager.setWifiEnabled(true);
                    btnEstado.setText("OF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        titulo.setText("Dispositivo Iniciado");
                    }

                    @Override
                    public void onFailure(int reason) {
                        titulo.setText("Iniciacion del Dispocitivo fallida");
                    }
                });
            }
        });

        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice dispositivo = arregloDispocitivos[position];
                WifiP2pConfig configuracion = new WifiP2pConfig();
                configuracion.deviceAddress=dispositivo.deviceAddress;
                mManager.connect(mChannel, configuracion, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Conectado a" + dispositivo.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Fallo al conectar", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg= editEscribir.getText().toString();

                enviarMEnsaje.leer(msg.getBytes());
            }
        });
    }


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress grupo = info.groupOwnerAddress;

            if(info.groupFormed && info.isGroupOwner){
                titulo.setText("host");
                serverClass=new ServerClass();
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                serverClass.start();
            }else if (info.groupFormed){
                titulo.setText("cliente");
                Clientclass clientclass= new Clientclass(grupo);
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                clientclass.start();
            }
        }
    };

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    public class ServerClass extends Thread{

        Socket socket;

        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                enviarMEnsaje=new EnviarMEnsaje(socket);
                enviarMEnsaje.start();
            }catch (Exception e){
                Toast.makeText(MainActivity.this, "Fallo en el cliente", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class EnviarMEnsaje extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public EnviarMEnsaje(Socket socket){
            this.socket=socket;
            try {
                inputStream= this.socket.getInputStream();
                outputStream= this.socket.getOutputStream();
            }catch (Exception e){
                Toast.makeText(MainActivity.this, "error 1", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while(socket!=null){
                try {
                    bytes=inputStream.read(buffer);
                    if (bytes>0){
                        Toast.makeText(MainActivity.this, "leyendo mensaje", Toast.LENGTH_SHORT).show();
                        handler.obtainMessage(LEER_MENSAJE,bytes,-1,buffer).sendToTarget();
                    }
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "error 2", Toast.LENGTH_SHORT).show();

                }
            }
        }

        public void leer(byte[] bytes){
            try {
                outputStream.write(bytes);
            }catch (Exception e){

                e.printStackTrace();
                Toast.makeText(MainActivity.this, "error 3", Toast.LENGTH_SHORT).show();

            }

        }
    }

    public class Clientclass extends Thread{

        Socket socket;
        String direccionHost;

        public Clientclass(InetAddress inetAddress){
            this.direccionHost=inetAddress.getHostAddress();
            socket=new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(direccionHost,8888),500);
                enviarMEnsaje= new EnviarMEnsaje(socket);
                enviarMEnsaje.start();
            }catch (Exception e){
                Toast.makeText(MainActivity.this, "Fallo en el cliente", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
