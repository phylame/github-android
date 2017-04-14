package pw.phylame.support

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.Dimension
import android.support.annotation.StyleableRes

@ColorInt
fun Context.getStyledColor(@StyleableRes attr: Int, @ColorInt default: Int = 0): Int {
    val a = obtainStyledAttributes(intArrayOf(attr))
    val color = a.getColor(0, default)
    a.recycle()
    return color
}

@Dimension
fun Context.getStyledPixel(@StyleableRes attr: Int, @Dimension default: Int = 0): Int {
    val a = obtainStyledAttributes(intArrayOf(attr))
    val color = a.getDimensionPixelSize(0, default)
    a.recycle()
    return color
}

fun Context.dip(value: Float): Float = resources.displayMetrics.density * value + 0.5F
