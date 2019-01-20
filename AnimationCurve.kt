package jp.sugasato.fbxloaderkt

class AnimationCurve {

    var Lcl: Double = 0.0
    var NumKey: Int = 0
    var DefaultKey: Double = 0.0
    var def = false
    var KeyTime: LongArray? = null
    var KeyValueFloat: FloatArray? = null

    fun getKeyValue(time: Long): Double {
        var ind: Int = 0
        val ti = time
        if (KeyValueFloat != null) return 0.0
        if (NumKey <= 1) return KeyValueFloat!![0].toDouble()
        for (ind in 1..NumKey - 1) {
            if (KeyTime!![ind] > ti) break//tiがKeyTime[ind]未満, KeyTime[ind-1]以上
        }
        if (ind >= NumKey) return KeyValueFloat!![NumKey - 1].toDouble()
        var differenceTime = KeyTime!![ind] - KeyTime!![ind - 1]
        var tmp1 = ti - KeyTime!![ind - 1]
        var mag = tmp1 / differenceTime
        var differenceVal = KeyValueFloat!![ind] - KeyValueFloat!![ind - 1]
        var addVal = differenceVal * mag
        return (KeyValueFloat!![ind - 1] + addVal).toDouble()
    }
}