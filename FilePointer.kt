package jp.sugasato.fbxloaderkt

import java.io.File

class FilePointer {

    private var pointer: Int = 0
    private var fileStr: ByteArray? = null

    fun setFile(pass: String) {
        val file = File(pass)
        fileStr = file.readBytes()
    }

    fun getPos(): Int {
        return pointer
    }

    fun seekPointer(ind: Int) {
        pointer = ind
    }

    fun getByte(): Byte {
        var ret = fileStr!![pointer]
        pointer++
        return ret
    }

    fun convertBYTEtoUINT(): UInt {
        var ret: UInt = ((fileStr!![3 + pointer].toInt() shl 24) or (fileStr!![2 + pointer].toInt() shl 16) or
                (fileStr!![1 + pointer].toInt() shl 8) or (fileStr!![0 + pointer].toInt())).toUInt()
        pointer += 4
        return ret
    }
}