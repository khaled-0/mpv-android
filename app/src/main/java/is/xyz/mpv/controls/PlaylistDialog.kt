package `is`.xyz.mpv.controls

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.R
import `is`.xyz.mpv.Utils
import `is`.xyz.mpv.Utils.getThemeColorAttribute
import `is`.xyz.mpv.databinding.DialogPlaylistBinding


internal class PlaylistDialog(private val player: MPVView) {
    private lateinit var binding: DialogPlaylistBinding

    private var playlist = listOf<MPVView.PlaylistItem>()
    private var selectedIndex = -1

    interface Listeners {
        fun pickFile()
        fun openUrl()
        fun onItemPicked(item: MPVView.PlaylistItem)
    }

    var listeners: Listeners? = null

    fun buildView(layoutInflater: LayoutInflater): View {
        binding = DialogPlaylistBinding.inflate(layoutInflater)

        // Set up recycler view
        binding.list.adapter = CustomAdapter(this)
        binding.list.setHasFixedSize(true)
        refresh()

        binding.fileBtn.setOnClickListener { listeners?.pickFile() }
        binding.urlBtn.setOnClickListener { listeners?.openUrl() }

        binding.shuffleBtn.setOnClickListener {
            player.changeShuffle(true)
            refresh()
        }
        binding.repeatBtn.setOnClickListener {
            player.cycleRepeat()
            refresh()
        }

        return binding.root
    }

    fun refresh() {
        selectedIndex = MPVLib.getPropertyInt("playlist-pos") ?: -1
        playlist = player.loadPlaylist()
        Log.v(TAG, "PlaylistDialog: loaded ${playlist.size} items")
        binding.list.adapter!!.notifyDataSetChanged()
        binding.list.scrollToPosition(playlist.indexOfFirst { it.index == selectedIndex })

        /*
         * At least on api 33 there is in some cases a (reproducible) bug, where the space below the
         * recycler view for the two buttons is not taken into account and they go out-of-bounds of the
         * alert dialog. This fixes it.
         */
        binding.list.post {
            binding.list.parent.requestLayout()
        }

        val accent = getThemeColorAttribute(binding.root.context)
        val normal = getThemeColorAttribute(binding.root.context, android.R.attr.colorForeground)

        val shuffleState = player.getShuffle()
        binding.shuffleBtn.apply {
            isEnabled = playlist.size > 1
            imageTintList = if (isEnabled)
                if (shuffleState) ColorStateList.valueOf(accent) else ColorStateList.valueOf(normal)
            else
                ColorStateList.valueOf(normal)
        }
        val repeatState = player.getRepeat()
        binding.repeatBtn.apply {
            imageTintList =
                if (repeatState > 0) ColorStateList.valueOf(accent) else ColorStateList.valueOf(
                    normal
                )
            setImageResource(if (repeatState == 2) R.drawable.round_repeat_one_24 else R.drawable.round_repeat_24)
        }
    }

    private fun clickItem(position: Int) {
        val item = playlist[position]
        listeners?.onItemPicked(item)
    }

    class CustomAdapter(private val parent: PlaylistDialog) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        class ViewHolder(private val parent: PlaylistDialog, view: View) :
            RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(android.R.id.text1)
            private val playingIndicator: ImageView = view.findViewById(R.id.playingIndicator)

            init {
                view.setOnClickListener {
                    parent.clickItem(bindingAdapterPosition)
                }
            }

            fun bind(item: MPVView.PlaylistItem, selected: Boolean) {
                textView.text = item.title ?: Utils.fileBasename(item.filename)
                playingIndicator.visibility = if (selected) View.VISIBLE else View.INVISIBLE
                textView.setTextColor(
                    if (selected) getThemeColorAttribute(textView.context) else getThemeColorAttribute(
                        textView.context,
                        android.R.attr.colorForeground
                    )
                )
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dialog_playlist_item, viewGroup, false)
            return ViewHolder(parent, view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val item = parent.playlist[position]
            viewHolder.bind(item, item.index == parent.selectedIndex)
        }

        override fun getItemCount() = parent.playlist.size
    }


    companion object {
        private const val TAG = "mpv"
    }
}