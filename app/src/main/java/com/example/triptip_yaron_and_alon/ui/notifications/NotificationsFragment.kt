package com.example.triptip_yaron_and_alon.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptip_yaron_and_alon.databinding.FragmentNotificationsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadNotifications()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(com.example.triptip_yaron_and_alon.R.id.action_notificationsFragment_to_feedFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(
            onNotificationClick = { notification ->
                viewModel.markAsRead(notification.id)
                notification.targetPostId?.let { postId ->
                    val bundle = android.os.Bundle().apply { putString("postId", postId) }
                    findNavController().navigate(
                        com.example.triptip_yaron_and_alon.R.id.action_notificationsFragment_to_postDetailsFragment,
                        bundle
                    )
                }
            }
        )
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            renderNotificationsUi()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            renderNotificationsUi()
        }
    }

    private fun renderNotificationsUi() {
        val notifications = viewModel.notifications.value.orEmpty()
        val loading = viewModel.isLoading.value == true
        val hasItems = notifications.isNotEmpty()

        binding.tvEmpty.visibility = if (!loading && !hasItems) View.VISIBLE else View.GONE
        binding.rvNotifications.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
