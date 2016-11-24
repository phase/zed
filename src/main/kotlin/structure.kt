class Post(val name: String, val link: String, val user: User) {

    var points = 0

}

// Stored values in caches
val userCaches: MutableMap<String, User> = mutableMapOf()

val NULL_USER = User(-1, "null", "null")

class User(val id: Int, val name: String, val hashedPassword: String) {

    var points = 0

    fun cache(): String {
        val generatedString = "zed_" + System.currentTimeMillis()
        userCaches.put(generatedString, this)
        return generatedString
    }

}
