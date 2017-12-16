package humazed.github.com.anonymouschatanddate.chat.model


class Message(
        var idSender: String = "",
        var idReceiver: String = "",
        var text: String = "",
        var timestamp: Long = 0
)