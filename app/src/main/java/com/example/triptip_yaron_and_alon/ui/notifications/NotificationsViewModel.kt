package com.example.triptip_yaron_and_alon.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.triptip_yaron_and_alon.data.remote.firebase.FirebaseAuthDataSource
import com.example.triptip_yaron_and_alon.data.remote.firebase.NotificationsDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val authDataSource by lazy { FirebaseAuthDataSource() }
    private val notificationsDataSource by lazy { NotificationsDataSource() }

    private val _notifications = MutableLiveData<List<NotificationsDataSource.NotificationDoc>>(emptyList())
    val notifications: LiveData<List<NotificationsDataSource.NotificationDoc>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _unreadCount = MutableLiveData<Int>(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private var listenJob: Job? = null

    fun loadNotifications() {
        if (listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            _isLoading.value = true
            val userId = authDataSource.getCurrentUser().firstOrNull()?.id
            if (userId == null) {
                _notifications.value = emptyList()
                _isLoading.value = false
                return@launch
            }
            notificationsDataSource.getNotificationsForUser(userId).collect { list ->
                _notifications.value = list
                _unreadCount.value = list.count { !it.isRead }
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationsDataSource.markAsRead(notificationId)
        }
    }
}
