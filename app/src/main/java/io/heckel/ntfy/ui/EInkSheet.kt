package io.heckel.ntfy.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.heckel.ntfy.R
import androidx.appcompat.widget.ActionMenuView
import androidx.core.view.size
import androidx.core.widget.TextViewCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

/**
 * E Ink (kompakt) menus as the Kompakt's stock apps show them: instead of a popup, a bottom
 * sheet with a 3 dp black top border, a centred title, full-width outlined 48 dp buttons and a
 * filled black Cancel. No dim, shadow or animation. Only used when BuildConfig.E_INK.
 */
object EInkSheet {
    /**
     * Hides the toolbar's popup overflow button, whenever the toolbar lays out: the E Ink build's
     * own ⋮ item opens [show] with those items instead. The items stay in the menu, so code that
     * shows or hides them keeps working.
     */
    fun hideOverflowButton(toolbar: View) {
        if (toolbar.getTag(R.id.eink_menu_more) == true) return // once per toolbar
        toolbar.setTag(R.id.eink_menu_more, true)
        toolbar.viewTreeObserver.addOnGlobalLayoutListener {
            (toolbar as? ViewGroup)?.let { bar ->
                (0 until bar.childCount).map { bar.getChildAt(it) }.filterIsInstance<ActionMenuView>().forEach { amv ->
                    (0 until amv.childCount).map { amv.getChildAt(it) }
                        .filter { (it.layoutParams as? ActionMenuView.LayoutParams)?.isOverflowButton == true && it.visibility != View.GONE }
                        .forEach { it.visibility = View.GONE }
                }
            }
        }
    }

    /** The items a popup or overflow menu would show: visible, enabled, with a title. */
    fun menuItems(menu: Menu, include: (MenuItem) -> Boolean = { true }): List<MenuItem> =
        (0 until menu.size).map { menu.getItem(it) }.filter { it.isVisible && it.isEnabled && !it.title.isNullOrBlank() && include(it) }

    fun show(context: Context, title: String, items: List<MenuItem>, onPick: (MenuItem) -> Unit) {
        val dp = context.resources.displayMetrics.density
        fun px(v: Float) = (v * dp).toInt()
        val dialog = BottomSheetDialog(context)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(12f), px(3f + 16f), px(12f), px(16f))
            // White, with only a 3 dp black top border.
            background = LayerDrawable(arrayOf(ColorDrawable(Color.BLACK), ColorDrawable(Color.WHITE))).apply {
                setLayerInset(1, 0, px(3f), 0, 0)
            }
        }
        content.addView(
            TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                maxLines = 2
                TextViewCompat.setTextAppearance(this, themeRes(context, MaterialR.attr.textAppearanceTitleLarge))
                setTextColor(Color.BLACK)
            },
        )
        items.forEach { item ->
            content.addView(button(context, item.title.toString(), filled = false) { dialog.dismiss(); onPick(item) })
        }
        content.addView(button(context, context.getString(android.R.string.cancel), filled = true) { dialog.dismiss() })

        dialog.setContentView(content)
        dialog.window?.apply {
            setDimAmount(0f)
            setWindowAnimations(0)
        }
        // Placed expanded before the first layout, so it appears in one redraw instead of sliding.
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)?.apply {
            background = null
            elevation = 0f
        }
        dialog.show()
    }

    private fun button(context: Context, label: String, filled: Boolean, onClick: () -> Unit): View {
        val dp = context.resources.displayMetrics.density
        return MaterialButton(context, null, if (filled) MaterialR.attr.materialButtonStyle else MaterialR.attr.materialButtonOutlinedStyle).apply {
            text = label
            isAllCaps = false
            minHeight = (48 * dp).toInt()
            minimumHeight = (48 * dp).toInt()
            insetTop = 0
            insetBottom = 0
            cornerRadius = (12 * dp).toInt()
            elevation = 0f
            stateListAnimator = null
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            TextViewCompat.setTextAppearance(this, themeRes(context, MaterialR.attr.textAppearanceLabelLarge))
            if (filled) {
                backgroundTintList = ColorStateList.valueOf(Color.BLACK)
                setTextColor(Color.WHITE)
            } else {
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                strokeColor = ColorStateList.valueOf(Color.BLACK)
                strokeWidth = (2 * dp).toInt()
                setTextColor(Color.BLACK)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (16 * dp).toInt()
            }
        }
    }

    private fun themeRes(context: Context, attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.resourceId
    }

}
