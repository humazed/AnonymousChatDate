package humazed.github.com.anonymouschatanddate.chat.util

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

/**
 * User: YourPc
 * Date: 12/14/2017
 */
object ChildEventListenerDelegate : ChildEventListener {
    override fun onChildChanged(p0: DataSnapshot?, p1: String?) {}
    override fun onCancelled(p0: DatabaseError?) {}
    override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}
    override fun onChildAdded(p0: DataSnapshot?, p1: String?) {}
    override fun onChildRemoved(p0: DataSnapshot?) {}
}
