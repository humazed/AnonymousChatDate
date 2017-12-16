package humazed.github.com.anonymouschatanddate.chat.model

import java.util.*


open class Room(
        var member: ArrayList<String> = ArrayList(),
        var groupInfo: Map<String, String> = HashMap()
)
