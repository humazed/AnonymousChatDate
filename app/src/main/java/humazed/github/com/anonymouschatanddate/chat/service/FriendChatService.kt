package humazed.github.com.anonymouschatanddate.chat.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Base64
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.MainChatActivity
import humazed.github.com.anonymouschatanddate.chat.data.FriendDB
import humazed.github.com.anonymouschatanddate.chat.data.GroupDB
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.model.Friend
import humazed.github.com.anonymouschatanddate.chat.model.Group
import humazed.github.com.anonymouschatanddate.chat.util.ChildEventListenerDelegate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn
import java.util.*
import kotlin.collections.HashMap

class FriendChatService : Service(), AnkoLogger {
    // Binder given to clients
    private val mBinder: IBinder = LocalBinder()
    lateinit var mapMark: MutableMap<String, Boolean>
    private lateinit var mapQuery: MutableMap<String, Query>
    private lateinit var mapChildEventListenerMap: MutableMap<String, ChildEventListener>
    lateinit var mapBitmap: MutableMap<String, Bitmap>
    private lateinit var listKey: ArrayList<String>
    lateinit var listFriend: ArrayList<Friend>
    private lateinit var listGroup: ArrayList<Group>
    private lateinit var updateOnline: CountDownTimer

    override fun onCreate() {
        super.onCreate()
        warn { "onCreate() called with: " }
        mapMark = HashMap()
        mapQuery = HashMap()
        mapChildEventListenerMap = HashMap()
        listFriend = FriendDB.getInstance(this).listFriend.listFriend
        listGroup = GroupDB.getInstance(this).listGroups
        listKey = ArrayList()
        mapBitmap = HashMap()
        updateOnline = object : CountDownTimer(System.currentTimeMillis(), StaticConfig.TIME_TO_REFRESH) {
            override fun onTick(l: Long) {
                ServiceUtils.updateUserStatus(applicationContext)
            }

            override fun onFinish() {}
        }
        updateOnline.start()

        if (listFriend.size > 0 || listGroup.size > 0) {
            warn { "listFriend.size = ${listFriend.size}" }
            //Subscribe to the rooms here
            for (friend in listFriend) {
                if (!listKey.contains(friend.idRoom)) {
                    mapQuery.put(friend.idRoom, FirebaseDatabase.getInstance().reference.child("message/${friend.idRoom}").limitToLast(1))

                    mapChildEventListenerMap.put(friend.idRoom, object : ChildEventListener by ChildEventListenerDelegate {
                        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                            warn { "onChildAdded() called with: dataSnapshot = [$dataSnapshot], s = [$s]" }
                            if (mapMark[friend.idRoom] != null && mapMark[friend.idRoom]!!) {
                                toast(friend.name + ": " + (dataSnapshot.value as HashMap<*, *>)["text"])
                                if (mapBitmap[friend.idRoom] == null) {
                                    if (friend.avata != StaticConfig.STR_DEFAULT_BASE64) {
                                        val decodedString = Base64.decode(friend.avata, Base64.DEFAULT)
                                        mapBitmap.put(friend.idRoom, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size))
                                    } else {
                                        mapBitmap.put(friend.idRoom, BitmapFactory.decodeResource(resources, R.drawable.default_avata))
                                    }
                                }
                                createNotify(friend.name,
                                        (dataSnapshot.value as HashMap<*, *>)["text"] as String, friend.idRoom.hashCode(),
                                        mapBitmap[friend.idRoom]!!, false)

                            } else {
                                mapMark.put(friend.idRoom, true)
                            }
                        }
                    })
                    listKey.add(friend.idRoom)
                }
                mapQuery[friend.idRoom]?.addChildEventListener(mapChildEventListenerMap[friend.idRoom])
            }

            for (group in listGroup) {
                if (!listKey.contains(group.id)) {
                    mapQuery.put(group.id, FirebaseDatabase.getInstance().reference.child("message/" + group.id)
                            .limitToLast(1))
                    mapChildEventListenerMap.put(group.id, object : ChildEventListener by ChildEventListenerDelegate {
                        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                            if (mapMark[group.id] != null && mapMark[group.id]!!) {
                                if (mapBitmap[group.id] == null) {
                                    mapBitmap.put(group.id, BitmapFactory.decodeResource(resources, R.drawable.ic_notify_group))
                                }
                                createNotify(group.groupInfo["name"]!!,
                                        (dataSnapshot.value as HashMap<*, *>)["text"] as String, group.id.hashCode(),
                                        mapBitmap[group.id]!!, true)
                            } else {
                                mapMark.put(group.id, true)
                            }
                        }
                    })
                    listKey.add(group.id)
                }
                mapQuery[group.id]?.addChildEventListener(mapChildEventListenerMap[group.id])
            }
        } else {
            stopSelf()
        }
    }

    fun createNotify(name: String, content: String, id: Int, icon: Bitmap, isGroup: Boolean) {
        val activityIntent = Intent(this, MainChatActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(this, "messages")
                .setLargeIcon(icon)
                .setContentTitle(name)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(1000, 1000))
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)

        if (isGroup) notificationBuilder.setSmallIcon(R.drawable.ic_tab_group)
        else notificationBuilder.setSmallIcon(R.drawable.ic_tab_person)

        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
        notificationManager.notify(id, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        debug { "OnStartService" }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        debug { "OnBindService" }
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        for (id in listKey) {
            mapQuery[id]?.removeEventListener(mapChildEventListenerMap[id])
        }
        mapQuery.clear()
        mapChildEventListenerMap.clear()
        mapBitmap.clear()
        updateOnline.cancel()
        debug { "OnDestroyService" }
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: FriendChatService
            get() = this@FriendChatService
    }
}
