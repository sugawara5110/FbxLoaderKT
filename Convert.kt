package jp.sugasato.fbxloaderkt

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun convertUCHARtoUINT(arr:UByteArray?,offset:Int):UInt {
    return ((arr!![3 + offset].toInt() shl 24) or (arr[2 + offset].toInt() shl 16) or
            (arr[1 + offset].toInt() shl 8) or (arr[0 + offset].toInt())).toUInt()
}

fun convertUCHARtoINT32(arr:UByteArray?,offset:Int):Int {
    return (arr!![3 + offset].toInt() shl 24) or (arr[2 + offset].toInt() shl 16) or
            (arr[1 + offset].toInt() shl 8) or (arr[0 + offset].toInt())
}

fun ConvertUCHARtoINT32(arr:UByteArray, offset:Int,outsize:Int):IntArray {
    val outArr = IntArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 4 + offset
        outArr[i] = convertUCHARtoINT32(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoint64(arr:UByteArray?,offset:Int):Long {
    return (arr!![7 + offset].toLong() shl 56) or (arr[6 + offset].toLong() shl 48) or
            (arr[5 + offset].toLong() shl 40) or (arr[4 + offset].toLong() shl 32) or
            (arr[3 + offset].toLong() shl 24) or (arr[2 + offset].toLong() shl 16) or
            (arr[1 + offset].toLong() shl 8) or (arr[0 + offset].toLong())
}

fun ConvertUCHARtoint64_t(arr:UByteArray?,offset:Int, outsize:Int):LongArray {
    val outArr = LongArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 8 + offset
        outArr[i] = convertUCHARtoint64(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoUINT64(arr:UByteArray?,offset:Int):ULong {
    return ((arr!![7 + offset].toLong()).toULong() shl 56) or ((arr[6 + offset].toLong()).toULong() shl 48) or
            ((arr[5 + offset].toLong()).toULong() shl 40) or ((arr[4 + offset].toLong()).toULong() shl 32) or
            ((arr[3 + offset].toLong()).toULong() shl 24) or ((arr[2 + offset].toLong()).toULong() shl 16) or
            ((arr[1 + offset].toLong()).toULong() shl 8) or ((arr[0 + offset].toLong()).toULong())
}

fun convertUCHARtoDouble(arr:UByteArray?,offset:Int):Double {
    //(-1)S × (1 + M) × 2E
    //S = 符号部(1bit), E = 指数部(11bit), M = 仮数部(52bit)
    val sign = 1.0 - 2.0 * (arr!![7 + offset].toInt() ushr 7 and 0x01)//1bit
    val exponent = ((arr[7 + offset].toInt() shl 4) or (arr[6 + offset].toInt() ushr 4)) and 0x7ff//11bit
    val fraction: ULong =
        (((arr[6 + offset].toLong()).toULong() shl 48) or ((arr[5 + offset].toLong()).toULong() shl 40) or
                ((arr[4 + offset].toLong()).toULong() shl 32) or ((arr[3 + offset].toLong()).toULong() shl 24) or
                ((arr[2 + offset].toLong()).toULong() shl 16) or ((arr[1 + offset].toLong()).toULong() shl 8) or
                ((arr[0 + offset].toLong()).toULong()))
    var sumfra = 0.0
    var siftfra = fraction
    for (i: Int in 52 downTo 1) {
        val iDou: Double = i.toDouble()
        sumfra += ((siftfra and 0x01u).toInt()).toDouble() * 2.0.pow(-iDou)
        siftfra = siftfra shr 1
    }
    return sign * (1.0 + sumfra) * 2.0.pow(exponent - 1023)
}

fun ConvertUCHARtoDouble(arr:UByteArray?, offset:Int, outsize:Int):DoubleArray {
    val outArr = DoubleArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 8 + offset
        outArr[i] = convertUCHARtoDouble(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoFloat(arr:UByteArray?,offset:Int):Float {
    //(-1)S × (1 + M) × 2E
    //S = 符号部(1bit), E = 指数部(8bit), M = 仮数部(23bit)
    val sign = 1.0 - 2.0 * (arr!![3 + offset].toInt() ushr 7 and 0x01)//1bit
    val exponent = ((arr[3 + offset].toInt() shl 1) or (arr[2 + offset].toInt() ushr 7)) and 0xff//8bit
    val fraction: UInt =
        ((arr[2 + offset].toUInt() shl 16) or (arr[1 + offset].toUInt() shl 8) or (arr[0 + offset].toUInt()))
    var sumfra = 0.0
    var siftfra = fraction
    for (i: Int in 23 downTo 1) {
        val iDou: Double = i.toDouble()
        sumfra += ((siftfra and 0x01u).toInt()).toFloat() * 2.0.pow(-iDou)
        siftfra = siftfra shr 1
    }
    return (sign * (1.0 + sumfra) * 2.0.pow(exponent - 127)).toFloat()
}

fun ConvertUCHARtofloat(arr:UByteArray?, offset:Int, outsize:Int):FloatArray {
    val outArr = FloatArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 4 + offset
        outArr[i] = convertUCHARtoFloat(arr, Offset)
    }
    return outArr
}

fun nameComparison(name1: CharArray?, name2: CharArray?): Boolean {
    var stInd1 = 0
    var stInd2 = 0
    for (i in name1!!.size - 1 downTo 0) {
        if (name1[i] == ' ') {
            stInd1 = i + 1
            break
        }
    }
    for (i in name2!!.size - 1 downTo 0) {
        if (name2[i] == ' ') {
            stInd2 = i + 1
            break
        }
    }
    val n1 = name1.copyOfRange(stInd1, name1.size)
    val n2 = name2.copyOfRange(stInd2, name2.size)
    if (n1.size != n2.size) return false
    for (i in 0 until n1.size) {
        if (n1[i] != n2[i]) return false
    }
    return true
}

fun nameComparison(name1: Array<Char?>, name2: String): Boolean {
    val ch1 = CharArray(name1.size)
    val ch2 = CharArray(name2.length)
    for (i in 0..name1.size - 1) ch1[i] = name1[i]!!.toChar()
    for (i in 0..name2.length - 1) ch2[i] = name2[i].toChar()
    return nameComparison(ch1, ch2)
}

fun nameComparison(name1: CharArray?, name2: String): Boolean {
    val ch1 = name1!!.copyOf()
    val ch2 = CharArray(name2.length)
    for (i in 0..name2.length - 1) ch2[i] = name2[i].toChar()
    return nameComparison(ch1, ch2)
}

fun nameComparison(name1: NameSet?, name2: String): Boolean {
    return nameComparison(name1!!.getName(), name2)
}

fun MatrixScaling(mat: DoubleArray, sx: Double, sy: Double, sz: Double) {
    mat[0] = sx; mat[1] = 0.0; mat[2] = 0.0; mat[3] = 0.0
    mat[4] = 0.0; mat[5] = sy; mat[6] = 0.0; mat[7] = 0.0
    mat[8] = 0.0; mat[9] = 0.0; mat[10] = sz; mat[11] = 0.0
    mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
}

fun MatrixRotationX(mat: DoubleArray, theta: Double) {
    val the = theta * 3.14 / 180.0
    mat[0] = 1.0; mat[1] = 0.0; mat[2] = 0.0; mat[3] = 0.0
    mat[4] = 0.0; mat[5] = cos(the); mat[6] = sin(the); mat[7] = 0.0
    mat[8] = 0.0; mat[9] = -sin(the); mat[10] = cos(the); mat[11] = 0.0
    mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
}

fun MatrixRotationY(mat: DoubleArray, theta: Double) {
    val the = theta * 3.14 / 180.0
    mat[0] = cos(the); mat[1] = 0.0; mat[2] = -sin(the); mat[3] = 0.0
    mat[4] = 0.0; mat[5] = 1.0; mat[6] = 0.0; mat[7] = 0.0
    mat[8] = sin(the); mat[9] = 0.0; mat[10] = cos(the); mat[11] = 0.0
    mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
}

fun MatrixRotationZ(mat: DoubleArray, theta: Double) {
    val the = theta * 3.14 / 180.0
    mat[0] = cos(the); mat[1] = sin(the); mat[2] = 0.0; mat[3] = 0.0
    mat[4] = -sin(the); mat[5] = cos(the); mat[6] = 0.0; mat[7] = 0.0
    mat[8] = 0.0; mat[9] = 0.0; mat[10] = 1.0; mat[11] = 0.0
    mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
}

fun MatrixTranslation(mat: DoubleArray, movx: Double, movy: Double, movz: Double) {
    mat[0] = 1.0; mat[1] = 0.0; mat[2] = 0.0; mat[3] = 0.0
    mat[4] = 0.0; mat[5] = 1.0; mat[6] = 0.0; mat[7] = 0.0
    mat[8] = 0.0; mat[9] = 0.0; mat[10] = 1.0; mat[11] = 0.0
    mat[12] = movx; mat[13] = movy; mat[14] = movz; mat[15] = 1.0
}

fun MatrixMultiply(outmat: DoubleArray, mat1: DoubleArray, mat2: DoubleArray) {
    outmat[0] = mat1[0] * mat2[0] + mat1[1] * mat2[4] + mat1[2] * mat2[8] + mat1[3] * mat2[12]
    outmat[1] = mat1[0] * mat2[1] + mat1[1] * mat2[5] + mat1[2] * mat2[9] + mat1[3] * mat2[13]
    outmat[2] = mat1[0] * mat2[2] + mat1[1] * mat2[6] + mat1[2] * mat2[10] + mat1[3] * mat2[14]
    outmat[3] = mat1[0] * mat2[3] + mat1[1] * mat2[7] + mat1[2] * mat2[11] + mat1[3] * mat2[15]

    outmat[4] = mat1[4] * mat2[0] + mat1[5] * mat2[4] + mat1[6] * mat2[8] + mat1[7] * mat2[12]
    outmat[5] = mat1[4] * mat2[1] + mat1[5] * mat2[5] + mat1[6] * mat2[9] + mat1[7] * mat2[13]
    outmat[6] = mat1[4] * mat2[2] + mat1[5] * mat2[6] + mat1[6] * mat2[10] + mat1[7] * mat2[14]
    outmat[7] = mat1[4] * mat2[3] + mat1[5] * mat2[7] + mat1[6] * mat2[11] + mat1[7] * mat2[15]

    outmat[8] = mat1[8] * mat2[0] + mat1[9] * mat2[4] + mat1[10] * mat2[8] + mat1[11] * mat2[12]
    outmat[9] = mat1[8] * mat2[1] + mat1[9] * mat2[5] + mat1[10] * mat2[9] + mat1[11] * mat2[13]
    outmat[10] = mat1[8] * mat2[2] + mat1[9] * mat2[6] + mat1[10] * mat2[10] + mat1[11] * mat2[14]
    outmat[11] = mat1[8] * mat2[3] + mat1[9] * mat2[7] + mat1[10] * mat2[11] + mat1[11] * mat2[15]

    outmat[12] = mat1[12] * mat2[0] + mat1[13] * mat2[4] + mat1[14] * mat2[8] + mat1[15] * mat2[12]
    outmat[13] = mat1[12] * mat2[1] + mat1[13] * mat2[5] + mat1[14] * mat2[9] + mat1[15] * mat2[13]
    outmat[14] = mat1[12] * mat2[2] + mat1[13] * mat2[6] + mat1[14] * mat2[10] + mat1[15] * mat2[14]
    outmat[15] = mat1[12] * mat2[3] + mat1[13] * mat2[7] + mat1[14] * mat2[11] + mat1[15] * mat2[15]
}

fun CalDetMat4x4(mat: DoubleArray): Double {
    return (mat[0] * mat[5] * mat[10] * mat[15] + mat[0] * mat[6] * mat[11] * mat[13] + mat[0] * mat[7] * mat[9] * mat[14]
            + mat[1] * mat[4] * mat[11] * mat[14] + mat[1] * mat[6] * mat[8] * mat[15] + mat[1] * mat[7] * mat[10] * mat[12]
            + mat[2] * mat[4] * mat[9] * mat[15] + mat[2] * mat[5] * mat[11] * mat[12] + mat[2] * mat[7] * mat[8] * mat[13]
            + mat[3] * mat[4] * mat[10] * mat[13] + mat[3] * mat[5] * mat[8] * mat[14] + mat[3] * mat[6] * mat[9] * mat[12]
            - mat[0] * mat[5] * mat[11] * mat[14] - mat[0] * mat[6] * mat[9] * mat[15] - mat[0] * mat[7] * mat[10] * mat[13]
            - mat[1] * mat[4] * mat[10] * mat[15] - mat[1] * mat[6] * mat[11] * mat[12] - mat[1] * mat[7] * mat[8] * mat[14]
            - mat[2] * mat[4] * mat[11] * mat[13] - mat[2] * mat[5] * mat[8] * mat[15] - mat[2] * mat[7] * mat[9] * mat[12]
            - mat[3] * mat[4] * mat[9] * mat[14] - mat[3] * mat[5] * mat[10] * mat[12] - mat[3] * mat[6] * mat[8] * mat[13])
}

fun MatrixInverse(outmat: DoubleArray, mat: DoubleArray) {
    val det = CalDetMat4x4(mat)
    val inv_det = (1.0 / det)

    outmat[0] = inv_det *
            (mat[5] * mat[10] * mat[15] + mat[6] * mat[11] * mat[13] + mat[7] * mat[9] * mat[14] - mat[5] * mat[11] * mat[14] - mat[6] * mat[9] * mat[15] - mat[7] * mat[10] * mat[13])
    outmat[1] = inv_det *
            (mat[1] * mat[11] * mat[14] + mat[2] * mat[9] * mat[15] + mat[3] * mat[10] * mat[13] - mat[1] * mat[10] * mat[15] - mat[2] * mat[11] * mat[13] - mat[3] * mat[9] * mat[14])
    outmat[2] = inv_det *
            (mat[1] * mat[6] * mat[15] + mat[2] * mat[7] * mat[13] + mat[3] * mat[5] * mat[14] - mat[1] * mat[7] * mat[14] - mat[2] * mat[5] * mat[15] - mat[3] * mat[6] * mat[13])
    outmat[3] = inv_det *
            (mat[1] * mat[7] * mat[10] + mat[2] * mat[5] * mat[11] + mat[3] * mat[6] * mat[9] - mat[1] * mat[6] * mat[11] - mat[2] * mat[7] * mat[9] - mat[3] * mat[5] * mat[10])

    outmat[4] = inv_det *
            (mat[4] * mat[11] * mat[14] + mat[6] * mat[8] * mat[15] + mat[7] * mat[10] * mat[12] - mat[4] * mat[10] * mat[15] - mat[6] * mat[11] * mat[12] - mat[7] * mat[8] * mat[14])
    outmat[5] = inv_det *
            (mat[0] * mat[10] * mat[15] + mat[2] * mat[11] * mat[12] + mat[3] * mat[8] * mat[14] - mat[0] * mat[11] * mat[14] - mat[2] * mat[8] * mat[15] - mat[3] * mat[10] * mat[12])
    outmat[6] = inv_det *
            (mat[0] * mat[7] * mat[14] + mat[2] * mat[4] * mat[15] + mat[3] * mat[6] * mat[12] - mat[0] * mat[6] * mat[15] - mat[2] * mat[7] * mat[12] - mat[3] * mat[4] * mat[14])
    outmat[7] = inv_det *
            (mat[0] * mat[6] * mat[11] + mat[2] * mat[7] * mat[8] + mat[3] * mat[4] * mat[10] - mat[0] * mat[7] * mat[10] - mat[2] * mat[4] * mat[11] - mat[3] * mat[6] * mat[8])

    outmat[8] = inv_det *
            (mat[4] * mat[9] * mat[15] + mat[5] * mat[11] * mat[12] + mat[7] * mat[8] * mat[13] - mat[4] * mat[11] * mat[13] - mat[5] * mat[8] * mat[15] - mat[7] * mat[9] * mat[12])
    outmat[9] = inv_det *
            (mat[0] * mat[11] * mat[13] + mat[1] * mat[8] * mat[15] + mat[3] * mat[9] * mat[12] - mat[0] * mat[9] * mat[15] - mat[1] * mat[11] * mat[12] - mat[3] * mat[8] * mat[13])
    outmat[10] = inv_det *
            (mat[0] * mat[5] * mat[15] + mat[1] * mat[7] * mat[12] + mat[3] * mat[4] * mat[13] - mat[0] * mat[7] * mat[13] - mat[1] * mat[4] * mat[15] - mat[3] * mat[5] * mat[12])
    outmat[11] = inv_det *
            (mat[0] * mat[7] * mat[9] + mat[1] * mat[4] * mat[11] + mat[3] * mat[5] * mat[8] - mat[0] * mat[5] * mat[11] - mat[1] * mat[7] * mat[8] - mat[3] * mat[4] * mat[9])

    outmat[12] = inv_det *
            (mat[4] * mat[10] * mat[13] + mat[5] * mat[8] * mat[14] + mat[6] * mat[9] * mat[12] - mat[4] * mat[9] * mat[14] - mat[5] * mat[10] * mat[12] - mat[6] * mat[8] * mat[13])
    outmat[13] = inv_det *
            (mat[0] * mat[9] * mat[14] + mat[1] * mat[10] * mat[12] + mat[2] * mat[8] * mat[13] - mat[0] * mat[10] * mat[13] - mat[1] * mat[8] * mat[14] - mat[2] * mat[9] * mat[12])
    outmat[14] = inv_det *
            (mat[0] * mat[6] * mat[13] + mat[1] * mat[4] * mat[14] + mat[2] * mat[5] * mat[12] - mat[0] * mat[5] * mat[14] - mat[1] * mat[6] * mat[12] - mat[2] * mat[4] * mat[13])
    outmat[15] = inv_det *
            (mat[0] * mat[5] * mat[10] + mat[1] * mat[6] * mat[8] + mat[2] * mat[4] * mat[9] - mat[0] * mat[6] * mat[9] - mat[1] * mat[4] * mat[10] - mat[2] * mat[5] * mat[8])
}