package com.example.ocrapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity {

    EditText mResultEt;
    EditText ncnhet;
    EditText catet;
    EditText cpfet;
    EditText estadoet;
    ImageView mPreviewIv;
    ImageView imgCortada;
    ImageView imgteste;
    ImageView testeEstado;
    ImageView testeCpf;
    ImageView testeCateg;
    ImageView numHabilit;


    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1001;

    String cameraPermission[];
    String storegePermission[];

    Uri image_uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("clique em + para adicionar uma imagem");

        mResultEt = findViewById(R.id.resultEt);
        mPreviewIv = findViewById(R.id.imageIv);
        imgteste=findViewById(R.id.teste);
        imgCortada = findViewById(R.id.testeimg);
        testeCpf = findViewById(R.id.testecpf);
        testeCateg=findViewById(R.id.testecategoria);
        numHabilit=findViewById(R.id.testenumhab);
        testeEstado=findViewById(R.id.testeestado);

        ncnhet = findViewById(R.id.ncnh);
        catet=findViewById(R.id.cat);
        cpfet=findViewById(R.id.cpf);
        estadoet=findViewById(R.id.estado);

        //permissão da camera
        cameraPermission = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //permissão do armazenamento
        storegePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    //actionbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflar menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    //handle actionbar item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.addImage){
            showImageImportDialog();
        }
        if(id==R.id.settings){
            Toast.makeText(this, "Configurações", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImageImportDialog() {
        String[] items = {" Camera", " Galeria"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        //titulo
        dialog.setTitle("Selecionar Imagem");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0){
                    //opção da camera selecionada
                    if(!checkCameraPermission()){
                        //se não houver permissão, solicita
                        requestCameraPermission();
                    }
                    else{
                        //permissão concedida para tirar a foto
                        pickCamera();

                    }

                }
                if(which==1){
                    //opção galeria selecionada
                    if(!checkStoregePermission()){
                        //se não houver permissão, solicita
                        requestStoragePermission();
                    }
                    else{
                        //permissão concedida para tirar a foto
                        pickGallery();

                    }
                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        //intent para obter imagem da galeria
        Intent intent = new Intent(Intent.ACTION_PICK);
        //setar o tipo de intent para a imagem
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NovaImg"); //Titulo da nova imagem
        values.put(MediaStore.Images.Media.DESCRIPTION, "Imagem para texto"); //descrição
        image_uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storegePermission, STORAGE_REQUEST_CODE);

    }

    private boolean checkStoregePermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void reconhecer(Bitmap bitmap, EditText texto ){
        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!recognizer.isOperational()){
            Toast.makeText(this, "Erro", Toast.LENGTH_SHORT).show();
        }
        else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> items = recognizer.detect(frame);
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<items.size(); i++){
                TextBlock myItem = items.valueAt(i);
                sb.append(myItem.getValue());
                sb.append("\n");
            }
            //enviar texto para o EditText
            texto.setText(sb.toString());
        }
    }
    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length>0){
                    boolean cameraAccepted = grantResults[0]==
                            PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0]==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }
                    else {
                        Toast.makeText(this, "permissão negada", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if(grantResults.length>0){
                    boolean writeStorageAccepted = grantResults[0]==
                            PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickGallery();
                    }
                    else {
                        Toast.makeText(this, "permissão negada", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    //handle image result


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode==RESULT_OK){
            if(requestCode==IMAGE_PICK_GALLERY_CODE){
                // receber imagem da galeria e cortar
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            if(requestCode==IMAGE_PICK_CAMERA_CODE){
                // receber imagem da camera e cortar
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        //.setInitialCropWindowRectangle(rect)
                        .start(this);
            }
        }
        //obter imagem cortada
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result= CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK){
                Uri resultUri = result.getUri();
                //enviar a imagem para o ImageView
                mPreviewIv.setImageURI(resultUri);
                //Obter Imagem para o reconhecimento
                BitmapDrawable bitmapDrawable = (BitmapDrawable)mPreviewIv.getDrawable();
                Bitmap bitmap2 = bitmapDrawable.getBitmap();

                //escalando o bitmap para a largura de 860
                int whidth=860;
                float fator = whidth/(float) bitmap2.getWidth();
                Bitmap fff = Bitmap.createScaledBitmap(bitmap2,whidth,(int) (bitmap2.getHeight()*fator),true);

                //criação dos bitmaps conforme as posições dos campos desejados e aumento das medidas dos mesmos

                //Campo de Nome presente na CNH
                Bitmap bitmap1=Bitmap.createBitmap(fff, 140,165, 650,30);
                Bitmap bitmap=Bitmap.createScaledBitmap(bitmap1,bitmap1.getWidth()*3,bitmap1.getHeight()*3,true);
                //Campo de categoria da CNH
                Bitmap categoria1=Bitmap.createBitmap(fff, 720,475, 70,30);
                Bitmap categoria = Bitmap.createScaledBitmap(categoria1,categoria1.getWidth()*3,categoria1.getHeight()*3,true);
                //Campo CPF
                Bitmap cpf1=Bitmap.createBitmap(fff, 420,275, 220,30);
                Bitmap cpf=Bitmap.createScaledBitmap(cpf1,cpf1.getWidth()*3,cpf1.getHeight()*3,true);
                //Campo de foto
                Bitmap foto=Bitmap.createBitmap(fff, 140,200, 220,310);
                //Campo de Numero da CNH
                Bitmap numeroCnh1=Bitmap.createBitmap(fff, 55,320, 50,300);
                Bitmap numeroCnh=Bitmap.createScaledBitmap(numeroCnh1,numeroCnh1.getWidth()*3,numeroCnh1.getHeight()*3,true);
                //Campo de Estado da CNH
                Bitmap estadocnh1=Bitmap.createBitmap(fff, 300,1090, 330,50);
                Bitmap estadocnh=Bitmap.createScaledBitmap(estadocnh1,estadocnh1.getWidth()*3,estadocnh1.getHeight()*3,true);

                //função para rotacionar a imagem
                Matrix matrix = new Matrix();
                matrix.setRotate(180);
                matrix.setRotate(90);
                Bitmap rotacionado1 = Bitmap.createBitmap(numeroCnh,0,0,numeroCnh.getWidth(),numeroCnh.getHeight(),matrix,true);
                Bitmap rotacionado=Bitmap.createScaledBitmap(rotacionado1,rotacionado1.getWidth()*3,rotacionado1.getHeight()*3,true);

                //enviar imagens obtidas para imageviews correspondentes
                imgCortada.setImageBitmap(foto);
                imgteste.setImageBitmap(bitmap);
                testeCpf.setImageBitmap(cpf);
                testeCateg.setImageBitmap(categoria);
                numHabilit.setImageBitmap(rotacionado);
                testeEstado.setImageBitmap(estadocnh);

                //reconhecimento dos textos obtidos nas imagens e envio dos mesmos para os campos de textos correspondentes
                reconhecer(bitmap,mResultEt);
                reconhecer(rotacionado,ncnhet);
                reconhecer(categoria,catet);
                reconhecer(cpf,cpfet);
                reconhecer(estadocnh,estadoet);

            }
            else if(resultCode==CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                //se ocorrer algum erro, mostra qual
                Exception error =result.getError();
                Toast.makeText(this, ""+error, Toast.LENGTH_SHORT).show();

            }
        }
    }
}


