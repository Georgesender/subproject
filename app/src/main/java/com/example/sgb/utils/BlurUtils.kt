package com.example.sgb.utils

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView

object BlurUtils {
    const val BLUR_RADIUS = 20f

    /** Накладає розмиття на imageView, або прибирає effect, якщо radius == 0 */
    fun applyBlur(imageView: ImageView, radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (radius > 0f) {
                val effect = RenderEffect.createBlurEffect(
                    radius, radius, Shader.TileMode.CLAMP
                )
                imageView.setRenderEffect(effect)
            } else {
                imageView.setRenderEffect(null)
            }
        }
    }
}