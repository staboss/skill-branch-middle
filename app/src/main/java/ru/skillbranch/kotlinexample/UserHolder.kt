package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.User.Factory.fullNameToPair

@SuppressLint("DefaultLocale")
object UserHolder {

    private val map = mutableMapOf<String, User>()

    fun registerUser(fullName: String, email: String, password: String): User =
        if (map.contains(parseLogin(email))) {
            throw IllegalArgumentException("A user with this email already exists")
        } else {
            User.makeUser(fullName, email = email, password = password).also { user ->
                map[user.login] = user
            }
        }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        val phone = rawPhone.replace("""[^+\d]""".toRegex(), "").also { phone ->
            if (phone.length != 12) {
                throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
            }
        }

        if (map.any { it.value.login == phone }) {
            throw IllegalArgumentException("A user with this phone already exists")
        }

        return User.makeUser(fullName, phone = rawPhone).also { user ->
            map[user.login] = user
        }
    }

    fun loginUser(login: String, password: String): String? = map[parseLogin(login)]?.let { user ->
        if (user.checkPassword(password)) user.userInfo
        else null
    }

    fun requestAccessCode(login: String) {
        map[parseLogin(login)]?.let { user ->
            with(user) {
                val code = generateAccessCode()
                accessCode?.let { changePassword(it, code) }
            }
        }
    }

    fun importUsers(list: List<String>): List<User> = mutableListOf<User>().apply {
        list.forEach { line ->
            val params = line.trim().split(";")

            val (firstName, lastName) = params[0].fullNameToPair()
            val email = params[1].parseNullIfBlank()
            val (salt, password) = params[2].split(":").run { first().trim() to last().trim() }
            val phone = params[3].parseNullIfBlank()

            User(firstName, lastName, email, phone, password, salt).also { user ->
                if (!map.contains(user.login)) {
                    map[user.login] = user
                    add(user)
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    private fun parseLogin(login: String): String = when {
        !login.contains('@') -> login.replace("""[^+\d]""".toRegex(), "")
        else -> login.toLowerCase()
    }.trim()

    private fun String.parseNullIfBlank(): String? = if (isEmpty() || isBlank()) null else this
}