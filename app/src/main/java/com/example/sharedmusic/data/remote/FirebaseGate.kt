package com.example.sharedmusic.data.remote

import com.example.sharedmusic.core.SharyConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Доступ к Firebase.
 *
 * Вход выполняется анонимно: для MVP не нужны email и пароль,
 * а позже анонимный аккаунт можно привязать к постоянному.
 * Все методы возвращают результат, а не бросают исключения в UI.
 */
object FirebaseGate {

    /**
     * Результат входа.
     *
     * `reason` — техническая причина для логов, а не текст для пользователя:
     * UI показывает свои строки из strings.xml и не зависит от формулировок
     * Firebase, которые к тому же приходят на английском.
     */
    sealed interface AuthResult {
        data class Ready(val uid: String) : AuthResult
        data class Failed(val reason: String) : AuthResult
    }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    /** Realtime Database конкретного проекта; адрес задаётся в SharyConfig */
    val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(SharyConfig.DATABASE_URL)

    val currentUid: String?
        get() = auth.currentUser?.uid

    /** true, если google-services.json подключён и Firebase инициализирован */
    val isAvailable: Boolean
        get() {
            val context = appContextOrNull() ?: return false
            return FirebaseApp.getApps(context).isNotEmpty()
        }

    /** Контекст приложения; при раннем вызове может быть ещё не задан */
    private fun appContextOrNull(): android.content.Context? =
        runCatching { com.example.sharedmusic.SharyApplication.appContext }.getOrNull()

    /** Инициализирует Firebase и выполняет анонимный вход, если пользователя нет */
    suspend fun ensureSignedIn(): AuthResult = try {
        if (!isAvailable) {
            AuthResult.Failed("firebase_not_configured")
        } else {
            val user = auth.currentUser

            if (user != null) {
                AuthResult.Ready(user.uid)
            } else {
                val created = auth.signInAnonymously().await().user
                if (created == null) {
                    AuthResult.Failed("anonymous_sign_in_failed")
                } else {
                    AuthResult.Ready(created.uid)
                }
            }
        }
    } catch (error: Exception) {
        AuthResult.Failed(error.message ?: "auth_error")
    }

    /** Текущий пользователь Firebase или null */
    fun currentUser(): FirebaseUser? = auth.currentUser

    /** Выход из профиля: следующий вход создаст новый анонимный аккаунт */
    fun signOut() {
        auth.signOut()
    }
}
