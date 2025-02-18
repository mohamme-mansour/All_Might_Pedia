package com.mohammedev.allmightpedia.ui.profile;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mohammedev.allmightpedia.Adapters.PostsAdapter;
import com.mohammedev.allmightpedia.R;
import com.mohammedev.allmightpedia.data.FanArtPost;
import com.mohammedev.allmightpedia.data.User;
import com.mohammedev.allmightpedia.utils.CurrentUserData;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifImageView;


public class ProfileFragment extends Fragment {
    private static final int GET_IMAGE_STORAGE_CODE = 101;

    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    private ShapeableImageView userImage;
    private TextView userName, userBio, userEmail;
    private Button editButton;
    private RecyclerView recyclerView;
    private ConstraintLayout constraintLayout;
    private ViewStub viewStub;
    private EditText editProfileNameEditText, editProfileBioEditText;
    private ImageView editProfileImageView;
    private GifImageView noResultGifImage;
    private TextView noResultTextView;
    private Uri imageUri;
    public PostsAdapter postsAdapter;
    ArrayList<FanArtPost> fanArtPostArrayList = new ArrayList<>();
    ShimmerFrameLayout postsShimmerLayout;
    ShimmerFrameLayout userImageShimmerLayout;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_profile, container, false);

        userImage = view.findViewById(R.id.user_image_profile);
        userName = view.findViewById(R.id.user_name_profile);
        userBio = view.findViewById(R.id.user_bio_profile);
        userEmail = view.findViewById(R.id.user_email_profile);
        editButton = view.findViewById(R.id.edit_profile_btn);
        noResultGifImage = view.findViewById(R.id.no_posts_gif);
        noResultTextView = view.findViewById(R.id.no_result_txt);
        recyclerView = view.findViewById(R.id.posts_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        constraintLayout = view.findViewById(R.id.profile_fragment_layout);

        postsShimmerLayout = view.findViewById(R.id.shimmer_view_container);
        postsShimmerLayout.setVisibility(View.VISIBLE);
        postsShimmerLayout.startShimmer();

        userImageShimmerLayout = view.findViewById(R.id.shimmer_user_image_view_container);
        userImageShimmerLayout.setVisibility(View.VISIBLE);
        userImageShimmerLayout.startShimmer();

        // included layout
        viewStub = view.findViewById(R.id.edit_profile_view_stub);
        viewStub.setLayoutResource(R.layout.edit_profile_layout);
        View inflated = viewStub.inflate();
        inflated.setVisibility(View.INVISIBLE);
        editProfileNameEditText = inflated.findViewById(R.id.user_name_edit_profile);
        editProfileImageView = inflated.findViewById(R.id.user_edit_image_profile);
        editProfileBioEditText = inflated.findViewById(R.id.user_bio_profile);
        /**--------------------------------------------------------------------------**/

        setUserData();

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editProfileNameEditText.setText(userName.getText().toString());
                editProfileBioEditText.setText(userBio.getText().toString());
                editProfileImageView.setImageDrawable(userImage.getDrawable());
                inflated.setVisibility(View.VISIBLE);
                constraintLayout.setVisibility(View.INVISIBLE);
            }
        });

        inflated.findViewById(R.id.edit_profile_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUserNameString = editProfileNameEditText.getText().toString();
                String newUserBioString = editProfileBioEditText.getText().toString();
                editProfile(CurrentUserData.USER_UID, newUserNameString, newUserBioString);

                if (imageUri != null) {
                    changeImageStorage(CurrentUserData.USER_UID);
                    getUserData(CurrentUserData.USER_UID);
                    setUserData();
                }
                getUserData(CurrentUserData.USER_UID);
                setUserData();
                inflated.setVisibility(View.INVISIBLE);
                constraintLayout.setVisibility(View.VISIBLE);
            }
        });

        editProfileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GalleryIntent();
            }
        });
        setHasOptionsMenu(true);
        return view;
    }

    private void editProfile(String userUID, String newUserName, String newUserBio) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("users").child(userUID).child("userName").setValue(newUserName);
        reference.child("users").child(userUID).child("userBio").setValue(newUserBio);


    }

    private void setUserData() {
            fanArtPostArrayList = CurrentUserData.USER_FAN_ARTS;



        User user = CurrentUserData.USER_DATA;
        if (user != null && !CurrentUserData.USER_UID.equals("")) {
            Picasso.with(getContext()).load(user.getImageUrl()).into(userImage, new Callback() {
                @Override
                public void onSuccess() {
                    userImageShimmerLayout.stopShimmer();
                }

                @Override
                public void onError() {

                }
            });

            userName.setText(user.getUserName());
            userBio.setText(user.getUserBio());
            userEmail.setText(user.getEmail());
        }

        if (!fanArtPostArrayList.isEmpty()) {
            postsAdapter = new PostsAdapter(fanArtPostArrayList, getContext(), postsShimmerLayout);
            recyclerView.setAdapter(postsAdapter);
            postsShimmerLayout.stopShimmer();
        }else{
            postsShimmerLayout.stopShimmer();
            recyclerView.setVisibility(View.INVISIBLE);
            postsShimmerLayout.setVisibility(View.INVISIBLE);
            noResultTextView.setVisibility(View.VISIBLE);
            noResultGifImage.setVisibility(View.VISIBLE);


        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (fanArtPostArrayList != null) {
            getUserData(CurrentUserData.USER_UID);

        }
    }

    private void getUserData(String userUID) {

        databaseReference.child("users").child(userUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CurrentUserData.USER_DATA = snapshot.getValue(User.class);
                User user = CurrentUserData.USER_DATA;
                if (user != null){
                    userName.setText(user.getUserName());
                    userBio.setText(user.getUserBio());
                    userEmail.setText(user.getEmail());
                    Picasso.with(getContext()).load(user.getImageUrl()).into(userImage);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void GalleryIntent() {
        Intent photoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoIntent.setType("image/*");
        startActivityForResult(photoIntent, GET_IMAGE_STORAGE_CODE);

    }

    public void changeImageStorage(String userID) {
        getUserData(CurrentUserData.USER_UID);
        setUserData();
        if (imageUri != null) {
            StorageReference referenceDelete = FirebaseStorage.getInstance().getReference().child("userImages").child(userID);
            referenceDelete.delete();

            StorageReference reference = FirebaseStorage.getInstance().getReference().child("userImages").child(userID);
            reference.putFile(imageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    } else {
                        return reference.getDownloadUrl();
                    }
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        String url = task.getResult().toString();
                        Map<String, Object> map = new HashMap<>();
                        map.put("imageUrl", url);
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(userID);
                        databaseReference.updateChildren(map);

                    } else {
                        System.out.println(task.getException().toString());
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "null", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GET_IMAGE_STORAGE_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            editProfileImageView.setImageURI(imageUri);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.delete_account){
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle("Alert");
            dialog.setMessage("Are you sure you want to delete your account permanently?");
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    AuthCredential credential = EmailAuthProvider.getCredential("user@example.com" , "password1234");

                    user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    CurrentUserData.USER_UID = "";
                                    CurrentUserData.USER_DATA = null;
                                    CurrentUserData.USER_FAN_ARTS = null;
                                }
                            });

                            FirebaseDatabase.getInstance().getReference().child("users").child(CurrentUserData.USER_UID).removeValue();
                            FirebaseStorage.getInstance().getReference().child("users").child(CurrentUserData.USER_UID).delete();
                            FirebaseStorage.getInstance().getReference().child("userImages").child(CurrentUserData.USER_UID).delete();
                            getActivity().finish();
                            startActivity(getActivity().getIntent());
                        }
                    });
                }
            });
            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }
}