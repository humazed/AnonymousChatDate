package humazed.github.com.anonymouschatanddate.chat.model

import java.util.*


class ListFriend(var listFriend: ArrayList<Friend> = ArrayList()) {
    fun getAvataById(id: String): String {
        return listFriend
                .firstOrNull { id == it.id }
                ?.avata ?: ""
    }
}
