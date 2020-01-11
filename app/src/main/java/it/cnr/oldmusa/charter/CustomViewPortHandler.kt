package it.cnr.oldmusa.charter

import android.graphics.Matrix
import android.graphics.RectF
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomViewPortHandler : ViewPortHandler() {
    var translationListener: (CustomViewPortHandler) -> Unit = {}


    override fun limitTransAndScale(matrix: Matrix?, content: RectF?) {
        val oldTransX = transX
        val oldTransY = transY
        super.limitTransAndScale(matrix, content)

        if (oldTransX != transX || oldTransY != transY) {
            translationListener(this)
        }
    }

    val maxTransY: Float
            get() = (this.mContentRect?.height() ?: 0f) * (scaleY - 1f)

    val canScrollUp: Boolean
        get() = maxTransY - transY > 0.0001
}