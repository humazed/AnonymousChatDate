package humazed.github.com.anonymouschatanddate.chat.data

import com.kizitonwose.time.minutes
import com.kizitonwose.time.seconds


object StaticConfig {
    const val REQUEST_CODE_REGISTER = 2000
    const val STR_EXTRA_ACTION_LOGIN = "login"
    const val STR_EXTRA_ACTION_RESET = "resetpass"
    const val STR_EXTRA_ACTION = "action"
    const val STR_EXTRA_USERNAME = "username"
    const val STR_EXTRA_PASSWORD = "password"
    const val STR_DEFAULT_BASE64 = "default"
    @JvmStatic var UID = ""
    //TODO only use this UID for debug mode
    //    public static String UID = "6kU0SbJPF5QJKZTfvW1BqKolrx22";
    const val INTENT_KEY_CHAT_FRIEND = "friendname"
    const val INTENT_KEY_CHAT_AVATA = "friendavata"
    const val INTENT_KEY_CHAT_ID = "friendid"
    const val INTENT_KEY_CHAT_ROOM_ID = "roomid"
    @JvmStatic val TIME_TO_REFRESH: Long = 5.seconds.inMilliseconds.longValue
    @JvmStatic val TIME_TO_OFFLINE = 5.minutes.inMilliseconds.longValue
}
