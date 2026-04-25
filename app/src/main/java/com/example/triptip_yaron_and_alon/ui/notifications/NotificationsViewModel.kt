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

    private var loadJob: Job? = null
    private var currentUserId: String? = null

    fun loadNotifications() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (userId == null) {
                _notifications.value = emptyList()
                _isLoading.value = false
                return@launch
            }
            currentUserId = userId
            try {
                notificationsDataSource.getNotificationsForUser(userId).collect { list ->
                    _notifications.value = list
                    _isLoading.value = false
                }
            } catch (_: Exception) {
                _isLoading.value = false
            }
        }
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
        val userId = currentUserId ?: return
        viewModelScope.launch {
            notificationsDataSource.markAllAsRead(userId)
            _notifications.value = _notifications.value?.map { it.copy(isRead = true) }
        }
    }
}
