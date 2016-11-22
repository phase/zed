class Post(val name: String, val link: String, val user: User) {

    var points = 0

}

class User(val id: Int, val name: String, val hashedPassword: String) {

    var points = 0

}
