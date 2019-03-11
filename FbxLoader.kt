package jp.sugasato.fbxloaderkt

import android.content.Context

class FbxLoader {

    private val Kaydara_FBX_binary = 18
    private var fp: FilePointer = FilePointer()
    private var version = 0//23から26バイトまで4バイト分符号なし整数,リトルエンディアン(下から読む)
    private var FbxRecord: NodeRecord = NodeRecord()//ファイルそのまま
    private var rootNode: NodeRecord? = null//ConnectionID:0のポインタ
    private var cnNo: ArrayList<ConnectionNo?> = arrayListOf()
    private var cnLi: ArrayList<ConnectionList?> = arrayListOf()
    private var NumMesh = 0
    private var Mesh: Array<FbxMeshNode?>? = null
    private var NumDeformer = 0
    private var deformer: Array<Deformer?> = arrayOfNulls(256)//デフォーマーのみのファイル対応
    private var rootDeformer: Deformer? = null

    private fun fileCheck(): Boolean {
        val str1: String = "Kaydara FBX binary"
        val str2: CharArray = str1.toCharArray()

        var missCnt = 0
        for (i in 0..Kaydara_FBX_binary - 1) {
            if (fp.getByte() != str2[i].toByte().toUByte()) {
                missCnt++;
                if (missCnt > 3) {
                    return false
                }
            }
        }
        //0-20バイト 『Kaydara FBX binary  [null]』
        //21-22バイト 0x1a, 0x00
        //23-26バイトまで(4バイト分): 符号なし整数,バージョンを表す
        fp.seekPointer(23u)
        return true
    }

    private fun searchVersion() {
        //バージョンは23-26バイトまで(4バイト分)リトルエンディアン(下の位から読んでいく)
        version = fp.convertBYTEtoUINT().toInt()
    }

    private fun readFBX() {
        val curpos = fp.getPos()
        var nodeCount = 0
        var endoffset = 0u
        while (fp.convertBYTEtoUINT() != 0u) {
            fp.seekPointer(fp.getPos() - 4u)//"convertBYTEtoUINT() != 0"の分戻す
            nodeCount++
            endoffset = fp.convertBYTEtoUINT()
            fp.seekPointer(endoffset)
        }
        fp.seekPointer(curpos)

        val classNameLen = 9
        FbxRecord.className.setSize(classNameLen)
        FbxRecord.className.setName("FbxRecord")
        FbxRecord.NumChildren = nodeCount
        FbxRecord.nodeChildren = arrayOfNulls(nodeCount)
        for (i in 0..nodeCount - 1) {
            FbxRecord.nodeChildren!![i] = NodeRecord()
            FbxRecord.nodeChildren!![i]!!.set(fp, cnNo, cnLi)
        }

        //ノードIDの通りにノードを繋げる
        for (i in 0..cnLi.size - 1) {
            for (j in 0..cnNo.size - 1) {
                if (cnLi[i]!!.ParentID == cnNo[j]!!.ConnectionID) {//親ノード検索
                    for (j1 in 0..cnNo.size - 1) {
                        if (cnLi[i]!!.ChildID == cnNo[j1]!!.ConnectionID) {//子ノード検索
                            //親ノード内connectionNodeへ子ノード追加
                            cnNo[j]!!.ConnectionIDPointer!!.connectionNode.add(cnNo[j1]!!.ConnectionIDPointer)
                        }
                    }
                }
            }
        }
        //rootNode設定
        for (i in 0..cnNo.size - 1) {
            if (cnNo[i]!!.ConnectionID == 0L) {
                rootNode = cnNo[i]!!.ConnectionIDPointer
                break;
            }
        }
    }

