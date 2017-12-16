package humazed.github.com.anonymouschatanddate.chat.ui.friendslist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.R.color
import humazed.github.com.anonymouschatanddate.R.drawable
import humazed.github.com.anonymouschatanddate.chat.data.FriendDB
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.model.Friend
import humazed.github.com.anonymouschatanddate.chat.model.ListFriend
import humazed.github.com.anonymouschatanddate.chat.service.ServiceUtils
import kotlinx.android.synthetic.main.fragment_people.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.warn
import java.util.*

class FriendsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    companion object {
        val ACTION_START_CHAT = 1
        val ACTION_DELETE_FRIEND = "com.android.rivchat.DELETE_FRIEND"
    }

    private var adapter: ListFriendsAdapter? = null
    private var dataListFriend: ListFriend? = null
    private var listFriendID: ArrayList<String>? = null
    private var dialogFindAllFriend: LovelyProgressDialog? = null
    private var detectFriendOnline: CountDownTimer? = null

    private var deleteFriendReceiver: BroadcastReceiver? = null

    val ref: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_people, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detectFriendOnline = object : CountDownTimer(System.currentTimeMillis(), StaticConfig.TIME_TO_REFRESH) {
            override fun onTick(l: Long) {
                dataListFriend?.let { ServiceUtils.updateFriendStatus(context!!, it) }
                ServiceUtils.updateUserStatus(context!!)
            }

            override fun onFinish() {}
        }

        if (dataListFriend == null) {
            dataListFriend = FriendDB.getInstance(context).listFriend
            if (dataListFriend!!.listFriend.size > 0) {
                listFriendID = ArrayList()
                dataListFriend?.listFriend?.forEach { friend -> listFriendID?.add(friend.id) }
                detectFriendOnline!!.start()
            }
        }

        swipeRefreshLayout.setOnRefreshListener(this)

        adapter = ListFriendsAdapter(context!!, dataListFriend!!, this)
        recycleListFriend.adapter = adapter

        dialogFindAllFriend = LovelyProgressDialog(context)
        if (listFriendID == null) {
            listFriendID = ArrayList()
            dialogFindAllFriend!!.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle("Get all friend....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()
            getListFriendUId()
        }

        deleteFriendReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val idDeleted = intent.extras!!.getString("idFriend")
                for (friend in dataListFriend!!.listFriend) {
                    if (idDeleted == friend.id) {
                        val friends = dataListFriend!!.listFriend
                        friends.remove(friend)
                        break
                    }
                }
                adapter!!.notifyDataSetChanged()
            }
        }

        context!!.registerReceiver(deleteFriendReceiver, IntentFilter(ACTION_DELETE_FRIEND))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detectFriendOnline?.cancel()
        context!!.unregisterReceiver(deleteFriendReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (ACTION_START_CHAT == requestCode && data != null) {
            adapter!!.mapMark.put(data.getStringExtra("idFriend"), false)
        }
    }

    override fun onRefresh() {
        listFriendID!!.clear()
        dataListFriend!!.listFriend.clear()
        adapter!!.notifyDataSetChanged()
        FriendDB.getInstance(context).dropDB()
        detectFriendOnline!!.cancel()
        getListFriendUId()
    }

    /**
     * Get a list of friends on the server
     */
    private fun getListFriendUId() {
        ref.child("friend/" + StaticConfig.UID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    val mapRecord = dataSnapshot.value as HashMap<*, *>?
                    mapRecord!!.keys.forEach { listFriendID!!.add(mapRecord[it].toString()) }
                    getAllFriendInfo(0)
                } else {
                    dialogFindAllFriend!!.dismiss()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                dialogFindAllFriend!!.dismiss()
                swipeRefreshLayout.isRefreshing = false
            }
        })

