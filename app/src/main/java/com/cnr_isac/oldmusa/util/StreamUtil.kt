package com.cnr_isac.oldmusa.util

import android.util.Base64
import android.util.SparseBooleanArray
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest


object StreamUtil {
    fun readFullyAsString(inputStream: InputStream, encoding: String): String {
        return readFully(inputStream).toString(encoding)
    }

    fun readFullyAsBytes(inputStream: InputStream): ByteArray {
        return readFully(inputStream).toByteArray()
    }

    private fun readFully(inputStream: InputStream): ByteArrayOutputStream {
        val res = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length = inputStream.read(buffer)
        while (length != -1) {
            res.write(buffer, 0, length)
            length = inputStream.read(buffer)
        }
        return res
    }

    private val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    fun ByteArray.toHex(): String {
            val hexChars = CharArray(this.size * 2)
            var v: Int
            for (j in 0 until this.size) {
                v = this[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_CHARS[v.ushr(4)]
                hexChars[j * 2 + 1] = HEX_CHARS[v and 0x0F]
            }
            return String(hexChars)
        }


    fun ByteArray.getMd5(): String {
        return Base64.encodeToString(BigInteger(1, MessageDigest.getInstance("MD5").digest(this)).toByteArray(), 16).trim()
    }

    inline fun SparseBooleanArray.forEachTrue(f: (index: Int) -> Unit) {
        for (i in (0..size())) {
            if (!valueAt(i)) continue
            f(keyAt(i))
        }
    }
}