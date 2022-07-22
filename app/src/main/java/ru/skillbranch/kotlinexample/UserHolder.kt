package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map.contains(user.login)) throw IllegalArgumentException("A user with this email already exists")
                else map[user.login] = user
            }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if (!rawPhone.isPhoneNumber()) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        return User.makeUser(fullName, phone = rawPhone)
            .also { user ->
                if (map.contains(user.login)) throw IllegalArgumentException("A user with this phone already exists")
                else map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? {
        var cleanLogin = login
        if (login.isPhoneNumber()) cleanLogin = login.cleanPhoneNumber()
        return map[cleanLogin.trim().toLowerCase()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        val user = map[login.trim().cleanPhoneNumber()] ?: return
        val code = user.generateAccessCode()
        user.sendAccessCodeToUser(user.login, code)
        user.changePassword(user.accessCode ?: "", code)
        user.accessCode = code
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun String.isPhoneNumber(): Boolean {
        val regex =
            "\\+? ?\\d? ??\\(?\\d{3}?\\)?? ??\\-?\\d{3}? ??\\-?\\d{2}? ??\\-?\\d{2}".toRegex()
        return regex.matches(this)
    }

    fun String.cleanPhoneNumber(): String {
        return replace("[^\\d\\+]".toRegex(), "")
    }

    fun importUsers(list: List<String>): List<User> {
        var usersList = mutableListOf<User>()

        for (item in list) {
            val line = item.split(';')
            val (salt, passwordHash) = line[2].fullPassToPair()
            val user = User.makeUser(
                line[0],
                if (line[1].isNullOrBlank()) null else line[1],
                salt,
                passwordHash,
                if (line[3].isNullOrBlank()) null else line[3]
            ).also { user ->
                if (map.contains(user.login)) throw IllegalArgumentException("A user with this email already exists")
                else map[user.login] = user
            }
            if (user != null) usersList.add(user)
        }
        return usersList
    }

    private fun String.fullPassToPair(): Pair<String?, String?> {
        return split(":")
            .filter { it.isNotBlank() }
            .run {
                when (size) {
                    2 -> first() to last()
                    else -> throw IllegalArgumentException("Password need contain salt")
                }
            }
    }
}
