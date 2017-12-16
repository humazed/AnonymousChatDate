package humazed.github.com.anonymouschatanddate.chat.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import humazed.github.com.anonymouschatanddate.R;
import humazed.github.com.anonymouschatanddate.chat.data.FriendDB;
import humazed.github.com.anonymouschatanddate.chat.data.GroupDB;
import humazed.github.com.anonymouschatanddate.chat.data.SharedPreferenceHelper;
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig;
import humazed.github.com.anonymouschatanddate.chat.model.Configuration;
import humazed.github.com.anonymouschatanddate.chat.model.User;
import humazed.github.com.anonymouschatanddate.chat.service.ServiceUtils;
import humazed.github.com.anonymouschatanddate.chat.util.ImageUtils;


public class UserProfileFragment extends Fragment {
    TextView tvUserName;
    ImageView avatar;

    private List<Configuration> listConfig = new ArrayList<>();
    private RecyclerView recyclerView;
    private UserInfoAdapter infoAdapter;

    private static final String USERNAME_LABEL = "Username";
    private static final String EMAIL_LABEL = "Email";
    private static final String SIGNOUT_LABEL = "Sign out";
    private static final String RESETPASS_LABEL = "Change Password";

    private static final int PICK_IMAGE = 1994;
    private LovelyProgressDialog waitingDialog;

    private DatabaseReference userDB;
    private FirebaseAuth mAuth;
    private User myAccount;
    private Context context;

    public UserProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            //Retrieve user information and updating interface
            listConfig.clear();
            myAccount = dataSnapshot.getValue(User.class);

            if (myAccount != null) {
                setupArrayListInfo(myAccount);

                if (infoAdapter != null) infoAdapter.notifyDataSetChanged();
                if (tvUserName != null) tvUserName.setText(myAccount.getName());

                setImageAvatar(context, myAccount.getAvata());
                SharedPreferenceHelper preferenceHelper = SharedPreferenceHelper.getInstance(context);
                preferenceHelper.saveUserInfo(myAccount);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            //Có lỗi xảy ra, không lấy đc dữ liệu
            Log.e(UserProfileFragment.class.getName(), "loadPost:onCancelled", databaseError.toException());
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        userDB = FirebaseDatabase.getInstance().getReference().child("user").child(StaticConfig.getUID());
        userDB.addListenerForSingleValueEvent(userListener);
        mAuth = FirebaseAuth.getInstance();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        context = view.getContext();
        avatar = view.findViewById(R.id.img_avatar);
        avatar.setOnClickListener(onAvatarClick);
        tvUserName = view.findViewById(R.id.tv_username);

        SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(context);
        myAccount = prefHelper.getUserInfo();
        setupArrayListInfo(myAccount);
        setImageAvatar(context, myAccount.getAvata());
        tvUserName.setText(myAccount.getName());

        recyclerView = view.findViewById(R.id.info_recycler_view);
        infoAdapter = new UserInfoAdapter(listConfig);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(infoAdapter);

        waitingDialog = new LovelyProgressDialog(context);
        return view;
    }

