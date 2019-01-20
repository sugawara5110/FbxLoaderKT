package jp.sugasato.fbxloaderkt

import kotlin.math.*

class Deformer {

    //ツリー情報
    var thisName: CharArray? = null//自身の名前
    var NumChild: Int = 0
    var childName: Array<CharArray?> = arrayOfNulls(100)//子DeformerName
    var parentNode: Deformer? = null//EvaluateGlobalTransformの計算に使う

    var IndicesCount: Int = 0//このボーンに影響を受ける頂点インデックス数
    var Indices: IntArray? = null//このボーンに影響を受ける頂点のインデックス配列
    var Weights: DoubleArray? = null//このボーンに影響を受ける頂点のウエイト配列
    var TransformMatrix: DoubleArray = DoubleArray(16, { 0.0 })
    var TransformLinkMatrix: DoubleArray = DoubleArray(16, { 0.0 })//初期姿勢行列(絶対位置)
    var LocalPose: DoubleArray = DoubleArray(16, { 0.0 })
    var GlobalPose: DoubleArray = DoubleArray(16, { 0.0 })

    var Translation: Array<AnimationCurve?> = arrayOfNulls(3)
    var Rotation: Array<AnimationCurve?> = arrayOfNulls(3)
    var Scaling: Array<AnimationCurve?> = arrayOfNulls(3)

    fun MatrixScaling(mat: DoubleArray, sx: Double, sy: Double, sz: Double) {
        mat[0] = sx; mat[1] = 0.0; mat[2] = 0.0; mat[3] = 0.0
        mat[4] = 0.0; mat[5] = sy; mat[6] = 0.0; mat[7] = 0.0
        mat[8] = 0.0; mat[9] = 0.0; mat[10] = sz; mat[11] = 0.0
        mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
    }

    fun MatrixRotationX(mat: DoubleArray, theta: Double) {
        var the = theta * 3.14 / 180.0
        mat[0] = 1.0; mat[1] = 0.0; mat[2] = 0.0; mat[3] = 0.0
        mat[4] = 0.0; mat[5] = cos(the); mat[6] = sin(the); mat[7] = 0.0
        mat[8] = 0.0; mat[9] = -sin(the); mat[10] = cos(the); mat[11] = 0.0
        mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
    }

    fun MatrixRotationY(mat: DoubleArray, theta: Double) {
        var the = theta * 3.14 / 180.0
        mat[0] = cos(the); mat[1] = 0.0; mat[2] = -sin(the); mat[3] = 0.0
        mat[4] = 0.0; mat[5] = 1.0; mat[6] = 0.0; mat[7] = 0.0
        mat[8] = sin(the); mat[9] = 0.0; mat[10] = cos(the); mat[11] = 0.0
        mat[12] = 0.0; mat[13] = 0.0; mat[14] = 0.0; mat[15] = 1.0
    }

    fun MatrixRotationZ(mat: DoubleArray, theta: Double) {
        var the = theta * 3.14 / 180.0
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

    fun getName(): CharArray? {
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
        var sca = DoubleArray(16, { 0.0 })
        MatrixScaling(
            sca,
            Scaling[0]!!.getKeyValue(time),
            Scaling[1]!!.getKeyValue(time),
            Scaling[2]!!.getKeyValue(time)
        )
        var rotx = DoubleArray(16, { 0.0 })
        MatrixRotationX(rotx, Rotation[0]!!.getKeyValue(time))
        var roty = DoubleArray(16, { 0.0 })
        MatrixRotationY(roty, Rotation[1]!!.getKeyValue(time))
        var rotz = DoubleArray(16, { 0.0 })
        MatrixRotationZ(rotz, Rotation[2]!!.getKeyValue(time))
        var mov = DoubleArray(16, { 0.0 })
        MatrixTranslation(
            mov,
            Translation[0]!!.getKeyValue(time),
            Translation[1]!!.getKeyValue(time),
            Translation[2]!!.getKeyValue(time)
        )

        var rotxy = DoubleArray(16, { 0.0 })
        MatrixMultiply(rotxy, rotx, roty)
        var rotxyz = DoubleArray(16, { 0.0 })
        MatrixMultiply(rotxyz, rotxy, rotz)

        var scrot = DoubleArray(16, { 0.0 })
        MatrixMultiply(scrot, sca, rotxyz)
        MatrixMultiply(LocalPose, scrot, mov)
    }

    fun SubEvaluateGlobalTransform(time: Long): DoubleArray {
        EvaluateLocalTransform(time)
        if (parentNode != null) {
            //ルートノード以外
            var GlobalPosePare = parentNode!!.SubEvaluateGlobalTransform(time)
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