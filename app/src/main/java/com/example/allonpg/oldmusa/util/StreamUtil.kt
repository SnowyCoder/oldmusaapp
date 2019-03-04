package com.example.allonpg.oldmusa.util

import java.io.ByteArrayOutputStream
import java.io.InputStream


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
}