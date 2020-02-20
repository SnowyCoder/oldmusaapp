package it.cnr.oldmusa.api

import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.lifecycle.ViewModel

class CacheModel : ViewModel() {
    val mapCache: LruCache<Int, Bitmap> = LruCache(3)
}
