package jp.sugasato.fbxloaderkt

class Deformer {

    //ツリー情報
    var thisName: NameSet = NameSet()//自身の名前
    var NumChild: Int = 0
    var childName: Array<NameSet> = Array<NameSet>(100, { i -> NameSet() })//子DeformerName
    var parentNode: Deformer? = null//EvaluateGlobalTransformの計算に使う

    var IndicesCount: Int = 0//このボーンに影響を受ける頂点インデックス数
    var Indices: IntArray? = null//このボーンに影響を受ける頂点のインデックス配列
    var Weights: DoubleArray? = null//このボーンに影響を受ける頂点のウエイト配列
    var TransformMatrix: DoubleArray = DoubleArray(16, { 0.0 })
    var TransformLinkMatrix: DoubleArray = DoubleArray(16, { 0.0 })//初期姿勢行列(絶対位置)
    var LocalPose: DoubleArray = DoubleArray(16, { 0.0 })
    var GlobalPose: DoubleArray = DoubleArray(16, { 0.0 })

    var Translation: Array<AnimationCurve> = Array<AnimationCurve>(3, { i -> AnimationCurve() })
    var Rotation: Array<AnimationCurve> = Array<AnimationCurve>(3, { i -> AnimationCurve() })
    var Scaling: Array<AnimationCurve> = Array<AnimationCurve>(3, { i -> AnimationCurve() })

    fun getName(): NameSet {
        return thisName
    }

    fun getIndicesCnt(): Int {
        return IndicesCount
    }

    fun GetIndices(): IntArray? {
        return Indices
    }

    fun GetWeights(): DoubleArray? {
        return Weights
    }

    fun getTransformLinkMatrix(y: Int, x: Int): Double {
        return TransformLinkMatrix[y * 4 + x]
    }

    fun getTimeFRAMES60(frame: Int): Long {
        return frame * 769769300L
    }

    fun getTimeFRAMES30(frame: Int): Long {
        return frame * 1539538600L
    }

    fun EvaluateLocalTransform(time: Long) {
        val sca = DoubleArray(16, { 0.0 })
        MatrixScaling(
            sca,
            Scaling[0].getKeyValue(time),
            Scaling[1].getKeyValue(time),
            Scaling[2].getKeyValue(time)
        )
        val rotx = DoubleArray(16, { 0.0 })
        MatrixRotationX(rotx, Rotation[0].getKeyValue(time))
        val roty = DoubleArray(16, { 0.0 })
        MatrixRotationY(roty, Rotation[1].getKeyValue(time))
        val rotz = DoubleArray(16, { 0.0 })
        MatrixRotationZ(rotz, Rotation[2].getKeyValue(time))
        val mov = DoubleArray(16, { 0.0 })
        MatrixTranslation(
            mov,
            Translation[0].getKeyValue(time),
            Translation[1].getKeyValue(time),
            Translation[2].getKeyValue(time)
        )

        val rotxy = DoubleArray(16, { 0.0 })
        MatrixMultiply(rotxy, rotx, roty)
        val rotxyz = DoubleArray(16, { 0.0 })
        MatrixMultiply(rotxyz, rotxy, rotz)

        val scrot = DoubleArray(16, { 0.0 })
        MatrixMultiply(scrot, sca, rotxyz)
        MatrixMultiply(LocalPose, scrot, mov)
    }

    private fun SubEvaluateGlobalTransform(time: Long): DoubleArray {
        EvaluateLocalTransform(time)
        if (parentNode != null) {
            //ルートノード以外
            val GlobalPosePare = parentNode!!.SubEvaluateGlobalTransform(time)
            MatrixMultiply(GlobalPose, LocalPose, GlobalPosePare)
            return GlobalPose
        }
        //ルートノード
        return LocalPose
    }

    fun EvaluateGlobalTransform(time: Long) {
        SubEvaluateGlobalTransform(time)
    }

    fun getEvaluateLocalTransform(y: Int, x: Int): Double {
        return LocalPose[y * 4 + x]
    }

    fun getEvaluateGlobalTransform(y: Int, x: Int): Double {
        return GlobalPose[y * 4 + x]
    }
}