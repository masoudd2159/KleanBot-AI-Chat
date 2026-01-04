package dev.pegasus.kleanbot.presentation.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import kotlin.math.abs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.pegasus.kleanbot.data.entities.Message
import dev.pegasus.kleanbot.databinding.ItemHomeLeftBinding
import dev.pegasus.kleanbot.databinding.ItemHomeRightBinding
import dev.pegasus.kleanbot.presentation.enums.OpenAIRole
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.util.Collections

/**
 * Created by: Sohaib Ahmed
 * Date: 5/7/2025
 *
 * Links:
 * - LinkedIn: https://linkedin.com/in/epegasus
 * - GitHub: https://github.com/epegasus
 */

class AdapterOpenAI : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {

    private val parser = Parser.builder()
        .extensions(Collections.singletonList(TablesExtension.create()))
        .build()
    private val renderer = HtmlRenderer.builder()
        .extensions(Collections.singletonList(TablesExtension.create()))
        .build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> CustomViewHolderUser(ItemHomeRightBinding.inflate(layoutInflater, parent, false))
            else -> CustomViewHolderAssistant(ItemHomeLeftBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)

        when (holder) {
            is CustomViewHolderUser -> holder.binding.bindViews(currentItem)
            is CustomViewHolderAssistant -> holder.binding.bindViews(currentItem)
        }
    }

    private fun ItemHomeRightBinding.bindViews(message: Message) {
        mtvRightTextItemHome.text = message.content
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ItemHomeLeftBinding.bindViews(message: Message) {
        wvMessageItemHomeLeft.settings.javaScriptEnabled = false
        wvMessageItemHomeLeft.setBackgroundColor(0x00000000) // Transparent

        // Handle Horizontal Scrolling
        wvMessageItemHomeLeft.setOnTouchListener(object : android.view.View.OnTouchListener {
            private var startX = 0f
            private var startY = 0f

            override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(event.x - startX)
                        val dy = abs(event.y - startY)
                        // If horizontal movement is significant and greater than vertical
                        if (dx > 10 && dx > dy) {
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        } else if (dy > dx) {
                             v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                return false // Allow WebView to handle the event
            }
        })

        val document = parser.parse(message.content)
        val htmlBody = renderer.render(document)
            .replace("<table>", "<div class=\"table-wrapper\"><table>")
            .replace("</table>", "</table></div>")

        val fullHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="stylesheet" type="text/css" href="markdown.css">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            </head>
            <body>
                $htmlBody
            </body>
            </html>
        """.trimIndent()

        wvMessageItemHomeLeft.loadDataWithBaseURL("file:///android_asset/", fullHtml, "text/html", "UTF-8", null)
    }

    inner class CustomViewHolderUser(val binding: ItemHomeRightBinding) : RecyclerView.ViewHolder(binding.root)
    inner class CustomViewHolderAssistant(val binding: ItemHomeLeftBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].role == OpenAIRole.USER.value) 1 else 0
    }

    object DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.content == newItem.content
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
