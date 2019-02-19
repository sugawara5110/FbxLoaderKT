package jp.sugasato.fbxloaderkt

import java.io.InputStream
import android.content.Context

class FilePointer {

    private var pointer: UInt = 0u
    private var fileStr: UByteArray? = null

    fun setFile(con: Context, rawId: Int) {
        val raw: InputStream = con.getResources().openRawResource(rawId)
        fileStr = raw.readBytes().toUByteArray()
    }

    fun getPos(): UInt {
        return pointer
    }

    fun seekPointer(ind: UInt) {
        pointer = ind
    }

    fun getByte(): UByte {
        var ret = fileStr!![pointer.toInt()]
        pointer++
        return ret
    }

    fun convertBYTEtoUINT(): UInt {
        val Pointer = pointer.toInt()
        val ret: Int = ((fileStr!![3 + Pointer].toInt() shl 24) or (fileStr!![2 + Pointer].toInt() shl 16) or
                (fileStr!![1 + Pointer].toInt() shl 8) or (fileStr!![0 + Pointer].toInt())).toInt()
        pointer += 4u
        return ret.toUInt()
    }
}