    private fun Decompress(node: NodeRecord?, typeSize: Int): Triple<Boolean, UByteArray, Int> {
        //型1byte, 配列数4byte, 圧縮有無4byte, サイズ4byte, メタdata2byte 計15byte後data
        val comp = convertUCHARtoUINT(node!!.Property, 5)//圧縮有無
        var meta = 0u
        if (comp == 1u) meta = 2u
        val inSize = (convertUCHARtoUINT(node.Property, 9) - meta).toInt()//メタdata分引く
        if (inSize <= 0) {
            val nullRet = UByteArray(1)
            return Triple(false, nullRet, 0)
        }
        val outSize = convertUCHARtoUINT(node.Property, 1).toInt()
        var output = UByteArray(outSize * typeSize)
        if (comp == 1u) {
            val propertyData = UByteArray(inSize.toInt())
            for (i in 0..inSize - 1) {
                propertyData[i] = node.Property!![15 + i].toByte().toUByte()
            }
            val dd: DecompressDeflate = DecompressDeflate()
            output = dd.getDecompressArray(propertyData, inSize, outSize * typeSize)//解凍
        } else {
            for (i in 0..outSize * typeSize - 1) {
                output[i] = node.Property!![13 + i].toByte().toUByte()//解凍無しの場合メタdata2byte無し
            }
        }
        return Triple(true, output, outSize)
    }

    private fun getLayerElementSub(node: NodeRecord, le: LayerElement?) {

        for (i in 0..node.NumChildren - 1) {
            val n1 = node.nodeChildren!![i]
            if (nameComparison(n1!!.className, "Name")) {
                val size = convertUCHARtoUINT(n1.Property, 1).toInt()
                if (size > 0) {
                    le!!.name.setSize(size)
                    le.name.setName(n1.Property, 5)
                }
            }

            if (nameComparison(n1.className, "MappingInformationType")) {
                val size = convertUCHARtoUINT(n1.Property, 1).toInt()
                if (size > 0) {
                    le!!.MappingInformationType.setSize(size)
                    le.MappingInformationType.setName(n1.Property, 5)
                }
            }

            if (nameComparison(n1.className, "Materials")) {
                val out = Decompress(n1, 4)
                le!!.Nummaterialarr = out.third
                le.materials = IntArray(out.third)
                le.materials = ConvertUCHARtoINT32(out.second, 0, out.third)
            }

            if (nameComparison(n1.className, "Normals")) {
                val out = Decompress(n1, 8)
                le!!.Numnormals = out.third
                le.normals = DoubleArray(out.third)
                le.normals = ConvertUCHARtoDouble(out.second, 0, out.third)
            }

            if (nameComparison(n1.className, "UV")) {
                val out = Decompress(n1, 8)
                le!!.NumUV = out.third
                le.UV = DoubleArray(out.third)
                le.UV = ConvertUCHARtoDouble(out.second, 0, out.third)
            }

            if (nameComparison(n1.className, "UVIndex")) {
                val out = Decompress(n1, 4)
                le!!.NumUVindex = out.third
                le.UVindex = IntArray(out.third)
                le.UVindex = ConvertUCHARtoINT32(out.second, 0, out.third)
            }
        }
    }

    private fun getLayerElement(node: NodeRecord, mesh: FbxMeshNode) {
        if (nameComparison(node.className, "LayerElementMaterial")) {
            val No = convertUCHARtoINT32(node.Property, 1)//たぶんレイヤーNo取得
            mesh.Material[No] = LayerElement()
            val mat = mesh.Material[No]
            val Numl = No + 1
            if (Numl > mesh.NumMaterial) mesh.NumMaterial = Numl
            getLayerElementSub(node, mat)
        }
        if (nameComparison(node.className, "LayerElementNormal")) {
            val No = convertUCHARtoINT32(node.Property, 1)
            mesh.Normals[No] = LayerElement()
            val nor = mesh.Normals[No]
            getLayerElementSub(node, nor)
        }
        if (nameComparison(node.className, "LayerElementUV")) {
            val No = convertUCHARtoINT32(node.Property, 1)
            mesh.UV[No] = LayerElement()
            val uv = mesh.UV[No]
            getLayerElementSub(node, uv)
        }
    }

