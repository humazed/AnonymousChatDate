package humazed.github.com.anonymouschatanddate.chat.ui.friendslist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyProgressDialog
import de.hdodenhof.circleimageview.CircleImageView
import humazed.github.com.anonymouschatanddate.R
import humazed.github.com.anonymouschatanddate.chat.data.StaticConfig
import humazed.github.com.anonymouschatanddate.chat.model.ListFriend
import humazed.github.com.anonymouschatanddate.chat.ui.ChatActivity
import humazed.github.com.anonymouschatanddate.chat.util.ChildEventListenerDelegate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ListFriendsAdapter(private val context: Context, private val listFriend: ListFriend, private val fragment: FriendsFragment)
    : RecyclerView.Adapter<ListFriendsAdapter.ItemFriendViewHolder>() {
    private val dialogWaitDeleting: LovelyProgressDialog = LovelyProgressDialog(context)

    private val mapQuery: MutableMap<String, Query> = HashMap()
    private val mapQueryOnline: MutableMap<String, DatabaseReference> = HashMap()
    private val mapChildListener: MutableMap<String, ChildEventListener> = HashMap()
    private val mapChildListenerOnline: MutableMap<String, ChildEventListener> = HashMap()
    val mapMark: MutableMap<String, Boolean?> = HashMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFriendViewHolder {
        return ItemFriendViewHolder(LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false))
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: ItemFriendViewHolder, position: Int) {
        val friend = listFriend.listFriend[position]
        val name = friend.name
        val id = friend.id
        val idRoom = friend.idRoom
        val avata = friend.avata
        holder.txtName.text = name

        holder.root.setOnClickListener {
            holder.txtMessage.typeface = Typeface.DEFAULT
            holder.txtName.typeface = Typeface.DEFAULT
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_FRIEND, name)
            val idFriend = ArrayList<CharSequence>()
            idFriend.add(id)
            intent.putCharSequenceArrayListExtra(StaticConfig.INTENT_KEY_CHAT_ID, idFriend)
            intent.putExtra(StaticConfig.INTENT_KEY_CHAT_ROOM_ID, idRoom)
            ChatActivity.bitmapAvatarFriend = HashMap()
            if (avata != StaticConfig.STR_DEFAULT_BASE64) {
                val decodedString = Base64.decode(avata, Base64.DEFAULT)
                ChatActivity.bitmapAvatarFriend.put(id, BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size))
            } else {
                ChatActivity.bitmapAvatarFriend.put(id, BitmapFactory.decodeResource(context.resources, R.drawable.default_avata))
            }

            mapMark.put(id, null)
            fragment.startActivityForResult(intent, FriendsFragment.ACTION_START_CHAT)
        }

        // hold to delete.
        holder.root.setOnLongClickListener {
            val friendName = holder.txtName.text as String

            AlertDialog.Builder(context)
                    .setTitle("Delete Friend")
                    .setMessage("Are you sure want to delete $friendName?")
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                        dialogInterface.dismiss()
                        val idFriendRemoval = friend.id
                        dialogWaitDeleting.setTitle("Deleting...")
                                .setCancelable(false)
                                .setTopColorRes(R.color.colorAccent)
                                .show()
                        deleteFriend(idFriendRemoval)
                    }
                    .setNegativeButton(android.R.string.cancel) { dialogInterface, i -> dialogInterface.dismiss() }.show()

            true
        }

        if (friend.message.text.isNotEmpty()) {
            holder.txtMessage.visibility = View.VISIBLE
            holder.txtTime.visibility = View.VISIBLE
            if (!friend.message.text.startsWith(id)) {
                holder.txtMessage.text = friend.message.text
                holder.txtMessage.typeface = Typeface.DEFAULT
                holder.txtName.typeface = Typeface.DEFAULT
            } else {
                holder.txtMessage.text = friend.message.text.substring((id + "").length)
                holder.txtMessage.typeface = Typeface.DEFAULT_BOLD
                holder.txtName.typeface = Typeface.DEFAULT_BOLD
            }
            val time = SimpleDateFormat("EEE, d MMM yyyy").format(Date(friend.message.timestamp))
            val today = SimpleDateFormat("EEE, d MMM yyyy").format(Date(System.currentTimeMillis()))
            if (today == time) {
                holder.txtTime.text = SimpleDateFormat("HH:mm").format(Date(friend.message.timestamp))
            } else {
                holder.txtTime.text = SimpleDateFormat("MMM d").format(Date(friend.message.timestamp))
            }
        } else {
            holder.txtMessage.visibility = View.GONE
            holder.txtTime.visibility = View.GONE
            if (mapQuery[id] == null && mapChildListener[id] == null) {
                mapQuery.put(id, FirebaseDatabase.getInstance().reference.child("message/" + idRoom).limitToLast(1))
                mapChildListener.put(id, object : ChildEventListener by ChildEventListenerDelegate {
                    override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {
                        val mapMessage = dataSnapshot?.value as HashMap<*, *>?
                        val isMapMark = mapMark[id]
                        if (position != NO_POSITION) {
                            if (isMapMark != null) {
                                if (!isMapMark) listFriend.listFriend[position].message.text = id + mapMessage!!["text"]
                                else listFriend.listFriend[position].message.text = mapMessage!!["text"] as String

                                notifyDataSetChanged()
                                mapMark.put(id, false)
                            } else {
                                listFriend.listFriend[position].message.text = mapMessage!!["text"] as String
                                notifyDataSetChanged()
                            }
                            listFriend.listFriend[position].message.timestamp = mapMessage["timestamp"] as Long
                        }
                    }
                })

                mapQuery[id]?.addChildEventListener(mapChildListener[id])
                mapMark.put(id, true)
            } else {
                mapQuery[id]?.removeEventListener(mapChildListener[id])
                mapQuery[id]?.addChildEventListener(mapChildListener[id])
                mapMark.put(id, true)
            }
        }

        if (friend.avata == StaticConfig.STR_DEFAULT_BASE64) {
            holder.avata.setImageResource(R.drawable.default_avata)
        } else {
            val decodedString = Base64.decode(friend.avata, Base64.DEFAULT)
            val src = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            holder.avata.setImageBitmap(src)
        }

        if (mapQueryOnline[id] == null && mapChildListenerOnline[id] == null) {
            mapQueryOnline.put(id, FirebaseDatabase.getInstance().reference.child("user/$id/status"))
            mapChildListenerOnline.put(id, object : ChildEventListener by ChildEventListenerDelegate {
                override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {
                    if (dataSnapshot?.value != null && dataSnapshot.key == "online") {
                        if (position != NO_POSITION) {
                            Log.d("FriendsFragment add " + id, (dataSnapshot.value as Boolean).toString())
                            listFriend.listFriend[position].status.online = dataSnapshot.value as Boolean
                            notifyDataSetChanged()
                        }
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot?, s: String?) {
                    if (dataSnapshot?.value != null && dataSnapshot.key == "online") {
                        if (position != NO_POSITION) {
                            Log.d("FriendsFragment change " + id, (dataSnapshot.value as Boolean).toString())
                            listFriend.listFriend[position].status.online = dataSnapshot.value as Boolean
                            notifyDataSetChanged()
                        }
                    }
                }
            })

            mapQueryOnline[id]?.addChildEventListener(mapChildListenerOnline[id])
        }

        if (friend.status.online) holder.avata.borderWidth = 10
        else holder.avata.borderWidth = 0
    }

    override fun getItemCount(): Int = listFriend.listFriend.size

    /**
     * Delete friend
     *
     * @param idFriend
     */
    private fun deleteFriend(idFriend: String?) {
        if (idFriend != null) {
            FirebaseDatabase.getInstance().reference.child("friend").child(StaticConfig.UID)
                    .orderByValue().equalTo(idFriend).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot?) {
                    if (dataSnapshot?.value == null) {
                        //email not found
                        dialogWaitDeleting.dismiss()
                        LovelyInfoDialog(context)
                                .setTopColorRes(R.color.colorAccent)
                                .setTitle("Error")
                                .setMessage("Error occurred during deleting friend")
                                .show()
                    } else {
                        val idRemoval = (dataSnapshot.value as HashMap<*, *>).keys.iterator().next().toString()
                        FirebaseDatabase.getInstance().reference.child("friend")
                                .child(StaticConfig.UID).child(idRemoval).removeValue()
                                .addOnCompleteListener { task ->
                                    dialogWaitDeleting.dismiss()

                                    LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorAccent)
                                            .setTitle("Success")
                                            .setMessage("Friend deleting successfully")
                                            .show()

                                    val intentDeleted = Intent(FriendsFragment.ACTION_DELETE_FRIEND)
                                    intentDeleted.putExtra("idFriend", idFriend)
                                    context.sendBroadcast(intentDeleted)
                                }
                                .addOnFailureListener { e ->
                                    dialogWaitDeleting.dismiss()
                                    LovelyInfoDialog(context)
                                            .setTopColorRes(R.color.colorAccent)
                                            .setTitle("Error")
                                            .setMessage("Error occurred during deleting friend")
                                            .show()
                                }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError?) {}
            })
        } else {
            dialogWaitDeleting.dismiss()
            LovelyInfoDialog(context)
                    .setTopColorRes(R.color.colorPrimary)
                    .setTitle("Error")
                    .setMessage("Error occurred during deleting friend")
                    .show()
        }
    }

    inner class ItemFriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var root: CardView = itemView.findViewById(R.id.root)
        var avata: CircleImageView = itemView.findViewById(R.id.icon_avata)
        var txtName: TextView = itemView.findViewById(R.id.txtName)
        var txtTime: TextView = itemView.findViewById(R.id.txtTime)
        var txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
    }
}
