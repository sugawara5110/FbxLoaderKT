package jp.sugasato.fbxloaderkt

class AnimationCurve {

    var Lcl: Double = 0.0
    var NumKey: UInt = 0u
    var DefaultKey: Double = 0.0
    var def = false
    var KeyTime: LongArray? = null
    var KeyValueFloat: FloatArray? = null

    fun getKeyValue(time: Long): Double {
        val ti = time
        if (KeyValueFloat == null) return 0.0
        if (NumKey <= 1u) return KeyValueFloat!![0].toDouble()
        var Ind: Int = 1
        while (Ind < NumKey.toInt()) {
            if (KeyTime!![Ind] > ti) break//tiがKeyTime[ind]未満, KeyTime[ind-1]以上
            Ind++
        }
        if (Ind >= NumKey.toInt()) return KeyValueFloat!![NumKey.toInt() - 1].toDouble()
        val differenceTime = KeyTime!![Ind] - KeyTime!![Ind - 1]
        val tmp1 = ti - KeyTime!![Ind - 1]
        val mag = tmp1 / differenceTime
        val differenceVal = KeyValueFloat!![Ind] - KeyValueFloat!![Ind - 1]
        val addVal = differenceVal * mag
        return (KeyValueFloat!![Ind - 1] + addVal).toDouble()
    }
}