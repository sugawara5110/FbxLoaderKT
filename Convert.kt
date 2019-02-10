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
    val sign = 1.0 - 2.0 * (arr!![7 + offset].toInt() ushr 7 and 0x01)//1bit
    val exponent = ((arr!![7 + offset].toInt() shl 4) or (arr!![6 + offset].toInt() ushr 4)) and 0x7ff//11bit
    val fraction: ULong =
        (((arr!![6 + offset].toLong()).toULong() shl 48) or ((arr!![5 + offset].toLong()).toULong() shl 40) or
                ((arr!![4 + offset].toLong()).toULong() shl 32) or ((arr!![3 + offset].toLong()).toULong() shl 24) or
                ((arr!![2 + offset].toLong()).toULong() shl 16) or ((arr!![1 + offset].toLong()).toULong() shl 8) or
                ((arr!![0 + offset].toLong()).toULong()))
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
    val sign = 1.0 - 2.0 * (arr!![3 + offset].toInt() ushr 7 and 0x01)//1bit
    val exponent = ((arr!![3 + offset].toInt() shl 1) or (arr!![2 + offset].toInt() ushr 7)) and 0xff//8bit
    val fraction: UInt =
        ((arr!![2 + offset].toUInt() shl 16) or (arr!![1 + offset].toUInt() shl 8) or (arr!![0 + offset].toUInt()))
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
    var outArr = FloatArray(outsize)
    for (i in 0..outsize - 1) {
        val Offset = i * 4 + offset
        outArr[i] = convertUCHARtoFloat(arr, Offset)
    }
    return outArr
}

fun nameComparison(name1: CharArray?, name2: CharArray?): Boolean {
    //名前文字列に空白文字が有る場合,空白文字以前を取り除く
    //空白は1個しかない
    val name1Tmp = name1!!.copyOf()
    var Ind1 = 0
    var name1out: CharArray? = null
    while (name1Tmp.size > Ind1 && name1Tmp[Ind1] != ' ') Ind1++
    val end = name1Tmp.size - 1
    var st = 0
    if (name1Tmp.size > Ind1) {
        st = Ind1 + 1
    }
    name1out = CharArray(name1Tmp.size - st)
    var ind11 = 0
    for (i in st..end) {
        name1out[ind11++] = name1Tmp[i]
    }

    val name2Tmp = name2!!.copyOf()
    var Ind2 = 0
    var name2out: CharArray? = null
    while (name2Tmp.size > Ind2 && name2Tmp[Ind2] != ' ') Ind2++
    val end2 = name2Tmp.size - 1
    var st2 = 0
    if (name2Tmp.size > Ind2) {
        st2 = Ind2 + 1
    }
    name2out = CharArray(name2Tmp.size - st2)
    var ind21 = 0
    for (i in st2..end2) {
        name2out[ind21++] = name2Tmp[i]
    }

    //名前が一致してるか
    val len1 = name1out!!.size
    val len2 = name2out!!.size
    if (len1 != len2) return false
    var cnt = 0
    for (i in 0..len1 - 1) {
        if (name1out!![i] == name2out!![i]) cnt++
    }
    if (len1 == cnt) return true
    return false
}

fun nameComparison(name1: Array<Char?>, name2: String): Boolean {

    var ch1 = CharArray(name1.size)
    var ch2 = CharArray(name2.length)
    for (i in 0..name1.size - 1) ch1[i] = name1[i]!!.toChar()
    for (i in 0..name2.length - 1) ch2[i] = name2[i]!!.toChar()
    return nameComparison(ch1, ch2)
}

fun nameComparison(name1: CharArray?, name2: String): Boolean {

    val ch1 = name1!!.copyOf()
    var ch2 = CharArray(name2.length)
    for (i in 0..name2.length - 1) ch2[i] = name2[i]!!.toChar()
    return nameComparison(ch1, ch2)
}

fun nameComparison(name1: NameSet?, name2: String): Boolean {
    return nameComparison(name1!!.getName(), name2)
}