    private fun setParentPointerOfSubDeformer(mesh: FbxMeshNode?) {
        for (i in 0..mesh!!.NumDeformer + 1) {
            var defo: Deformer? = null
            if (i < mesh.NumDeformer) {
                defo = mesh.deformer[i]
            } else
                defo = mesh.rootDeformer

            for (i1 in 0..defo!!.NumChild - 1) {
                for (i2 in 0..mesh.NumDeformer - 1) {
                    //登録した子Deformer名と一致するDeformerに自身のポインタを登録
                    if (nameComparison(
                            defo.childName[i1].getName(),
                            mesh.deformer[i2]!!.thisName.getName()
                        )
                    ) {
                        mesh.deformer[i2]!!.parentNode = defo
                    }
                }
            }
        }
    }

    private fun getSubDeformer(node: NodeRecord?, mesh: FbxMeshNode?) {
        //各Deformer情報取得
        if (nameComparison(node!!.className, "Deformer")) {
            mesh!!.deformer[mesh.NumDeformer] = Deformer()
            val defo = mesh.deformer[mesh.NumDeformer]
            //Deformer名
            val len = node.nodeName[0].getSize()
            defo!!.thisName.setSize(len)
            defo.thisName.setName(node.nodeName[0].getName(), 0)

            for (i in 0..node.connectionNode.size - 1) {
                val n1 = node.connectionNode[i]
                if (nameComparison(n1!!.className, "Model")) {//自身のModel
                    getAnimation(n1, defo)
                }
            }

            //子ノードName登録
            for (i in 0..node.connectionNode.size - 1) {
                val n1 = node.connectionNode[i]
                if (nameComparison(n1!!.className, "Model")) {//自身のModel
                    for (i1 in 0..n1.connectionNode.size - 1) {
                        val n2 = n1.connectionNode[i1]
                        if (nameComparison(n2!!.className, "Model")) {//子ノードのModel
                            val ln = n2.nodeName[0].getSize()
                            defo.childName[defo.NumChild].setSize(ln)
                            defo.childName[defo.NumChild++].setName(n2.nodeName[0].getName(), 0)
                        }
                    }
                }
            }

            for (i in 0..node.NumChildren - 1) {
                val n1 = node.nodeChildren!![i]

                //インデックス配列,数
                if (nameComparison(n1!!.className, "Indexes")) {
                    val out = Decompress(n1, 4)
                    defo.IndicesCount = out.third
                    defo.Indices = IntArray(out.third)
                    defo.Indices = ConvertUCHARtoINT32(out.second, 0, out.third)
                }

                //ウエイト
                if (nameComparison(n1.className, "Weights")) {
                    val out = Decompress(n1, 8)
                    defo.Weights = DoubleArray(out.third)
                    defo.Weights = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //Transform
                if (nameComparison(n1.className, "Transform")) {
                    val out = Decompress(n1, 8)
                    defo.TransformMatrix = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //TransformLink
                if (nameComparison(n1.className, "TransformLink")) {
                    val out = Decompress(n1, 8)
                    defo.TransformLinkMatrix = ConvertUCHARtoDouble(out.second, 0, out.third)
                }
            }
            mesh.NumDeformer++//Deformer数カウント
        }
    }

    private fun getDeformer(node: NodeRecord?, mesh: FbxMeshNode?) {
        if (nameComparison(node!!.className, "Deformer")) {
            for (i in 0..node.connectionNode.size - 1) {
                val n1 = node.connectionNode[i]
                //各Deformer情報取得
                getSubDeformer(n1, mesh)
            }
        }
    }

    private fun getGeometry(node: NodeRecord?, mesh: FbxMeshNode?) {
        if (nameComparison(node!!.className, "Geometry")) {
            val len = node.nodeName[0].getSize()
            mesh!!.Name.setSize(len)
            mesh.Name.setName(node.nodeName[0].getName(), 0)
            for (i in 0..node.NumChildren - 1) {
                val n1 = node.nodeChildren!![i]

                //頂点
                if (nameComparison(n1!!.className, "Vertices") && mesh.vertices == null) {
                    val out = Decompress(n1, 8)
                    mesh.NumVertices = out.third / 3
                    mesh.vertices = DoubleArray(out.third)
                    mesh.vertices = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //頂点インデックス
                if (nameComparison(n1.className, "PolygonVertexIndex") && mesh.polygonVertices == null) {
                    val out = Decompress(n1, 4)
                    mesh.NumPolygonVertices = out.third
                    mesh.polygonVertices = IntArray(out.third)
                    mesh.polygonVertices = ConvertUCHARtoINT32(out.second, 0, out.third)
                    for (i1 in 0..mesh.NumPolygonVertices - 1) {
                        if (mesh.polygonVertices!![i1] < 0) {
                            //ポリゴン毎の最終インデックスがbit反転されてるので
                            //そこでポリゴン数をカウント
                            mesh.NumPolygon++
                        }
                    }
                    mesh.PolygonSize = IntArray(mesh.NumPolygon)
                    var polCnt = 0
                    var PolygonSizeIndex = 0
                    for (i1 in 0..mesh.NumPolygonVertices - 1) {
                        polCnt++
                        if (mesh.polygonVertices!![i1] < 0) {
                            mesh.polygonVertices!![i1] = mesh.polygonVertices!![i1].inv()//bit反転
                            mesh.PolygonSize!![PolygonSizeIndex++] = polCnt
                            polCnt = 0
                        }
                    }
                }

                //Normal, UV
                getLayerElement(n1, mesh)
            }

            for (i in 0..node.connectionNode.size - 1) {
                val n1 = node.connectionNode[i]

                //ボーン関連
                getDeformer(n1, mesh)
            }
        }
    }

    private fun getMaterial(node: NodeRecord?, mesh: FbxMeshNode?, materialindex: Int): Int {
        var materialIndex = materialindex
        if (nameComparison(node!!.className, "Material")) {
            mesh!!.material[materialIndex] = FbxMaterialNode()
            val len = node.nodeName[0].getSize()
            mesh.material[materialIndex]!!.MaterialName.setSize(len)
            mesh.material[materialIndex]!!.MaterialName.setName(node.nodeName[0].getName(), 0)

            for (i in 0..node.NumChildren - 1) {
                if (nameComparison(node.nodeChildren!![i]!!.className, "Properties70")) {
                    val pro70 = node.nodeChildren!![i]
                    for (i1 in 0..pro70!!.NumChildren - 1) {
                        getCol(pro70.nodeChildren!![i1], mesh.material[materialIndex]!!.Diffuse, "DiffuseColor")
                        getCol(pro70.nodeChildren!![i1], mesh.material[materialIndex]!!.Specular, "SpecularColor")
                        getCol(pro70.nodeChildren!![i1], mesh.material[materialIndex]!!.Ambient, "AmbientColor")
                    }
                }
            }

            for (i in 0..node.connectionNode.size - 1) {
                if (nameComparison(node.connectionNode[i]!!.className, "Texture")) {
                    val tex = node.connectionNode[i]
                    var texTypeDiff = true
                    for (i1 in 0..tex!!.NumChildren - 1) {
                        if (nameComparison(tex.nodeChildren!![i1]!!.className, "TextureName")) {
                            val texname = tex.nodeChildren!![i1]!!.nodeName[0]
                            val normal_Len = 6
                            val end = texname.getSize() - 1
                            val st = end - (normal_Len - 1)
                            if (st >= 0) {
                                val tName = CharArray(normal_Len)
                                var tNameCnt = 0
                                for (i2 in st..end) {
                                    tName[tNameCnt++] = texname.getName()!![i2]
                                }
                                if (nameComparison(tName, "normal")) texTypeDiff = false
                            }
                        }
                    }
                    for (i1 in 0..tex.NumChildren - 1) {
                        if (nameComparison(tex.nodeChildren!![i1]!!.className, "FileName")) {
                            val texN = tex.nodeChildren!![i1]
                            val len = texN!!.nodeName[0].getSize()
                            if (texTypeDiff) {
                                if (null == mesh.material[materialIndex]!!.textureDifName.getName()) {
                                    mesh.material[materialIndex]!!.textureDifName.setSize(len)
                                    mesh.material[materialIndex]!!.textureDifName.setName(
                                        texN.nodeName[0].getName(),
                                        0
                                    )
                                }
                            } else {
                                if (null == mesh.material[materialIndex]!!.textureNorName.getName()) {
                                    mesh.material[materialIndex]!!.textureNorName.setSize(len)
                                    mesh.material[materialIndex]!!.textureNorName.setName(
                                        texN.nodeName[0].getName(),
                                        0
                                    )
                                }
                            }
                        }
                    }
                }
            }
            materialIndex++
        }
        return materialIndex
    }

    private fun getMesh() {
        for (i in 0..rootNode!!.connectionNode.size - 1) {
            if (nameComparison(rootNode!!.connectionNode[i]!!.className, "Model") &&
                nameComparison(rootNode!!.connectionNode[i]!!.nodeName[1], "Mesh")
            ) {
                NumMesh++
            }
        }
        if (NumMesh <= 0) return
        Mesh = arrayOfNulls(NumMesh)
        for (i in 0..NumMesh - 1) Mesh!![i] = FbxMeshNode()

        var mecnt = 0
        var matcnt = 0
        for (i in 0..rootNode!!.connectionNode.size - 1) {
            val n1 = rootNode!!.connectionNode[i]
            if (nameComparison(n1!!.className, "Model") &&
                nameComparison(n1.nodeName[1], "Mesh")
            ) {
                for (i1 in 0..n1.connectionNode.size - 1) {
                    val n2 = n1.connectionNode[i1]
                    getGeometry(n2, Mesh!![mecnt])
                    matcnt = getMaterial(n2, Mesh!![mecnt], matcnt)
                }
                mecnt++
                matcnt = 0
            }
        }

        //rootBone生成, name登録(本来Deformerじゃないので別に生成)
        for (i in 0..rootNode!!.connectionNode.size - 1) {
            val n1 = rootNode!!.connectionNode[i]
            if (nameComparison(n1!!.className, "Model") && n1.nodeName[1].getName() != null) {
                if (nameComparison(n1.nodeName[1], "Root") ||
                    nameComparison(n1.nodeName[1], "Limb")
                ) {
                    for (j in 0..NumMesh - 1) {
                        Mesh!![j]!!.rootDeformer = Deformer()
                        val defo = Mesh!![j]!!.rootDeformer
                        val len = n1.nodeName[0].getSize()
                        defo!!.thisName.setSize(len)
                        defo.thisName.setName(n1.nodeName[0].getName(), 0)
                        getAnimation(n1, defo)
                        //子ノードのModelName登録
                        for (i1 in 0..n1.connectionNode.size - 1) {
                            val n2 = n1.connectionNode[i1]
                            if (nameComparison(n2!!.className, "Model")) {
                                val ln = n2.nodeName[0].getSize()
                                defo.childName[defo.NumChild].setSize(ln)
                                defo.childName[defo.NumChild++].setName(n2.nodeName[0].getName(), 0)
                            }
                        }
                    }
                }
            }
        }

        //UV整列
        for (i in 0..NumMesh - 1) {
            for (i1 in 0..Mesh!![i]!!.NumMaterial - 1) {
                val uv = Mesh!![i]!!.UV[i1]
                uv!!.AlignedUV = DoubleArray(uv.NumUVindex * 2)
                var cnt = 0
                for (i2 in 0..uv.NumUVindex - 1) {
                    uv.AlignedUV!![cnt++] = uv.UV!![uv.UVindex!![i2] * 2]//UVindexはUVの2値を一組としてのインデックスなので×2で計算
                    uv.AlignedUV!![cnt++] = uv.UV!![uv.UVindex!![i2] * 2 + 1]
                }
            }
            setParentPointerOfSubDeformer(Mesh!![i])
        }
    }

    private fun setParentPointerOfNoneMeshSubDeformer() {
        for (i in 0..NumDeformer) {
            var defo: Deformer? = null
            if (i < NumDeformer)
                defo = deformer[i]
            else
                defo = rootDeformer

            for (i1 in 0..defo!!.NumChild - 1) {
                for (i2 in 0..NumDeformer - 1) {
                    //登録した子Deformer名と一致するDeformerに自身のポインタを登録
                    if (nameComparison(
                            defo.childName[i1].getName(),
                            deformer[i2]!!.thisName.getName()
                        )
                    ) {
                        deformer[i2]!!.parentNode = defo
                    }
                }
            }
        }
    }

    private fun getNoneMeshSubDeformer(node: NodeRecord?) {
        if (nameComparison(node!!.className, "Model")) {
            deformer[NumDeformer] = Deformer()
            val defo = deformer[NumDeformer]
            NumDeformer++
            val len = node.nodeName[0].getSize()
            defo!!.thisName.setSize(len)
            defo.thisName.setName(node.nodeName[0].getName(), 0)
            getAnimation(node, defo)
            //子ノードのModelName登録
            for (i in 0..node.connectionNode.size - 1) {
                val n1 = node.connectionNode[i]
                if (nameComparison(n1!!.className, "Model")) {
                    val ln = n1.nodeName[0].getSize()
                    defo.childName[defo.NumChild].setSize(ln)
                    defo.childName[defo.NumChild++].setName(n1.nodeName[0].getName(), 0)
                    getNoneMeshSubDeformer(n1)
                }
            }
        }
    }

    private fun getNoneMeshDeformer() {
        for (i in 0..rootNode!!.connectionNode.size - 1) {
            val n1 = rootNode!!.connectionNode[i]
            if (nameComparison(n1!!.className, "Model") && n1.nodeName[1].getName() != null) {
                if (nameComparison(
                        n1.nodeName[1],
                        "Root"
                    ) || nameComparison(n1.nodeName[1], "Limb")
                ) {
                    rootDeformer = Deformer()
                    val len = n1.nodeName[0].getSize()
                    rootDeformer!!.thisName.setSize(len)
                    rootDeformer!!.thisName.setName(n1.nodeName[0].getName(), 0)
                    getAnimation(n1, rootDeformer)
                    //子ノードのModelName登録
                    for (i1 in 0..n1.connectionNode.size - 1) {
                        val n2 = n1.connectionNode[i1]
                        if (nameComparison(n2!!.className, "Model")) {
                            val ln = n2.nodeName[0].getSize()
                            rootDeformer!!.childName[rootDeformer!!.NumChild].setSize(ln)
                            rootDeformer!!.childName[rootDeformer!!.NumChild++].setName(
                                n2.nodeName[0].getName(),
                                0
                            )
                            //ついでに子ノードのDeformer生成
                            getNoneMeshSubDeformer(n2)
                        }
                    }
                }
            }
        }
        setParentPointerOfNoneMeshSubDeformer()
    }

    private fun getCol(pro70Child: NodeRecord?, col: DoubleArray?, ColStr: String) {
        if (nameComparison(pro70Child!!.className, "P") &&
            nameComparison(pro70Child.nodeName[0], ColStr)
        ) {
            var proInd = 1u
            for (i in 0..3) {
                proInd += convertUCHARtoUINT(pro70Child.Property, proInd.toInt()) + 1u + 4u
            }
            for (i in 0..2) {
                var ou = DoubleArray(1, { 0.0 })
                ou = ConvertUCHARtoDouble(pro70Child.Property, proInd.toInt(), 1)
                col!![i] = ou[0]
                proInd += 9u
            }
        }
    }

    private fun getLcl(pro70Child: NodeRecord?, anim: Array<AnimationCurve>, LclStr: String) {
        if (nameComparison(pro70Child!!.className, "P") &&
            nameComparison(pro70Child.nodeName[0], LclStr)
        ) {
            var proInd = 1u
            for (i in 0..3) {
                proInd += convertUCHARtoUINT(pro70Child.Property, proInd.toInt()) + 1u + 4u
            }
            for (i in 0..2) {
                var ou = DoubleArray(1, { 0.0 })
                ou = ConvertUCHARtoDouble(pro70Child.Property, proInd.toInt(), 1)
                anim[i].Lcl = ou[0]
                proInd += 9u
            }
        }
    }

    private fun getAnimationCurve(animNode: NodeRecord?, anim: Array<AnimationCurve>, Lcl: String) {
        var animInd = 0u
        if (nameComparison(animNode!!.className, "AnimationCurveNode") &&
            nameComparison(animNode.nodeName[0], Lcl)
        ) {
            for (i in 0..animNode.connectionNode.size - 1) {
                if (nameComparison(animNode.connectionNode[i]!!.className, "AnimationCurve")) {
                    val animCurve = animNode.connectionNode[i]
                    for (i1 in 0..animCurve!!.NumChildren - 1) {
                        if (nameComparison(
                                animCurve.nodeChildren!![i1]!!.className,
                                "Default"
                            )
                        ) {
                            if (anim[animInd.toInt()].def) continue
                            anim[animInd.toInt()].DefaultKey =
                                convertUCHARtoDouble(
                                    animCurve.nodeChildren!![i1]!!.Property,
                                    1
                                )
                            anim[animInd.toInt()].def = true
                        }
                        if (nameComparison(
                                animCurve.nodeChildren!![i1]!!.className,
                                "KeyTime"
                            )
                        ) {
                            if (anim[animInd.toInt()].KeyTime != null) continue
                            val out = Decompress(animCurve.nodeChildren!![i1], 8)
                            if (out.first) {
                                anim[animInd.toInt()].NumKey = out.third.toUInt()
                                anim[animInd.toInt()].KeyTime = LongArray(out.third)
                                anim[animInd.toInt()].KeyTime =
                                    ConvertUCHARtoint64_t(out.second, 0, out.third)
                            }

                        }
                        if (nameComparison(
                                animCurve.nodeChildren!![i1]!!.className,
                                "KeyValueFloat"
                            )
                        ) {
                            if (anim[animInd.toInt()].KeyValueFloat != null) continue
                            val out = Decompress(animCurve.nodeChildren!![i1], 4)
                            if (out.first) {
                                anim[animInd.toInt()].NumKey = out.third.toUInt()
                                anim[animInd.toInt()].KeyValueFloat = FloatArray(out.third)
                                anim[animInd.toInt()].KeyValueFloat =
                                    ConvertUCHARtofloat(out.second, 0, out.third)
                            }
                            animInd++
                        }
                    }
                }
            }
        }
    }

    private fun getAnimation(model: NodeRecord?, defo: Deformer?) {
        //Lcl Translation, Lcl Rotation, Lcl Scaling取得
        for (i in 0..model!!.NumChildren - 1) {
            if (nameComparison(model.nodeChildren!![i]!!.className, "Properties70")) {
                val pro70 = model.nodeChildren!![i]
                for (i1 in 0..pro70!!.NumChildren - 1) {
                    getLcl(pro70.nodeChildren!![i1], defo!!.Translation, "Lcl Translation")
                    getLcl(pro70.nodeChildren!![i1], defo.Rotation, "Lcl Rotation")
                    getLcl(pro70.nodeChildren!![i1], defo.Scaling, "Lcl Scaling")
                }
            }
        }
        //Animation関連
        for (i in 0..model.connectionNode.size - 1) {
            getAnimationCurve(model.connectionNode[i], defo!!.Translation, "T")
            getAnimationCurve(model.connectionNode[i], defo.Rotation, "R")
            getAnimationCurve(model.connectionNode[i], defo.Scaling, "S")
        }
    }

    fun setFbxFile(con: Context, rawId: Int): Boolean {
        fp.setFile(con, rawId)
        if (fileCheck() == false) return false
        searchVersion()
        readFBX()
        getMesh()
        if (NumMesh <= 0) getNoneMeshDeformer()
        return true
    }

    fun GetFbxRecord(): NodeRecord {
        return FbxRecord
    }

    fun GetRootNode(): NodeRecord? {
        return rootNode
    }

    fun getNumFbxMeshNode(): Int {
        return NumMesh
    }

    fun getFbxMeshNode(index: Int): FbxMeshNode? {
        return Mesh!![index]
    }

    fun getNumNoneMeshDeformer(): Int {
        return NumDeformer
    }

    fun getNoneMeshDeformer(index: Int): Deformer? {
        return deformer[index]
    }

    fun GetVersion(): Int {
        return version
    }
}