/*
        ref.child("friend/" + StaticConfig.UID).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    val mapRecord = dataSnapshot.value as HashMap<*, *>?
                    mapRecord!!.keys.forEach { listFriendID!!.add(mapRecord[it].toString()) }
                    getAllFriendInfo(0)
                } else {
                    dialogFindAllFriend!!.dismiss()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                dialogFindAllFriend!!.dismiss()
                swipeRefreshLayout.isRefreshing = false
            }
        })
*/
    }

    /**
     * Accessible by the user to retrieve information user ID
     */
    private fun getAllFriendInfo(index: Int) {
        if (index == listFriendID!!.size) {
            //save list friend
            adapter!!.notifyDataSetChanged()
            dialogFindAllFriend!!.dismiss()
            swipeRefreshLayout.isRefreshing = false
            detectFriendOnline!!.start()
        } else {
            val id = listFriendID!![index]
            ref.child("user/" + id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {
                        val user = Friend()
                        val mapUserInfo = dataSnapshot.value as HashMap<*, *>?
                        user.name = mapUserInfo!!["name"] as String
                        user.email = mapUserInfo["email"] as String
                        user.avata = mapUserInfo["avata"] as String
                        user.id = id
                        user.idRoom = if (id > StaticConfig.UID) (StaticConfig.UID + id).hashCode().toString()
                        else (id + StaticConfig.UID).hashCode().toString()

                        dataListFriend!!.listFriend.add(user)
                        FriendDB.getInstance(context).addFriend(user)
                    }
                    getAllFriendInfo(index + 1)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    inner class FragFriendClickFloatButton : View.OnClickListener {
        internal lateinit var context: Context
        internal lateinit var dialogWait: LovelyProgressDialog

        fun getInstance(context: Context): FragFriendClickFloatButton {
            this.context = context
            dialogWait = LovelyProgressDialog(context)
            return this
        }

        var usersHashMap: HashMap<String, HashMap<String, *>>? = null
        override fun onClick(view: View) {
            ref.child("user").orderByChild("email").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    usersHashMap = dataSnapshot.value as HashMap<String, HashMap<String, *>>?
                    usersHashMap?.let { getRandomEmail(it) }?.let { findIDEmail(it) }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

        private fun getRandomEmail(usersHashMap: HashMap<String, HashMap<String, *>>): String {
            fun isOnlineAndNotTheCurrentUser(user: HashMap<String, *>?): Boolean {
                val email = user?.get("email") as String
                val status = user["status"] as HashMap<String, *>?
                val isOnline = status?.get("online") as Boolean
                return FirebaseAuth.getInstance().currentUser?.email != email && isOnline
            }

            fun returnEmail(user: HashMap<String, *>?): String {
                val email = user?.get("email") as String
                warn { "email = $email" }
                return email
            }

            usersHashMap.values.filter { isOnlineAndNotTheCurrentUser(it) }.forEach { returnEmail(it) }

            val rand = Random().nextInt(usersHashMap.size)
            val user: HashMap<String, *>? = usersHashMap[usersHashMap.keys.toTypedArray()[rand]]

            return returnEmail(user)
        }


        /**
         * TIm id cua email tren server
         *
         * @param email
         */
        private fun findIDEmail(email: String) {
            dialogWait.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle("Finding friend....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()

            ref.child("user").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dialogWait.dismiss()
                    if (dataSnapshot.value != null) {
                        val id = (dataSnapshot.value as HashMap<*, *>).keys.iterator().next().toString()
                        if (id == StaticConfig.UID) {
                            usersHashMap?.let { getRandomEmail(it) }?.let { findIDEmail(it) }
                        } else {
                            val userMap = (dataSnapshot.value as HashMap<*, *>)[id] as HashMap<*, *>
                            val user = Friend()
                            user.name = userMap["name"] as String
                            user.email = userMap["email"] as String
                            user.avata = userMap["avata"] as String
                            user.id = id
                            user.idRoom = if (id > StaticConfig.UID) (StaticConfig.UID + id).hashCode().toString()
                            else (id + StaticConfig.UID).hashCode().toString()

                            checkBeforeAddFriend(id, user)
                        }
                    } else {
                        //email not found
                        LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setIcon(R.drawable.ic_add_friend)
                                .setTitle("Fail")
                                .setMessage("Email not found")
                                .show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

        /**
         * show the friend list of a UID
         */
        private fun checkBeforeAddFriend(idFriend: String, userInfo: Friend) {
            dialogWait.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle("Add friend....")
                    .setTopColorRes(R.color.colorPrimary)
                    .show()

            // Check if id exists in id list
            if (listFriendID != null && listFriendID!!.contains(idFriend)) {
                dialogWait.dismiss()
                LovelyInfoDialog(context)
                        .setTopColorRes(R.color.colorPrimary)
                        .setIcon(R.drawable.ic_add_friend)
                        .setTitle("Friend")
//                        .setMessage("User " + userInfo.email + " has been friend please try again")
                        .setMessage("Didn't find any one please try again")
                        .show()
            } else {
                addFriend(idFriend)
                listFriendID?.add(idFriend)
                dataListFriend?.listFriend?.add(userInfo) // FIXME: 12/16/2017 some NPE
                FriendDB.getInstance(getContext()).addFriend(userInfo)
                adapter?.notifyDataSetChanged()
            }
        }

        /**
         * Add friend
         *
         * @param idFriend
         */
        private fun addFriend(idFriend: String) {
            ref.child("friend/${StaticConfig.UID}").push().setValue(idFriend).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    ref.child("friend/$idFriend").push().setValue(StaticConfig.UID).addOnCompleteListener { task1 ->
                        if (task1.isSuccessful) {
                            dialogWait.dismiss()
                            showSuccessDialog()
                        }
                    }.addOnFailureListener {
                        dialogWait.dismiss()
                        showErrorDialog()
                    }
                }
            }.addOnFailureListener {
                dialogWait.dismiss()
                showErrorDialog()
            }
        }

        private fun showSuccessDialog() = LovelyInfoDialog(context)
                .setTopColorRes(color.colorPrimary)
                .setIcon(drawable.ic_add_friend)
                .setTitle("Success")
                .setMessage("Add friend success")
                .show()


        private fun showErrorDialog() = LovelyInfoDialog(context)
                .setTopColorRes(color.colorAccent)
                .setIcon(drawable.ic_add_friend)
                .setTitle("Failed")
                .setMessage("Failed to add friend")
                .show()
    }
}