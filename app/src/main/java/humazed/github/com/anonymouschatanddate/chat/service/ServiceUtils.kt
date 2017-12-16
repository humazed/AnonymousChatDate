package humazed.github.com.anonymouschatanddate.chat.service

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.IBinder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import humazed.github.com.anonymouschatanddate.chat.data.SharedPreferenceHelper
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.model.ListFriend
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.warn
import java.util.*


object ServiceUtils : AnkoLogger {

    private var connectionServiceFriendChatForStart: ServiceConnection? = null
    private var connectionServiceFriendChatForDestroy: ServiceConnection? = null

    val ref: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun stopServiceFriendChat(context: Context, kill: Boolean) {
        warn { "stopServiceFriendChat() called with: context = [$context], kill = [$kill]" }
        if (isServiceFriendChatRunning(context)) {
            val intent = Intent(context, FriendChatService::class.java)
            if (connectionServiceFriendChatForDestroy != null) {
                context.unbindService(connectionServiceFriendChatForDestroy!!)
            }
            connectionServiceFriendChatForDestroy = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as FriendChatService.LocalBinder
                    binder.service.stopSelf()
                }

                override fun onServiceDisconnected(arg0: ComponentName) {}
            }
            context.bindService(intent, connectionServiceFriendChatForDestroy!!, Context.BIND_NOT_FOREGROUND)
        }
    }

    fun startServiceFriendChat(context: Context) {
        warn { "startServiceFriendChat() called with: context = [$context]" }
        if (!isServiceFriendChatRunning(context)) {
            val myIntent = Intent(context, FriendChatService::class.java)
            context.startService(myIntent)
        } else {
            if (connectionServiceFriendChatForStart != null) {
                context.unbindService(connectionServiceFriendChatForStart!!)
            }
            connectionServiceFriendChatForStart = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as FriendChatService.LocalBinder
                    for (friend in binder.service.listFriend) {
                        binder.service.mapMark.put(friend.idRoom, true)
                    }
                }

                override fun onServiceDisconnected(arg0: ComponentName) {}
            }
            val intent = Intent(context, FriendChatService::class.java)
            context.bindService(intent, connectionServiceFriendChatForStart!!, Context.BIND_NOT_FOREGROUND)
        }
    }

    fun updateUserStatus(context: Context) {
        warn { "updateUserStatus() called with: context = [$context]" }
        if (isNetworkConnected(context)) {
            val uid = SharedPreferenceHelper.getInstance(context).uid
            if (uid != "") {
                ref.child("user/$uid/status/online").setValue(true)
                ref.child("user/$uid/status/timestamp").setValue(System.currentTimeMillis())
            }
        }
    }

    fun updateFriendStatus(context: Context, listFriend: ListFriend) {
        warn { "updateFriendStatus() called with: context = [$context], listFriend = [$listFriend]" }
        if (isNetworkConnected(context)) {
            listFriend.listFriend
                    .map { it.id }
                    .forEach {
                        ref.child("user/$it/status").addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                                if (dataSnapshot?.value != null) {
                                    val mapStatus = dataSnapshot.value as HashMap<*, *>?
                                    if (mapStatus?.get("online") as Boolean &&
                                            System.currentTimeMillis() - mapStatus["timestamp"] as Long > StaticConfig.TIME_TO_OFFLINE) {
                                        ref.child("user/$it/status/online").setValue(false)
                                    }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
        }
    }

    private fun isServiceFriendChatRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { FriendChatService::class.java.name == it.service.className }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.activeNetworkInfo != null
        } catch (e: Exception) {
            true
        }
    }
}
