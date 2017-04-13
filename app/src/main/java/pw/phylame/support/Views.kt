package pw.phylame.support

import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.widget.TextView

fun TextView.tintDrawables() {
    val colors = textColors
    val drawables = arrayOfNulls<Drawable>(4)
    compoundDrawablesRelative.forEachIndexed { index, drawable ->
        if (drawable != null) {
            val wrapper = DrawableCompat.wrap(drawable)
            DrawableCompat.setTintList(wrapper, colors)
            drawables[index] = wrapper
        }
    }
    setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3])
}