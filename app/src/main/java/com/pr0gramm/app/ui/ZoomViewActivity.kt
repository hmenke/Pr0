package com.pr0gramm.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.pr0gramm.app.BuildConfig
import com.pr0gramm.app.R
import com.pr0gramm.app.Settings
import com.pr0gramm.app.feed.FeedItem
import com.pr0gramm.app.io.Cache
import com.pr0gramm.app.parcel.getFreezableExtra
import com.pr0gramm.app.services.ThemeHelper
import com.pr0gramm.app.services.UriHelper
import com.pr0gramm.app.ui.base.BaseAppCompatActivity
import com.pr0gramm.app.util.AndroidUtility.getTintedDrawable
import com.pr0gramm.app.util.decoders.Decoders
import com.pr0gramm.app.util.decoders.PicassoDecoder
import com.pr0gramm.app.util.di.instance
import com.pr0gramm.app.util.visible
import com.squareup.picasso.Picasso
import kotterknife.bindView

class ZoomViewActivity : BaseAppCompatActivity("ZoomViewActivity") {
    private val tag = "ZoomViewActivity" + System.currentTimeMillis()

    internal val item: FeedItem by lazy {
        intent.getFreezableExtra("ZoomViewActivity__item", FeedItem)
                ?: throw IllegalArgumentException("no feed item in intent")
    }

    private val hq: ImageView by bindView(R.id.hq)
    private val busyIndicator: View by bindView(R.id.busy_indicator)
    private val imageView: SubsamplingScaleImageView by bindView(R.id.image)

    private val picasso: Picasso by instance()
    private val cache: Cache by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeHelper.theme.fullscreen)
        super.onCreate(savedInstanceState)

        // normal content view
        setContentView(R.layout.activity_zoom_view)

        imageView.setMaxTileSize(4096)
        imageView.setDebug(BuildConfig.DEBUG)
        imageView.setBitmapDecoderFactory(PicassoDecoder.factory(tag, picasso))
        imageView.setRegionDecoderFactory(Decoders.regionDecoderFactory(cache))

        imageView.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
            override fun onImageLoaded() {
                hideBusyIndicator()

                imageView.maxScale = Math.max(
                        2 * imageView.width.toFloat() / imageView.sWidth,
                        2 * imageView.height.toFloat() / imageView.sWidth
                )
            }
        })

        hq.setImageDrawable(getColoredHqIcon(R.color.grey_700))

        if (Settings.get().loadHqInZoomView && isHqImageAvailable) {
            loadHqImage()
        } else {
            loadImage()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && Settings.get().fullscreenZoomView) {
            val decorView = window.decorView
            var flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

            flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            decorView.systemUiVisibility = flags
        }
    }

    override fun onDestroy() {
        picasso.cancelTag(tag)
        super.onDestroy()
    }

    private fun loadImage() {
        val url = UriHelper.of(this).media(item)
        loadImageWithUrl(url)

        if (isHqImageAvailable) {
            hq.setOnClickListener { loadHqImage() }
            hq.animate().alpha(1f).start()
        } else {
            hq.visibility = View.GONE
        }
    }

    private val isHqImageAvailable: Boolean
        get() = item.fullsize.isNotBlank()

    private fun loadHqImage() {
        hq.setOnClickListener(null)
        hq.setImageDrawable(getColoredHqIcon(ThemeHelper.accentColor))
        hq.animate().alpha(1f).start()

        val url = UriHelper.of(this).media(item, true)
        loadImageWithUrl(url)
    }

    private fun getColoredHqIcon(@ColorRes colorId: Int): Drawable {
        return getTintedDrawable(this, R.drawable.ic_action_high_quality, colorId)
    }

    private fun loadImageWithUrl(url: Uri) {
        showBusyIndicator()
        picasso.cancelTag(tag)
        imageView.setImage(ImageSource.uri(url))
    }

    private fun showBusyIndicator() {
        busyIndicator.visible = true
    }

    private fun hideBusyIndicator() {
        busyIndicator.visible = false
    }

    companion object {
        fun newIntent(context: Context, item: FeedItem): Intent {
            val intent = Intent(context, ZoomViewActivity::class.java)
            intent.putExtra("ZoomViewActivity__item", item)
            return intent
        }
    }
}
