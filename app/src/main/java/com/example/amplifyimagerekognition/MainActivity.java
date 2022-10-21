package com.example.amplifyimagerekognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.util.IOUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvFileName;
    private TextView uploadsel;
    private TextView filenameenter;
    private EditText edtFileName;
    private ImageView cameraImg;
    private ListView listView;

    private String KEY;
    private String SECRET;

    private String host;
    private String clientId;
    private String userName;
    private String passWord;
    private String PUB_TOPIC;
    private String SUB_TOPIC;
   // private String imageFileNameExtension="androidphoto1.jpg";
    private static String s3FileImage;

    private AmazonS3Client s3Client;
    private BasicAWSCredentials credentials;

    private Uri fileUri;
    private Bitmap bitmap;

    MqttAndroidClient mqttAndroidClient;
    //track Choosing Image Intent
    private static final int CHOOSING_IMAGE_REQUEST = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtFileName = findViewById(R.id.edt_file_name);
        tvFileName = findViewById(R.id.tv_file_name);
        uploadsel = findViewById(R.id.upload_select);
        filenameenter = findViewById(R.id.enterfilename);
        cameraImg = findViewById(R.id.camera_img);
        listView = findViewById(R.id.listView);
        tvFileName.setText("");
        uploadsel.setText("");
        filenameenter.setText("");

        try {
            Util.initialize(this.getApplicationContext());
            KEY = Util.getProperty("KEY");
            SECRET = Util.getProperty("SECRET");
            host = Util.getProperty("BROKERIP")+":"+Util.getProperty("PORT");
            PUB_TOPIC = Util.getProperty("PUBLISHTOPIC");
            SUB_TOPIC = Util.getProperty("SUBSCRIBETOPIC");
        }
        catch (IOException e) {
          e.printStackTrace();
        }

        Log.i("keyprop",KEY);
        Log.i("secretprop",SECRET);
        Log.i("hostprop",host);
        Log.i("PUB_TOPICprop",PUB_TOPIC);
        Log.i("subscribeTopicprop",SUB_TOPIC);


        findViewById(R.id.btn_choose_file).setOnClickListener(this);
        findViewById(R.id.btn_upload).setOnClickListener(this);
        findViewById(R.id.btn_analyze).setOnClickListener(this);

        AWSMobileClient.getInstance().initialize(this).execute();

        credentials = new BasicAWSCredentials(KEY, SECRET);
        s3Client = new AmazonS3Client(credentials);

        mqttConnect();

    }


    @Override
    public void onClick(View view) {

        int i = view.getId();

        if (i == R.id.btn_choose_file) {
            showChoosingFile();
        } else if (i == R.id.btn_upload) {
             uploadFile();
        } else if (i == R.id.btn_analyze) {
            if(s3FileImage != null && !s3FileImage.isEmpty()) {
                Log.i("s3imagefile", s3FileImage);
                filenameenter.setText("");
                tvFileName.setText("");
                publishMessage(s3FileImage);
            }
        }
    }

    private void showChoosingFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), CHOOSING_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (bitmap != null) {
            bitmap.recycle();
        }

        if (requestCode == CHOOSING_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            if(fileUri != null)
            {
                uploadsel.setText("File selected");
            }
            Log.i("fileuri",fileUri.getPath());
            Toast.makeText(this, "File Selected!" + fileUri.getPath(), Toast.LENGTH_SHORT).show();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void createFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            IOUtils.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile() {

        if (fileUri != null) {
            uploadsel.setText("File selected");
            final String fileName = edtFileName.getText().toString();

            if (fileName.equals("")) {
                filenameenter.setText("Enter filename");
            } else {
                filenameenter.setText("");
                s3FileImage = fileName + "." + getFileExtension(fileUri);
                Log.i("s3uploadimg",s3FileImage);
                final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "/" + fileName);

            createFile(getApplicationContext(), fileUri, file);

            TransferUtility transferUtility =
                    TransferUtility.builder()
                            .context(getApplicationContext())
                            .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                            .s3Client(s3Client)
                            .build();

            TransferObserver uploadObserver =
                    transferUtility.upload( fileName + "." + getFileExtension(fileUri), file);

            uploadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        uploadsel.setText("upload completed");
                        filenameenter.setText("");
                        edtFileName.setText("");
                        file.delete();
                    } else if (TransferState.FAILED == state) {
                        uploadsel.setText("upload failed");
                        edtFileName.setText("");
                        filenameenter.setText("");
                        file.delete();
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;

                    tvFileName.setText("ID:" + id + "|bytesCurrent: " + bytesCurrent + "|bytesTotal: " + bytesTotal + "|" + percentDone + "%");
                }

                @Override
                public void onError(int id, Exception ex) {
                    ex.printStackTrace();
                }

            });
          }
        }
        else{
            uploadsel.setText("Please select an image");
            Log.i("not","selected");
        }
    }
    private void mqttConnect()
    {
        /* Obtain the MQTT connection parameters clientId, username, and password. */
        AiotMqttOption aiotMqttOption = new AiotMqttOption();
            clientId = aiotMqttOption.getClientId();
            userName = aiotMqttOption.getUsername();
            passWord = aiotMqttOption.getPassword();

            Log.i("clientid",clientId);
            Log.i("userName",userName);
            Log.i("password",passWord);

        /* Create an MqttConnectOptions object and configure the username and password. */
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(passWord.toCharArray());

        /* Create an MqttAndroidClient object and configure the callback. */
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i( "connection","lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i("Messagereceived", topic+new String(message.getPayload()));

                tvFileName.setText("");
                edtFileName.setText("");
                ArrayList<LabelModel> labelNames = new ArrayList<>();
               // List<String> allNames = new ArrayList<String>();
                JSONObject jsonResponse = new JSONObject(new String(message.getPayload()));

                JSONArray row = jsonResponse.getJSONArray("Labels");
                for (int i=0; i<row.length(); i++) {
                    JSONObject obj = row.getJSONObject(i);
                    String name = obj.getString("Name");
                    String confidence = obj.getString("Confidence");
                    LabelModel model = new LabelModel();
                    model.setLabelName(name);
                    model.setConfidence(confidence);
                    labelNames.add(model);
                  //  allNames.add(obj.getString("Name"));
                }
                CustomAdapter adapter = new CustomAdapter(getApplicationContext(), labelNames);
                listView.setAdapter(adapter);

                /**for (int i = 0; i < allNames.size();i++)
                {
                    Log.i("Label",+i+""+allNames.get(i));
                }**/
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i( "msg","delivered");
            }
        });

        /* Establish a connection to IoT Platform by using MQTT. */
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i( "connect","succeed");

                    subscribeTopic(SUB_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i( "connect", "failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i( "subscribed", "succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i( "subscribed", "failed");
                }
            });

        } catch (MqttException e) {
            Log.e("mqttexceptionsub", e.toString());
            e.printStackTrace();
        }
    }

    private void publishMessage(String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i( "publish", "succeed") ;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i( "publish", "failed") ;
                }
            });
        } catch (MqttException e) {
            Log.e("mqttexceptionpub", e.toString());
            e.printStackTrace();
        }
    }

}
