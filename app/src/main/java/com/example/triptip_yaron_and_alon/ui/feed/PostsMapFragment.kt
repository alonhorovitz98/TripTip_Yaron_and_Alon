package com.example.triptip_yaron_and_alon.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.triptip_yaron_and_alon.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class PostsMapFragment : Fragment(), OnMapReadyCallback {

    private val feedViewModel: FeedViewModel by activityViewModels()
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_posts_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loading = view.findViewById<ProgressBar>(R.id.loadingIndicator)
        loading.visibility = View.VISIBLE

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        view?.findViewById<ProgressBar>(R.id.loadingIndicator)?.visibility = View.GONE

        feedViewModel.posts.observe(viewLifecycleOwner) { posts ->
            map.clear()
            val boundsBuilder = LatLngBounds.Builder()
            var markerCount = 0

            posts.filter { it.latitude != null && it.longitude != null }.forEach { post ->
                val latLng = LatLng(post.latitude!!, post.longitude!!)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(post.userName)
                        .snippet(post.text.take(60))
                )
                marker?.tag = post.id
                boundsBuilder.include(latLng)
                markerCount++
            }

            if (markerCount > 0) {
                try {
                    val padding = resources.getDimensionPixelSize(R.dimen.map_bounds_padding)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding)
                    )
                } catch (e: Exception) {
                    // Single marker or layout not ready — default zoom
                }
            }
        }

        map.setOnMarkerClickListener { marker ->
            val postId = marker.tag as? String ?: return@setOnMarkerClickListener false
            val action = PostsMapFragmentDirections
                .actionPostsMapFragmentToPostDetailsFragment(postId)
            findNavController().navigate(action)
            true
        }

        if (feedViewModel.posts.value.isNullOrEmpty()) {
            feedViewModel.loadPosts()
        }
    }
}
