package com.lyy.keepassa.util.transformation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.RSRuntimeException
import com.arialyy.frame.util.ResUtil
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.lyy.keepassa.R
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.internal.FastBlur
import jp.wasabeef.glide.transformations.internal.RSBlur

/**
 * @Author laoyuyu
 * @Description
 * @Date 20:55 2024/5/14
 **/
class WhiteBgBlurTransformation(val radius: Int, val sampling: Int) : BlurTransformation(radius, sampling) {
  override fun transform(
    context: Context,
    pool: BitmapPool,
    toTransform: Bitmap,
    outWidth: Int,
    outHeight: Int
  ): Bitmap {
    val width = toTransform.width
    val height = toTransform.height
    val scaledWidth = width / sampling
    val scaledHeight = height / sampling

    var bitmap: Bitmap = pool[scaledWidth, scaledHeight, ARGB_8888]

    bitmap.eraseColor(ResUtil.getColor(R.color.background_color))
    bitmap.density = toTransform.getDensity()
    val canvas = Canvas(bitmap)
    canvas.scale(1 / sampling.toFloat(), 1 / sampling.toFloat())
    val paint = Paint()
    paint.flags = Paint.FILTER_BITMAP_FLAG
    canvas.drawBitmap(toTransform, 0f, 0f, paint)

    bitmap = try {
      RSBlur.blur(context, bitmap, radius)
    } catch (e: RSRuntimeException) {
      FastBlur.blur(bitmap, radius, true)
    }

    return bitmap
  }
}