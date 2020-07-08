package net.anas.pfeshoping;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Map<String, Object> produit = new HashMap<>();
    private String TAG;
    private ImageView imageView;
    private EditText fieldName,fieldDesignation,fieldCategorie,fieldPrix;
    private Uri imageUri;
    private Button buttonUpload;
    private static final int PICK_IMAGE_REQUEST=1;
    private ImageButton buttonUploadImage;
    private ProgressBar progressBar;
    private StorageReference storageReference=null;
    private DatabaseReference databaseReference=null;
    private FirebaseAuth mAuth=null;
    private FirebaseFirestore db=null;

    private void openImgechooser(){
        Intent intent=new Intent(  );
        intent.setType( "image/*" );
        intent.setAction( Intent.ACTION_GET_CONTENT );
        startActivityForResult( intent,PICK_IMAGE_REQUEST );
    }

    private String getFileExt(Uri uri){
        ContentResolver contentResolver=getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType( contentResolver.getType( uri ) );
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                // do your stuff
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                    }
                });
    }

    private  void uploadProduit(final DatabaseReference databaseReference,final StorageReference storageReference){
        if(imageUri != null) {
            StorageReference fileReference = storageReference.child(System.currentTimeMillis()+"."+getFileExt(imageUri));
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri DownloadLink = taskSnapshot.getUploadSessionUri();
                            String imgUrl = DownloadLink.toString();
                            FirebaseDatabase.getInstance().getReference().child("image_link").setValue(imgUrl);
                            TAG = "produit";
                            // Create a new user with a first and last name
                            produit.put("name", fieldName.getText().toString().trim());
                            produit.put("designation", fieldDesignation.getText().toString().trim());
                            produit.put("categorie", fieldCategorie.getText().toString().trim());
                            produit.put("prix", fieldPrix.getText().toString());
                            produit.put("image", taskSnapshot.getUploadSessionUri().toString());
                            // Add a new document with a generated ID
                            db.collection("produits")
                                    .add(produit)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                                }
                            })
                    .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error adding document", e);
                                    Toast.makeText( MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT ).show();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText( MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT ).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                        }
                    });
        }else{
            Toast.makeText( MainActivity.this,"no fil selected",Toast.LENGTH_SHORT ).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // do your stuff
        } else {
            signInAnonymously();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // connect to db
        db = FirebaseFirestore.getInstance();
        // get res references
        imageView = findViewById(R.id.imageView);
        buttonUpload = findViewById(R.id.buttonUpload);
        fieldName = findViewById(R.id.fieldName);
        fieldDesignation = findViewById(R.id.fieldDesignation);
        fieldCategorie = findViewById(R.id.fieldCategorie);
        fieldPrix = findViewById(R.id.fieldPrix);
        buttonUploadImage = findViewById(R.id.buttonUploadImage);
        progressBar = findViewById(R.id.progressBar);
        databaseReference= FirebaseDatabase.getInstance().getReference("produits");
        storageReference = FirebaseStorage.getInstance().getReference("/images");
        mAuth = FirebaseAuth.getInstance();

        buttonUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImgechooser();
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                uploadProduit(databaseReference,storageReference);
               Intent intent=new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    @Override
    protected void onActivityResult( int requestCode, int resultCode, @Nullable Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==PICK_IMAGE_REQUEST || requestCode==RESULT_OK || data!=null || data.getData()!=null){
            imageUri=data.getData();
            imageView.setImageURI(imageUri);
        }
    }
}