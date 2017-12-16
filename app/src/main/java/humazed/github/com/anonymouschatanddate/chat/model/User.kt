package humazed.github.com.anonymouschatanddate.chat.model


open class User(
        var name: String = "",
        var email: String = "",
        var avata: String = "",
        var status: Status = Status(),
        var message: Message = Message()
)
