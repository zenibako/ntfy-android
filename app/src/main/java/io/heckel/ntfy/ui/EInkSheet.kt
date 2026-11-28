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
        sheet(context, title) { dialog, content ->
            items.forEach { item ->
                content.addView(button(context, item.title.toString(), filled = false) { dialog.dismiss(); onPick(item) })
            }
            content.addView(button(context, context.getString(android.R.string.cancel), filled = true) { dialog.dismiss() })
        }
    }

    /**
     * A confirm sheet: the [question] as the title, the action ([actionLabel], e.g. "Permanently
     * delete") as the one filled button and an outlined [cancelLabel], so only one button is
     * black. [onCancel] runs only for the Cancel button, as a dialog's negative button would;
     * Back or a tap above the sheet just closes it.
     */
    fun confirm(
        context: Context,
        question: String,
        actionLabel: String,
        cancelLabel: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {},
    ) {
        sheet(context, question) { dialog, content ->
            content.addView(button(context, actionLabel, filled = true) { dialog.dismiss(); onConfirm() })
            content.addView(button(context, cancelLabel, filled = false) { dialog.dismiss(); onCancel() })
        }
    }

    /** The shared frame: no dim or animation, a 3 dp top border, a centred 25 sp title. Shows it. */
    private fun sheet(context: Context, title: String, fill: (BottomSheetDialog, LinearLayout) -> Unit) {
        frame(context, title, fill).show()
    }

    /** Builds (doesn't show) the shared frame, e.g. for a DialogFragment's onCreateDialog. */
    fun frame(context: Context, title: String, fill: (BottomSheetDialog, LinearLayout) -> Unit): BottomSheetDialog {
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
                maxLines = 4
                TextViewCompat.setTextAppearance(this, themeRes(context, MaterialR.attr.textAppearanceTitleLarge))
                setTextColor(Color.BLACK)
            },
        )
        fill(dialog, content)

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
        return dialog
    }

    /**
     * A choice sheet (settings lists, mute durations): one full-width outlined option per entry,
     * the current one 3 dp and Black with a check, then a filled Cancel. Picking applies and
     * closes. Long lists scroll inside the sheet, up to 320 dp tall.
     */
    fun choiceDialog(context: Context, title: String, options: List<String>, selected: Int, onPick: (Int) -> Unit): BottomSheetDialog =
        frame(context, title) { dialog, content ->
            val dp = context.resources.displayMetrics.density
            val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            options.forEachIndexed { i, label ->
                val chosen = i == selected
                list.addView(
                    button(context, if (chosen) "$label  ✓" else label, filled = false) { dialog.dismiss(); onPick(i) }.apply {
                        this as MaterialButton
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        // The current option is 3 dp and Black; the others Medium
                        typeface = android.graphics.Typeface.create(typeface, if (chosen) 900 else 500, false)
                        if (chosen) strokeWidth = (3 * dp).toInt()
                    },
                )
            }
            content.addView(
                android.widget.ScrollView(context).apply {
                    addView(list)
                    val rows = (options.size * 64 * dp).toInt()
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, minOf(rows, (320 * dp).toInt()))
                },
            )
            content.addView(button(context, context.getString(android.R.string.cancel), filled = true) { dialog.dismiss() })
        }

    /**
     * A text-input sheet (settings text fields): the [body] (message and field), then the save
     * action as the one filled button and an outlined Cancel.
     */
    fun inputDialog(context: Context, title: String, body: View, saveLabel: String, onSave: () -> Unit): BottomSheetDialog =
        frame(context, title) { dialog, content ->
            content.addView(body)
            content.addView(button(context, saveLabel, filled = true) { dialog.dismiss(); onSave() })
            content.addView(button(context, context.getString(android.R.string.cancel), filled = false) { dialog.dismiss() })
        }

    /** An information sheet: the [message] in 21 sp, then one filled [okLabel] button. */
    fun info(context: Context, title: String, message: String, okLabel: String) {
        sheet(context, title) { dialog, content ->
            content.addView(
                TextView(context).apply {
                    text = message
                    TextViewCompat.setTextAppearance(this, themeRes(context, MaterialR.attr.textAppearanceBodyLarge))
                    setTextColor(Color.BLACK)
                    setPadding(0, (16 * context.resources.displayMetrics.density).toInt(), 0, 0)
                },
            )
            content.addView(button(context, okLabel, filled = true) { dialog.dismiss() })
        }
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
