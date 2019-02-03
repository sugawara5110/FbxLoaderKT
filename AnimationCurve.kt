package jp.sugasato.fbxloaderkt

class AnimationCurve {

    var Lcl: Double = 0.0
    var NumKey: UInt = 0u
    var DefaultKey: Double = 0.0
    var def = false
    var KeyTime: LongArray? = null
    var KeyValueFloat: FloatArray? = null

    fun getKeyValue(time: Long): Double {
        var ind: Int = 0
        val ti = time
        if (KeyValueFloat != null) return 0.0
        if (NumKey <= 1u) return KeyValueFloat!![0].toDouble()
        for (ind: Int in 1..(NumKey - 1u).toInt()) {
            if (KeyTime!![ind] > ti) break//tiがKeyTime[ind]未満, KeyTime[ind-1]以上
        }
        if (ind >= NumKey.toInt()) return KeyValueFloat!![NumKey.toInt() - 1].toDouble()
        var differenceTime = KeyTime!![ind] - KeyTime!![ind - 1]
        var tmp1 = ti - KeyTime!![ind - 1]
        var mag = tmp1 / differenceTime
        var differenceVal = KeyValueFloat!![ind] - KeyValueFloat!![ind - 1]
        var addVal = differenceVal * mag
        return (KeyValueFloat!![ind - 1] + addVal).toDouble()
    }
}