package com.ownwn.baseless

import java.io.IOException
import java.io.OutputStream


class LimitedOutputStream(private val os: OutputStream, private val limit: Int) : OutputStream() {
    private var written = 0

    @Throws(IOException::class)
    override fun write(b: Int) {
        if (written++ > limit) throw IOException("Exceeded write limit!")
        os.write(b)
    }

    @Throws(IOException::class)
    override fun close() {
        os.close()
    }
}