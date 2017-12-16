package humazed.github.com.anonymouschatanddate.chat.data;

import android.content.Context;
import android.content.SharedPreferences;

import humazed.github.com.anonymouschatanddate.chat.model.User;


public class SharedPreferenceHelper {
    private static SharedPreferenceHelper instance = null;
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    private static String SHARE_USER_INFO = "userinfo";
    private static String SHARE_KEY_NAME = "name";
    private static String SHARE_KEY_EMAIL = "email";
    private static String SHARE_KEY_AVATA = "avata";
    private static String SHARE_KEY_UID = "uid";


    private SharedPreferenceHelper() {}

    public static SharedPreferenceHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferenceHelper();
            preferences = context.getSharedPreferences(SHARE_USER_INFO, Context.MODE_PRIVATE);
            editor = preferences.edit();
        }
        return instance;
    }

    public void saveUserInfo(User user) {
        editor.putString(SHARE_KEY_NAME, user.getName());
        editor.putString(SHARE_KEY_EMAIL, user.getEmail());
        editor.putString(SHARE_KEY_AVATA, user.getAvata());
        editor.putString(SHARE_KEY_UID, StaticConfig.getUID());
        editor.apply();
    }

    public User getUserInfo() {
        String userName = preferences.getString(SHARE_KEY_NAME, "");
        String email = preferences.getString(SHARE_KEY_EMAIL, "");
        String avatar = preferences.getString(SHARE_KEY_AVATA, "default");

        User user = new User();
        user.setName(userName);
        user.setEmail(email);
        user.setAvata(avatar);

        return user;
    }

    public String getUID() {
        return preferences.getString(SHARE_KEY_UID, "");
    }

}
