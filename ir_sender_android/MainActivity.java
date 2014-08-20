package com.example.android_bluetooth_ir_sender_02;

// IR Sender, Android Client
// (C) INOUE Hirokazu
// GNU GPL Free Software
//
// Version 0.1   2014/08/16
// Version 0.2   2014/08/20

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice mBluetoothDevice = null;
    BluetoothSocket mBluetoothSocket = null;
    ListView listView_Codes = null;
    ArrayList<String> arrayKey = new ArrayList<String>();   // リモコンのボタン名
    ArrayList<String> arrayData = new ArrayList<String>();  // 送信データ
    // @timing/@paramの初期値はここではなく Init_Param_and_Timing() で設定される
    byte[] byteTiming = {90, 45, 28, 84, 28};               // 各信号の持続時間 (St-hi*100, St-lo*100, 1/0-hi*20, 1-lo*20, 0-lo*20)
    byte[] byteParam = {0x01, 0x19, 0x08};                  // 制御 (mode 1:normal 2:pwm test 4:signal test, PR2, CCPR1L) 
    final int REQUEST_ENABLE_BLUETOOTH = 0x1010;    // BluetoothAdapter.ACTION_REQUEST_ENABLE の識別用
    final int REQUEST_FILE_SELECT = 0x1020;         // Intent.ACTION_GET_CONTENT の識別用
    long time_last_sent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ファイル選択と、CSVファイルを読み込み配列に格納する
        Make_Dummy_arrayKey();
        Intent fileSelectIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileSelectIntent.setType("file/*");
        startActivityForResult(fileSelectIntent, REQUEST_FILE_SELECT);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // BluetoothをONにし、デバイスを選択するリスト画面を表示
        if(mBluetoothAdapter.equals(null)){
            Toast.makeText(MainActivity.this, "Bluetooth機能が利用できません", Toast.LENGTH_LONG).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            // BluetoothをONにするよう、ダイアログを表示してユーザ選択を促す
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
        else {
            // BluetoothがすでにONの場合
            Find_Select_Bluetooth_Device();     // ペアリング済みのBluetoothデバイス一覧表示とユーザ選択
        }
    }

    // Intentの結果を受け取る
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // BluetoothをONにするIntentのユーザ選択の結果を受ける
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if(resultCode == Activity.RESULT_OK){
                Find_Select_Bluetooth_Device();     // ペアリング済みのBluetoothデバイス一覧表示とユーザ選択
            }
            else {
                Toast.makeText(MainActivity.this, "BluetoothがOFFです", Toast.LENGTH_LONG).show();
            }
        }
        // ファイル選択の結果を受け、CSVファイルを読み込んで配列に格納する
        else if(requestCode == REQUEST_FILE_SELECT){
            if(resultCode == Activity.RESULT_OK){
                String path = data.getData().getPath();
                Toast.makeText(MainActivity.this, "CSV = " + path, Toast.LENGTH_LONG).show();
                Read_csv_Data(path);
                if(listView_Codes != null){
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView_Codes.getAdapter();
                    adapter.clear();
                    for(int i=0; i<arrayKey.size(); i++){
                        adapter.add(arrayKey.get(i));
                    }
                }
            }
            else{
                Make_Dummy_arrayKey();      // ダミーデータ1件を登録する
                Toast.makeText(MainActivity.this, "リモコン定義ファイルが選択されませんでした", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if(mBluetoothSocket != null) {
            try{
                mBluetoothSocket.close();
            } catch(Exception e) {
                
            }
            Toast.makeText(MainActivity.this, "Bluetooth Socket を閉じました", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.menu_read_csv){
            if(listView_Codes != null){
                Intent fileSelectIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fileSelectIntent.setType("file/*");
                startActivityForResult(fileSelectIntent, REQUEST_FILE_SELECT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ペアリング済みのBluetoothデバイス一覧表示とユーザ選択
    private void Find_Select_Bluetooth_Device(){

        // ペアリング済みのアダプタ一覧を得る
        final Set<BluetoothDevice> btDevices = mBluetoothAdapter.getBondedDevices();

        ListView listView = new ListView(this);
        setContentView(listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        if(btDevices.size() > 0){
            // ListViewにアダプタの名称とアドレスを格納する
            for (BluetoothDevice device : btDevices) {
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else{
            Toast.makeText(MainActivity.this, "ペアリング済みのBluetoothアダプタが見つからない", Toast.LENGTH_LONG).show();
            return;
        }

        // ListViewでユーザがクリックした時の処理 → デバイスに接続する
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                ListView listView = (ListView) parent;
                // クリックされたアイテムを取得します
                String item = (String) listView.getItemAtPosition(position);
                String[] itemArray = item.split("\n");
                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(itemArray[1]);
                adapter.clear();
                if(mBluetoothDevice.equals(null)){
                    Toast.makeText(MainActivity.this, "BluetoothDevice選択内部エラー", Toast.LENGTH_LONG).show();
                }
                else{
                    // 選択したBluetoothデバイスに接続する
                    Connect_to_Device();
                }
            }
        });
    }

    // Bluetoothデバイスに接続し、接続後にコード送信画面に遷移する
    private void Connect_to_Device(){
        // Base UUID = "xxxxxxxx-0000-1000-8000-00805F9B34FB"
        // Service Classの例
        //      SPP : 0x00001101
        //      DUN : 0x00001103
        //      HID : 0x00001124

        try{
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch(Exception e){
            mBluetoothSocket = null;
            Toast.makeText(MainActivity.this, "SPP Socket createエラー", Toast.LENGTH_LONG).show();
            return;
        }
        
        try{
            mBluetoothSocket.connect();
        } catch(Exception e){
            mBluetoothSocket = null;
            Toast.makeText(MainActivity.this, "SPP Socket connectエラー", Toast.LENGTH_LONG).show();
            return;
        }
        
        Toast.makeText(MainActivity.this, "接続完了", Toast.LENGTH_LONG).show();

        // コード送信選択画面に遷移する
        Select_Send_Code();
    }

    // コード送信選択リスト画面の表示と、コード送信
    private void Select_Send_Code() {
        listView_Codes = new ListView(this);
        setContentView(listView_Codes);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        for(int i=0; i<arrayKey.size(); i++){
            adapter.add(arrayKey.get(i));
        }
        
        // ListViewでユーザがクリックした時の処理
        listView_Codes.setAdapter(adapter);
        listView_Codes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                if(time_last_sent >= System.currentTimeMillis() - 1500){
                    // 連続送信の禁止 （1500ミリ秒）
                    // （本来なら、送信機からのready文字列受信用スレッドを建てるべきだが、面倒なので…）
                    return;
                }
                time_last_sent = System.currentTimeMillis();

                if(position < arrayData.size()){
                    String [] arrayByte = arrayData.get(position).split(",");
                    if(arrayByte.length > 170){
                        Toast.makeText(MainActivity.this, "error:データが170Bytes以上", Toast.LENGTH_LONG).show();
                        return;
                    }
                    OutputStream outputStream;
                    try{
                        outputStream = mBluetoothSocket.getOutputStream();
                        // タイミングデータの送信
                        for(int j=0; j<byteTiming.length; j++){
                            outputStream.write(byteTiming[j]);
                        }
                        // モード及びPWMパラメータの送信
                        for(int j=0; j<byteParam.length; j++){
                            outputStream.write(byteParam[j]);
                        }
                        // IR信号データの送信
                        for(int j=0; j<arrayByte.length; j++){
                            outputStream.write((byte)Integer.parseInt(arrayByte[j], 16));
                        }
                        // データはNULLで終わる
                        outputStream.write(0x00);
                        
                        Toast.makeText(MainActivity.this, arrayKey.get(position), Toast.LENGTH_SHORT).show();
                    } catch (Exception e){
                        Toast.makeText(MainActivity.this, "送信エラー", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                time_last_sent = System.currentTimeMillis();
            }
        });

    }

    // CSVファイルを読み込んで配列に格納する
    private void Read_csv_Data(String filepath){
        arrayKey.clear();
        arrayData.clear();
        Init_Param_and_Timing();
        ActionBar ab = getSupportActionBar();   // Android 2.3対応
        String [] arrayPath = filepath.split("/");
        ab.setTitle(arrayPath[arrayPath.length-1]);     // タイトルバーのタイトル設定（とりあえずファイル名）

        try{
            FileInputStream fileInputStream = new FileInputStream(filepath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
            String strTemp;
            while ((strTemp = reader.readLine() ) != null){
                if(strTemp.charAt(0) == (char)'#'){ continue; }     // コメント行
                else if(strTemp.charAt(0) == (char)'@'){
                    // パラメータ行の解析
                    Parse_Param(strTemp);
                    continue;
                }
                String[] str1 = strTemp.split("=");
                arrayKey.add(str1[0].trim());
                arrayData.add(str1[1].trim());
            }
            reader.close();
            fileInputStream.close();
        } catch(Exception e){
            Make_Dummy_arrayKey();
        }
    }

    // CSVファイルの「@」で始まるコントロール行を読み込む処理
    private void Parse_Param(String str){
        String[] str1 = str.split("=");
        if(str1[0].trim().equals("@timing")){
            String [] str2 = str1[1].trim().split(",");
            if(str2.length != 5){ return; }     // 信号長パラメータは5個
            byteTiming[0] = (byte)(Integer.parseInt(str2[0], 10)/100);
            byteTiming[1] = (byte)(Integer.parseInt(str2[1], 10)/100);
            byteTiming[2] = (byte)(Integer.parseInt(str2[2], 10)/20);
            byteTiming[3] = (byte)(Integer.parseInt(str2[3], 10)/20);
            byteTiming[4] = (byte)(Integer.parseInt(str2[4], 10)/20);
            Toast.makeText(MainActivity.this, 
                    String.format("timing=%d,%d,%d,%d,%d", byteTiming[0],byteTiming[1],byteTiming[2],byteTiming[3],byteTiming[4]),
                    Toast.LENGTH_LONG).show();
        }
        else if(str1[0].trim().equals("@param")){
            String [] str2 = str1[1].trim().split(",");
            if(str2.length != 3){ return; }     // 信号長パラメータは5個
            byteParam[0] = (byte)(Integer.parseInt(str2[0], 10));
            byteParam[1] = (byte)(Integer.parseInt(str2[1], 10));
            byteParam[2] = (byte)(Integer.parseInt(str2[2], 10));
            Toast.makeText(MainActivity.this, 
                    String.format("mode=%02X, PR2=%02X, CCPR1L=%02X", byteParam[0],byteParam[1],byteParam[2]),
                    Toast.LENGTH_LONG).show();
        }
        else if(str1[0].trim().equals("@title")){
            ActionBar ab = getSupportActionBar();   // Android 2.3対応
            ab.setTitle(str1[1].trim());       // タイトルバーのタイトル文字列設定
        }
    }

    // CSV読み込み用配列にテスト用ダミーデータを登録する
    private void Make_Dummy_arrayKey(){
        arrayKey.clear();
        arrayData.clear();
        arrayKey.add("Sample Data");
        arrayData.add("5E,A1,54,AB");       // テストデータ (NEC format)
    }

    private void Init_Param_and_Timing(){
        // NECフォーマット時の信号Hi/Lo持続時間（マイクロ秒）をデフォルト設定
        byteTiming[0] = (byte)90;   // スタートビット Hi 90 * 100us
        byteTiming[1] = (byte)45;   // スタートビット Lo 45 * 100us
        byteTiming[2] = (byte)28;   // データ 1/0 Hi 28 * 20us
        byteTiming[3] = (byte)84;   // データ 1 Lo 84 * 20us
        byteTiming[4] = (byte)28;   // データ 0 Lo 28 * 20us

        byteParam[0] = (byte)0x01;
        byteParam[1] = (byte)0x19;  // PWM周波数パラメータ PR2 値
        byteParam[2] = (byte)0x08;  // PWMデューティ比パラメータ CCPR1L 値
    }
}
