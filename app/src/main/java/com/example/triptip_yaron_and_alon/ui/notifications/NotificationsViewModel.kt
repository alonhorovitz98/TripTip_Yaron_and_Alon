package com.example.triptip_yaron_and_alon.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.NotificationsDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val notificationsDataSource by lazy { NotificationsDataSource() }

    private val _notifications =
        MutableLiveData<List<NotificationsDataSource.NotificationDoc>>(emptyList())
    val notifications: LiveData<List<NotificationsDataSource.NotificationDoc>> = _notifications

    /** Live count of unread notifications — drives the badge in the toolbar. */
    val unreadCount: LiveData<Int> = _notifications.map { list -> list.count { !it.isRead } }

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var listenJob: Job? = null
    private var listeningUserId: String? = null

    /**
     * Starts (or restarts) the Firestore listener for the current user's notifications.
     * Restarts automatically when the signed-in user changes (e.g. logout → login as another account).
     */
    fun loadNotifications() {
        viewModelScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (userId == null) {
                stopListening()
                _notifications.value = emptyList()
                _isLoading.value = false
                return@launch
            }
            if (listenJob?.isActive == true && listeningUserId == userId) return@launch

            stopListening()
            listeningUserId = userId
            _isLoading.value = true
            listenJob = viewModelScope.launch {
                try {
                    notificationsDataSource.getNotificationsForUser(userId).collect { list ->
                        _notifications.value = list
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _notifications.value = emptyList()
                    _isLoading.value = false
                }
            }
        }
    }

    /** Cancel the active listener (e.g. on logout). */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        listeningUserId = null
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    /**
     * Mark a single notification as read (called on item tap).
     * Updates Firestore and optimistically updates the local list.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationsDataSource.markAsRead(notificationId)
            _notifications.value = _notifications.value?.map { n ->
                if (n.id == notificationId) n.copy(isRead = true) else n
            }
        }
    }

    /**
     * Mark every notification for the current user as read.
     * Called when the notifications screen is opened.
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id ?: return@launch
            notificationsDataSource.markAllAsRead(userId)
            _notifications.value = _notifications.value?.map { it.copy(isRead = true) }
        }
    }
}