    /**
     * Khi click vào avatar thì bắn intent mở trình xem ảnh mặc định để chọn ảnh
     */
    private View.OnClickListener onAvatarClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            new AlertDialog.Builder(context)
                    .setTitle("Avatar")
                    .setMessage("Are you sure want to change avatar profile?")
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss()).show();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(context, "An error occurred, please try again", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(data.getData());

                Bitmap imgBitmap = BitmapFactory.decodeStream(inputStream);
                imgBitmap = ImageUtils.cropToSquare(imgBitmap);
                InputStream is = ImageUtils.convertBitmapToInputStream(imgBitmap);
                final Bitmap liteImage = ImageUtils.makeImageLite(is,
                        imgBitmap.getWidth(), imgBitmap.getHeight(),
                        ImageUtils.AVATAR_WIDTH, ImageUtils.AVATAR_HEIGHT);

                String imageBase64 = ImageUtils.encodeBase64(liteImage);
                myAccount.setAvata(imageBase64);

                waitingDialog.setCancelable(false)
                        .setTitle("Avatar updating....")
                        .setTopColorRes(R.color.colorPrimary)
                        .show();

                userDB.child("avata").setValue(imageBase64)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                waitingDialog.dismiss();
                                SharedPreferenceHelper preferenceHelper = SharedPreferenceHelper.getInstance(context);
                                preferenceHelper.saveUserInfo(myAccount);
                                avatar.setImageDrawable(ImageUtils.roundedImage(context, liteImage));

                                new LovelyInfoDialog(context)
                                        .setTopColorRes(R.color.colorPrimary)
                                        .setTitle("Success")
                                        .setMessage("Update avatar successfully!")
                                        .show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            waitingDialog.dismiss();
                            Log.d("Update Avatar", "failed");
                            new LovelyInfoDialog(context)
                                    .setTopColorRes(R.color.colorAccent)
                                    .setTitle("False")
                                    .setMessage("False to update avatar")
                                    .show();
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Xóa list cũ và cập nhật lại list data mới
     *
     * @param myAccount
     */
    public void setupArrayListInfo(User myAccount) {
        listConfig.clear();
        Configuration userNameConfig = new Configuration(USERNAME_LABEL, myAccount.getName(), R.mipmap.ic_account_box);
        listConfig.add(userNameConfig);

        Configuration emailConfig = new Configuration(EMAIL_LABEL, myAccount.getEmail(), R.mipmap.ic_email);
        listConfig.add(emailConfig);

        Configuration resetPass = new Configuration(RESETPASS_LABEL, "", R.mipmap.ic_restore);
        listConfig.add(resetPass);

        Configuration signout = new Configuration(SIGNOUT_LABEL, "", R.mipmap.ic_power_settings);
        listConfig.add(signout);
    }

    private void setImageAvatar(Context context, String imgBase64) {
        try {
            Resources res = getResources();
            //Nếu chưa có avatar thì để hình mặc định
            Bitmap src;
            if (imgBase64.equals("default")) {
                src = BitmapFactory.decodeResource(res, R.drawable.default_avata);
            } else {
                byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            }

            avatar.setImageDrawable(ImageUtils.roundedImage(context, src));
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
        private List<Configuration> profileConfig;

        public UserInfoAdapter(List<Configuration> profileConfig) {
            this.profileConfig = profileConfig;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_info_item_layout, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Configuration config = profileConfig.get(position);
            holder.label.setText(config.getLabel());
            holder.value.setText(config.getValue());
            holder.icon.setImageResource(config.getIcon());
            ((RelativeLayout) holder.label.getParent()).setOnClickListener(view -> {
                if (config.getLabel().equals(SIGNOUT_LABEL)) {
                    FirebaseAuth.getInstance().signOut();
                    FriendDB.getInstance(getContext()).dropDB();
                    GroupDB.getInstance(getContext()).dropDB();
                    ServiceUtils.INSTANCE.stopServiceFriendChat(getContext().getApplicationContext(), true);
                    getActivity().finish();
                }

                if (config.getLabel().equals(USERNAME_LABEL)) {
                    View vewInflater = LayoutInflater.from(context)
                            .inflate(R.layout.dialog_edit_username, (ViewGroup) getView(), false);
                    final EditText input = vewInflater.findViewById(R.id.edit_username);
                    input.setText(myAccount.getName());
                    /*Hiển thị dialog với dEitText cho phép người dùng nhập username mới*/
                    new AlertDialog.Builder(context)
                            .setTitle("Edit username")
                            .setView(vewInflater)
                            .setPositiveButton("Save", (dialogInterface, i) -> {
                                String newName = input.getText().toString();
                                if (!myAccount.getName().equals(newName)) {
                                    changeUserName(newName);
                                }
                                dialogInterface.dismiss();
                            })
                            .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss()).show();
                }

                if (config.getLabel().equals(RESETPASS_LABEL)) {
                    new AlertDialog.Builder(context)
                            .setTitle("Password")
                            .setMessage("Are you sure want to reset password?")
                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                resetPassword(myAccount.getEmail());
                                dialogInterface.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss()).show();
                }
            });
        }

        /**
         * Cập nhật username mới vào SharedPreference và thay đổi trên giao diện
         */
        private void changeUserName(String newName) {
            userDB.child("name").setValue(newName);


            myAccount.setName(newName);
            SharedPreferenceHelper prefHelper = SharedPreferenceHelper.getInstance(context);
            prefHelper.saveUserInfo(myAccount);

            tvUserName.setText(newName);
            setupArrayListInfo(myAccount);
        }

        void resetPassword(final String email) {
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            new LovelyInfoDialog(context) {
                                @Override
                                public LovelyInfoDialog setConfirmButtonText(String text) {
                                    findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener(view -> dismiss());
                                    return super.setConfirmButtonText(text);
                                }
                            }
                                    .setTopColorRes(R.color.colorPrimary)
                                    .setIcon(R.drawable.ic_pass_reset)
                                    .setTitle("Password Recovery")
                                    .setMessage("Sent email to " + email)
                                    .setConfirmButtonText("Ok")
                                    .show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            new LovelyInfoDialog(context) {
                                @Override
                                public LovelyInfoDialog setConfirmButtonText(String text) {
                                    findView(com.yarolegovich.lovelydialog.R.id.ld_btn_confirm).setOnClickListener(view -> dismiss());
                                    return super.setConfirmButtonText(text);
                                }
                            }
                                    .setTopColorRes(R.color.colorAccent)
                                    .setIcon(R.drawable.ic_pass_reset)
                                    .setTitle("False")
                                    .setMessage("False to sent email to " + email)
                                    .setConfirmButtonText("Ok")
                                    .show();
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return profileConfig.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView label, value;
            public ImageView icon;

            public ViewHolder(View view) {
                super(view);
                label = view.findViewById(R.id.tv_title);
                value = view.findViewById(R.id.tv_detail);
                icon = view.findViewById(R.id.img_icon);
            }
        }
    }
}
