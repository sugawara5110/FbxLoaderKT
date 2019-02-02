package jp.sugasato.fbxloaderkt

import kotlin.math.pow

fun convertUCHARtoUINT(arr:UByteArray?,offset:Int):UInt {
    return ((arr!![3 + offset].toInt() shl 24) or (arr!![2 + offset].toInt() shl 16) or
            (arr!![1 + offset].toInt() shl 8) or (arr!![0 + offset].toInt())).toUInt()
}

fun convertUCHARtoINT32(arr:UByteArray?,offset:Int):Int {
    return (arr!![3 + offset].toInt() shl 24) or (arr!![2 + offset].toInt() shl 16) or
            (arr!![1 + offset].toInt() shl 8) or (arr!![0 + offset].toInt())
}

fun ConvertUCHARtoINT32(arr:UByteArray, offset:Int,outsize:Int):IntArray {
    var outArr = IntArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 4 + offset
        outArr[i] = convertUCHARtoINT32(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoint64(arr:UByteArray?,offset:Int):Long {
    return (arr!![7 + offset].toLong() shl 56) or (arr!![6 + offset].toLong() shl 48) or
            (arr!![5 + offset].toLong() shl 40) or (arr!![4 + offset].toLong() shl 32) or
            (arr!![3 + offset].toLong() shl 24) or (arr!![2 + offset].toLong() shl 16) or
            (arr!![1 + offset].toLong() shl 8) or (arr!![0 + offset].toLong())
}

fun ConvertUCHARtoint64_t(arr:UByteArray?,offset:Int, outsize:Int):LongArray {
    var outArr = LongArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 8 + offset
        outArr[i] = convertUCHARtoint64(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoUINT64(arr:UByteArray?,offset:Int):ULong {
    return ((arr!![7 + offset].toLong()).toULong() shl 56) or ((arr!![6 + offset].toLong()).toULong() shl 48) or
            ((arr!![5 + offset].toLong()).toULong() shl 40) or ((arr!![4 + offset].toLong()).toULong() shl 32) or
            ((arr!![3 + offset].toLong()).toULong() shl 24) or ((arr!![2 + offset].toLong()).toULong() shl 16) or
            ((arr!![1 + offset].toLong()).toULong() shl 8) or ((arr!![0 + offset].toLong()).toULong())
}

fun convertUCHARtoDouble(arr:UByteArray?,offset:Int):Double {
    //(-1)S × (1 + M) × 2E
    //S = 符号部(1bit), E = 指数部(11bit), M = 仮数部(52bit)
    val sign = -1 * (arr!![7 + offset].toInt() shr 7 and 0x01)//1bit
    val exponent = ((arr!![7 + offset].toInt() shl 8) or (arr!![6 + offset].toInt())) shr 4 and 0x7ff//11bit
    val fraction =
        (((arr!![6 + offset].toLong()).toULong() shl 48) or ((arr!![5 + offset].toLong()).toULong() shl 40) or
                ((arr!![4 + offset].toLong()).toULong() shl 32) or ((arr!![3 + offset].toLong()).toULong() shl 24) or
                ((arr!![2 + offset].toLong()).toULong() shl 16) or ((arr!![1 + offset].toLong()).toULong() shl 8) or
                ((arr!![0 + offset].toLong()).toULong()))
    var sumfra = 0.0
    var siftfra = fraction
    for (i: Int in 52..1 step -1) {
        val iDou: Double = i.toDouble()
        sumfra += ((siftfra and 0x01u).toInt()).toDouble() * 2.0.pow(-iDou)
        siftfra = siftfra shr 1
    }
    return sign * (1.0 + sumfra) * 2.0.pow(exponent)
}

fun ConvertUCHARtoDouble(arr:UByteArray?, offset:Int, outsize:Int):DoubleArray {
    var outArr = DoubleArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 8 + offset
        outArr[i] = convertUCHARtoDouble(arr, Offset)
    }
    return outArr
}

fun convertUCHARtoFloat(arr:UByteArray?,offset:Int):Float {
    //(-1)S × (1 + M) × 2E
    //S = 符号部(1bit), E = 指数部(8bit), M = 仮数部(23bit)
    val sign = -1 * (arr!![3 + offset].toInt() shr 7 and 0x01)//1bit
    val exponent = ((arr!![3 + offset].toInt() shl 1) or (arr!![2 + offset].toInt())) shr 7 and 0xff//8bit
    val fraction =
        (arr!![2 + offset].toInt() shl 16) or (arr!![1 + offset].toInt() shl 8) or (arr!![0 + offset].toInt())
    var sumfra = 0.0
    var siftfra = fraction
    for (i: Int in 23..1 step -1) {
        val iDou: Double = i.toDouble()
        sumfra += ((siftfra and 0x01).toInt()).toFloat() * 2.0.pow(-iDou)
        siftfra = siftfra shr 1
    }
    return (sign * (1.0 + sumfra) * 2.0.pow(exponent)).toFloat()
}

fun ConvertUCHARtofloat(arr:UByteArray?, offset:Int, outsize:Int):FloatArray {
    var outArr = FloatArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 4 + offset
        outArr[i] = convertUCHARtoFloat(arr, Offset)
    }
    return outArr
}