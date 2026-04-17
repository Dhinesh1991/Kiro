package com.pinkauto.app.data.local.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.pinkauto.app.domain.AuthSession
import com.pinkauto.app.domain.KycStatus
import com.pinkauto.app.domain.User
import com.pinkauto.app.domain.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException

class SessionStore(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("session.preferences_pb")
    }

    val sessionFlow: Flow<AuthSession?> = dataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs -> prefs.toSession() }

    val accessTokenFlow: Flow<String?> = sessionFlow.map { it?.accessToken }

    suspend fun saveSession(session: AuthSession) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS] = session.accessToken
            prefs[Keys.REFRESH] = session.refreshToken
            prefs[Keys.USER_ID] = session.user.id
            prefs[Keys.NAME] = session.user.name
            prefs[Keys.PHONE] = session.user.phone
            prefs[Keys.ROLE] = session.user.role.name
            prefs[Keys.KYC] = session.user.kycStatus.name
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun currentAccessToken(): String? = accessTokenFlow.firstOrNull()

    private fun Preferences.toSession(): AuthSession? {
        val access = this[Keys.ACCESS] ?: return null
        val refresh = this[Keys.REFRESH] ?: return null
        val user = User(
            id = this[Keys.USER_ID] ?: return null,
            name = this[Keys.NAME] ?: return null,
            phone = this[Keys.PHONE] ?: return null,
            role = UserRole.valueOf(this[Keys.ROLE] ?: return null),
            kycStatus = KycStatus.valueOf(this[Keys.KYC] ?: KycStatus.PENDING.name)
        )
        return AuthSession(access, refresh, user)
    }

    private object Keys {
        val ACCESS = stringPreferencesKey("access")
        val REFRESH = stringPreferencesKey("refresh")
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val PHONE = stringPreferencesKey("phone")
        val ROLE = stringPreferencesKey("role")
        val KYC = stringPreferencesKey("kyc")
    }
}
