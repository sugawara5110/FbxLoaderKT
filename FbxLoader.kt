package jp.sugasato.fbxloaderkt

class FbxLoader {

    val Kaydara_FBX_binary = 18
    var fp: FilePointer = FilePointer()
    var version = 0//23から26バイトまで4バイト分符号なし整数,リトルエンディアン(下から読む)
    var FbxRecord: NodeRecord = NodeRecord()//ファイルそのまま
    var rootNode: NodeRecord? = null//ConnectionID:0のポインタ
    var cnNo: ArrayList<ConnectionNo?> = arrayListOf()
    var cnLi: ArrayList<ConnectionList?> = arrayListOf()
    var NumMesh = 0
    var Mesh: Array<FbxMeshNode?>? = null
    var NumDeformer = 0
    var deformer: Array<Deformer?> = arrayOfNulls(256)//デフォーマーのみのファイル対応
    var rootDeformer: Deformer? = null

    fun fileCheck(): Boolean {
        val str1: String = "Kaydara FBX binary"
        val str2: CharArray = str1.toCharArray()

        var missCnt = 0
        var strCnt = 0
        for (i in 0..Kaydara_FBX_binary + 1) {
            if (fp.getByte() != str2[strCnt].toByte()) {
                missCnt++;
                if (missCnt > 3) {
                    //ソフトによってスペルミスが有るのでとりあえず3文字以上異なる場合falseにする
                    //別ファイルの場合ほぼ全数当てはまらないはず
                    return false
                }
            }
            strCnt++
        }
        //0-20バイト 『Kaydara FBX binary  [null]』
        //21-22バイト 0x1a, 0x00
        //23-26バイトまで(4バイト分): 符号なし整数,バージョンを表す
        fp.seekPointer(23)
        return true
    }

    fun searchVersion() {
        //バージョンは23-26バイトまで(4バイト分)リトルエンディアン(下の位から読んでいく)
        version = fp.convertBYTEtoUINT().toInt()
    }

    fun readFBX() {
        var curpos = fp.getPos()
        var nodeCount = 0
        var endoffset = 0

        while (fp.convertBYTEtoUINT().toInt() != 0) {

            fp.seekPointer(fp.getPos() - 4)//"convertBYTEtoUINT() != 0"の分戻す
            nodeCount++
            endoffset = fp.convertBYTEtoUINT().toInt()
            fp.seekPointer(endoffset)
        }
        fp.seekPointer(curpos)

        FbxRecord.classNameLen = 9
        var frName = "FbxRecord"
        FbxRecord.className = frName.toCharArray()
        FbxRecord.NumChildren = nodeCount
        FbxRecord.nodeChildren = arrayOfNulls(nodeCount)

        for (i in 0..nodeCount - 1) {
            FbxRecord.nodeChildren!![i] = NodeRecord()
            FbxRecord.nodeChildren!![i]!!.set(fp, cnNo, cnLi)
        }

        //ノードIDの通りにノードを繋げる
        for (i in 0..cnLi.size - 1) {
            for (j in 0..cnNo.size - 1) {
                if (cnLi[i]!!.ParentID == cnNo[j]!!.ConnectionID) {//親ノードのポインタ検索
                    for (j1 in 0..cnNo.size) {
                        if (cnLi[i]!!.ChildID == cnNo[j1]!!.ConnectionID) {//子ノードのポインタ検索
                            //親ノード内connectionNodeへ子ノードポインタ追加
                            cnNo[j]!!.ConnectionIDPointer!!.connectionNode.add(cnNo[j1]!!.ConnectionIDPointer)
                        }
                    }
                }
            }
        }
        //rootNode設定
        for (i in 0..cnNo.size - 1) {
            if (cnNo[i]!!.ConnectionID == 0L) {
                rootNode = cnNo[i]!!.ConnectionIDPointer;
                break;
            }
        }
    }

    fun Decompress(node: NodeRecord?, typeSize: Int): Triple<Boolean, UByteArray, Int> {
        //型1byte, 配列数4byte, 圧縮有無4byte, サイズ4byte, メタdata2byte 計15byte後data
        val comp = convertUCHARtoUINT(node!!.Property, 5)//圧縮有無
        var meta = 0u
        if (comp == 1u) meta = 2u
        var inSize = (convertUCHARtoUINT(node!!.Property, 9) - meta).toInt()//メタdata分引く
        if (inSize <= 0) {
            val nullRet = UByteArray(1)
            return Triple(false, nullRet, 0)
        }
        var outSize = convertUCHARtoUINT(node!!.Property, 1).toInt()
        var output = UByteArray(outSize * typeSize)
        if (comp == 1u) {
            var propertyData = UByteArray(inSize.toInt())
            for (i in 0..inSize - 1) {
                propertyData[i] = node!!.Property!![15 + i].toByte().toUByte()
            }
            var dd: DecompressDeflate = DecompressDeflate()
            output = dd.getDecompressArray(propertyData, inSize, outSize)//解凍
        } else {
            for (i in 0..outSize * typeSize - 1) {
                output[i] = node!!.Property!![13 + i].toByte().toUByte()//解凍無しの場合メタdata2byte無し
            }
        }
        return Triple(true, output, outSize)
    }

    fun getLayerElementSub(node: NodeRecord, le: LayerElement?) {

        for (i in 0..node.NumChildren - 1) {
            var n1 = node.nodeChildren!![i]
            if (0 == n1!!.className.toString().compareTo("Name")) {
                val size = convertUCHARtoUINT(n1.Property, 1).toInt()
                if (size > 0) {
                    le!!.name = CharArray(size)
                    for (i1 in 0..size - 1) {
                        le!!.name!![i1] = n1!!.Property!![5 + i1].toByte().toChar()
                    }
                }
            }

            if (0 == n1!!.className.toString().compareTo("MappingInformationType")) {
                val size = convertUCHARtoUINT(n1.Property, 1).toInt()
                if (size > 0) {
                    le!!.MappingInformationType = CharArray(size)
                    for (i1 in 0..size - 1) {
                        le!!.MappingInformationType!![i] = n1.Property!![5 + i1].toByte().toChar()
                    }
                }
            }

            if (0 == n1!!.className.toString().compareTo("Materials")) {
                val out = Decompress(n1, 4)
                le!!.Nummaterialarr = out.third
                le!!.materials = IntArray(out.third)
                le!!.materials = ConvertUCHARtoINT32(out.second, 0, out.third)
            }

            if (0 == n1!!.className.toString().compareTo("Normals")) {
                val out = Decompress(n1, 8)
                le!!.Numnormals = out.third
                le!!.normals = DoubleArray(out.third)
                le!!.normals = ConvertUCHARtoDouble(out.second, 0, out.third)
            }

            if (0 == n1!!.className.toString().compareTo("UV")) {
                val out = Decompress(n1, 8)
                le!!.NumUV = out.third
                le!!.UV = DoubleArray(out.third)
                le!!.UV = ConvertUCHARtoDouble(out.second, 0, out.third)
            }

            if (0 == n1!!.className.toString().compareTo("UVIndex")) {
                val out = Decompress(n1, 4)
                le!!.NumUVindex = out.third
                le!!.UVindex = IntArray(out.third)
                le!!.UVindex = ConvertUCHARtoINT32(out.second, 0, out.third)
            }
        }
    }

    fun getLayerElement(node: NodeRecord, mesh: FbxMeshNode) {
        if (0 == node!!.className.toString().compareTo("LayerElementMaterial")) {
            val No = convertUCHARtoINT32(node!!.Property, 1)//たぶんレイヤーNo取得
            mesh!!.Material[No] = LayerElement()
            var mat = mesh!!.Material[No]
            val Numl = No + 1
            if (Numl > mesh!!.NumMaterial) mesh!!.NumMaterial = Numl
            getLayerElementSub(node, mat)
        }
        if (0 == node!!.className.toString().compareTo("LayerElementNormal")) {
            val No = convertUCHARtoINT32(node!!.Property, 1)
            mesh!!.Normals[No] = LayerElement()
            var nor = mesh!!.Normals[No]
            getLayerElementSub(node, nor)
        }
        if (0 == node!!.className.toString().compareTo("LayerElementUV")) {
            val No = convertUCHARtoINT32(node!!.Property, 1)
            mesh!!.UV[No] = LayerElement()
            var uv = mesh!!.UV[No]
            getLayerElementSub(node, uv)
        }
    }

    fun nameComparison(name1: CharArray?, name2: CharArray?): Boolean {
        //名前文字列に空白文字が有る場合,空白文字以前を取り除く
        var name1Tmp = name1!!.copyOf()
        var Ind1 = 0
        var name1out: CharArray? = null
        do {
            while (name1Tmp[Ind1] != ' ' && name1Tmp.size > Ind1) {
                Ind1++;
            }
            if (name1Tmp.size == Ind1) {
                break
            } else {
                Ind1++;
                val cs: CharSequence = name1Tmp.toString().subSequence(Ind1, name1Tmp.toString().lastIndex)
                name1out = cs.toString().toCharArray()
            }
        } while (true)

        var name2Tmp = name2!!.copyOf()
        var Ind2 = 0
        var name2out: CharArray? = null
        do {
            while (name2Tmp[Ind2] != ' ' && name2Tmp.size > Ind2) {
                Ind2++
            }
            if (name2Tmp.size == Ind2) {
                break;
            } else {
                Ind2++;
                val cs: CharSequence = name2Tmp.toString().subSequence(Ind2, name2Tmp.toString().lastIndex)
                name2out = cs.toString().toCharArray()
            }
        } while (true)

        //名前が一致してるか
        val len1 = name1out!!.size
        val len2 = name2out!!.size
        if (len1 == len2 && 0 == name1.toString().compareTo(name2.toString())) return true
        return false
    }

    fun setParentPointerOfSubDeformer(mesh: FbxMeshNode?) {
        for (i in 0..mesh!!.NumDeformer + 1) {
            var defo: Deformer? = null
            if (i < mesh!!.NumDeformer) {
                defo = mesh!!.deformer[i]
            } else
                defo = mesh!!.rootDeformer

            for (i1 in 0..defo!!.NumChild - 1) {
                for (i2 in 0..mesh!!.NumDeformer - 1) {
                    //登録した子Deformer名と一致するDeformerに自身のポインタを登録
                    if (nameComparison(defo!!.childName[i1], mesh!!.deformer[i2]!!.thisName)) {
                        mesh!!.deformer[i2]!!.parentNode = defo
                    }
                }
            }
        }
    }

    fun getSubDeformer(node: NodeRecord?, mesh: FbxMeshNode?) {
        //各Deformer情報取得
        if (0 == node!!.className.toString().compareTo("Deformer")) {
            mesh!!.deformer[mesh!!.NumDeformer] = Deformer()
            var defo = mesh!!.deformer[mesh!!.NumDeformer]
            //Deformer名
            val len = node!!.nodeName!![0]!!.size
            defo!!.thisName = CharArray(len)
            defo!!.thisName = node!!.nodeName!![0]!!.copyOf()

            for (i in 0..node!!.connectionNode.size - 1) {
                var n1 = node!!.connectionNode[i]
                if (0 == n1!!.className.toString().compareTo("Model")) {//自身のModel
                    getAnimation(n1, defo)
                }
            }

            //子ノードName登録
            for (i in 0..node!!.connectionNode.size - 1) {
                var n1 = node!!.connectionNode[i]
                if (0 == n1!!.className.toString().compareTo("Model")) {//自身のModel
                    for (i1 in 0..n1!!.connectionNode.size) {
                        var n2 = n1!!.connectionNode[i1]
                        if (0 == n2!!.className.toString().compareTo("Model")) {//子ノードのModel
                            val ln = n2!!.nodeName!![0]!!.size
                            defo!!.childName[defo!!.NumChild] = CharArray(ln)
                            defo!!.childName[defo!!.NumChild++].toString().compareTo(n2!!.nodeName[0].toString())
                        }
                    }
                }
            }

            for (i in 0..node!!.NumChildren - 1) {
                var n1 = node!!.nodeChildren!![i]

                //インデックス配列,数
                if (0 == n1!!.className.toString().compareTo("Indexes")) {
                    val out = Decompress(n1, 4)
                    defo!!.IndicesCount = out.third
                    defo!!.Indices = IntArray(out.third)
                    defo!!.Indices = ConvertUCHARtoINT32(out.second, 0, out.third)
                }

                //ウエイト
                if (0 == n1!!.className.toString().compareTo("Weights")) {
                    val out = Decompress(n1, 8)
                    defo!!.Weights = DoubleArray(out.third)
                    defo!!.Weights = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //Transform
                if (0 == n1!!.className.toString().compareTo("Transform")) {
                    val out = Decompress(n1, 8)
                    defo!!.TransformMatrix = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //TransformLink
                if (0 == n1!!.className.toString().compareTo("TransformLink")) {
                    val out = Decompress(n1, 8)
                    defo!!.TransformLinkMatrix = ConvertUCHARtoDouble(out.second, 0, out.third)
                }
            }
            mesh!!.NumDeformer++//Deformer数カウント
        }
    }

    fun getDeformer(node: NodeRecord?, mesh: FbxMeshNode?) {
        if (0 == node!!.className.toString().compareTo("Deformer")) {
            for (i in 0..node!!.connectionNode.size - 1) {
                var n1 = node!!.connectionNode[i]
                //各Deformer情報取得
                getSubDeformer(n1, mesh)
            }
        }
    }

    fun getGeometry(node: NodeRecord?, mesh: FbxMeshNode?) {
        if (0 == node!!.className.toString().compareTo("Geometry")) {
            val len = node!!.nodeName[0]!!.size
            mesh!!.Name = CharArray(len)
            mesh!!.Name = node!!.nodeName[0]!!.copyOf()
            for (i in 0..node!!.NumChildren - 1) {
                var n1 = node!!.nodeChildren!![i]

                //頂点
                if (0 == n1!!.className.toString().compareTo("Vertices") && mesh!!.vertices == null) {
                    var out = Decompress(n1, 8)
                    mesh!!.NumVertices = out.third / 3;
                    mesh!!.vertices = DoubleArray(out.third)
                    mesh!!.vertices = ConvertUCHARtoDouble(out.second, 0, out.third)
                }

                //頂点インデックス
                if (0 == n1!!.className.toString().compareTo("PolygonVertexIndex") && mesh!!.polygonVertices == null) {
                    var out = Decompress(n1, 4)
                    mesh!!.NumPolygonVertices = out.third
                    mesh!!.polygonVertices = IntArray(out.third)
                    mesh!!.polygonVertices = ConvertUCHARtoINT32(out.second, 0, out.third)
                    for (i1 in 0..mesh!!.NumPolygonVertices - 1) {
                        if (mesh!!.polygonVertices!![i1] < 0) {
                            //ポリゴン毎の最終インデックスがbit反転されてるので
                            //そこでポリゴン数をカウント
                            mesh!!.NumPolygon++
                        }
                    }
                    mesh!!.PolygonSize = IntArray(mesh!!.NumPolygon)
                    var polCnt = 0
                    var PolygonSizeIndex = 0
                    for (i1 in 0..mesh!!.NumPolygonVertices - 1) {
                        polCnt++
                        if (mesh!!.polygonVertices!![i1] < 0) {
                            mesh!!.polygonVertices!![i1] = mesh!!.polygonVertices!![i1].inv()//bit反転
                            mesh!!.PolygonSize!![PolygonSizeIndex++] = polCnt
                            polCnt = 0;
                        }
                    }
                }

                //Normal, UV
                getLayerElement(n1, mesh)
            }

            for (i in 0..node!!.connectionNode.size - 1) {
                var n1 = node!!.connectionNode!![i]

                //ボーン関連
                getDeformer(n1, mesh)
            }
        }
    }

    fun getMaterial(node: NodeRecord?, mesh: FbxMeshNode?, materialindex: Int): Int {
        var materialIndex = materialindex
        if (0 == node!!.className.toString().compareTo("Material")) {
            mesh!!.material[materialIndex] = FbxMaterialNode()
            val len = node!!.nodeName[0]!!.size
            mesh!!.material[materialIndex]!!.MaterialName = CharArray(len)
            mesh!!.material[materialIndex]!!.MaterialName = node!!.nodeName[0]!!.copyOf()

            for (i in 0..node!!.NumChildren - 1) {
                if (0 == node!!.nodeChildren!![i]!!.className.toString().compareTo("Properties70")) {
                    var pro70 = node!!.nodeChildren!![i]
                    for (i1 in 0..pro70!!.NumChildren - 1) {
                        mesh!!.material[materialIndex]!!.Diffuse = getCol(pro70!!.nodeChildren!![i1], "DiffuseColor")
                        mesh!!.material[materialIndex]!!.Specular = getCol(pro70!!.nodeChildren!![i1], "SpecularColor")
                        mesh!!.material[materialIndex]!!.Ambient = getCol(pro70!!.nodeChildren!![i1], "AmbientColor")
                    }
                }
            }

            for (i in 0..node!!.connectionNode.size - 1) {
                if (0 == node!!.connectionNode!![i]!!.className.toString().compareTo("Texture")) {
                    var tex = node!!.connectionNode!![i]
                    var texTypeDiff = true
                    for (i1 in 0..tex!!.NumChildren - 1) {
                        if (0 == tex!!.nodeChildren!![i1]!!.className.toString()!!.compareTo("TextureName")) {
                            var texname = tex!!.nodeChildren!![i1]!!.nodeName[0]
                            val normal_Len = 6
                            val tName: CharSequence = texname.toString()
                                .subSequence(texname!!.size - normal_Len, texname.toString().lastIndex)
                            val tNames = tName.toString()
                            if (0 == tNames.compareTo("normal")) texTypeDiff = false
                        }
                    }
                    for (i1 in 0..tex!!.NumChildren - 1) {
                        if (0 == tex!!.nodeChildren!![i1]!!.className.toString().compareTo("FileName")) {
                            var texN = tex!!.nodeChildren!![i1]
                            val len = texN!!.nodeName[0]!!.size
                            if (texTypeDiff) {
                                if (null == mesh!!.material[materialIndex]!!.textureDifName) {
                                    mesh!!.material[materialIndex]!!.textureDifName = CharArray(len)
                                    mesh!!.material[materialIndex]!!.textureDifName = texN!!.nodeName[0]!!.copyOf()
                                }
                            } else {
                                if (null == mesh!!.material[materialIndex]!!.textureNorName) {
                                    mesh!!.material[materialIndex]!!.textureNorName = CharArray(len)
                                    mesh!!.material[materialIndex]!!.textureNorName = texN!!.nodeName[0]!!.copyOf()
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

    fun getMesh() {
        for (i in 0..rootNode!!.connectionNode!!.size - 1) {
            if (0 == rootNode!!.connectionNode!![i]!!.className.toString().compareTo("Model") &&
                0 == rootNode!!.connectionNode!![i]!!.nodeName[1].toString().compareTo("Mesh")
            ) {
                NumMesh++
            }
        }
        if (NumMesh <= 0) return
        Mesh = arrayOfNulls(NumMesh)
        for (i in 0..NumMesh - 1) Mesh!![i] = FbxMeshNode()

        var mecnt = 0
        var matcnt = 0
        for (i in 0..rootNode!!.connectionNode!!.size - 1) {
            var n1 = rootNode!!.connectionNode!![i]
            if (0 == n1!!.className.toString().compareTo("Model") &&
                0 == n1!!.nodeName!![1].toString().compareTo("Mesh")
            ) {
                for (i1 in 0..n1!!.connectionNode!!.size - 1) {
                    var n2 = n1!!.connectionNode!![i1]
                    getGeometry(n2, Mesh!![mecnt])
                    matcnt = getMaterial(n2, Mesh!![mecnt], matcnt)
                }
                mecnt++;
                matcnt = 0
            }
        }

        //rootBone生成, name登録(本来Deformerじゃないので別に生成)
        for (i in 0..rootNode!!.connectionNode!!.size - 1) {
            var n1 = rootNode!!.connectionNode!![i]
            if (0 == n1!!.className.toString().compareTo("Model") && n1.nodeName!![1] != null) {
                if (0 == n1!!.nodeName!![1].toString().compareTo("Root") ||
                    0 == n1!!.nodeName!![1].toString().compareTo("Limb")
                ) {
                    for (j in 0..NumMesh - 1) {
                        Mesh!![j]!!.rootDeformer = Deformer()
                        var defo = Mesh!![j]!!.rootDeformer
                        val len = n1!!.nodeName[0]!!.size
                        defo!!.thisName = CharArray(len)
                        defo!!.thisName = n1!!.nodeName[0]!!.copyOf()
                        getAnimation(n1, defo)
                        //子ノードのModelName登録
                        for (i1 in 0..n1!!.connectionNode!!.size - 1) {
                            var n2 = n1!!.connectionNode[i1]
                            if (0 == n2!!.className.toString().compareTo("Model")) {
                                val ln = n2!!.nodeName[0]!!.size
                                defo!!.childName[defo!!.NumChild] = CharArray(ln)
                                defo!!.childName[defo!!.NumChild++] = n2!!.nodeName[0]!!.copyOf()
                            }
                        }
                    }
                }
            }
        }

        //UV整列
        for (i in 0..NumMesh - 1) {
            for (i1 in 0..Mesh!![i]!!.NumMaterial - 1) {
                var uv = Mesh!![i]!!.UV[i1]
                uv!!.AlignedUV = DoubleArray(uv!!.NumUVindex * 2)
                var cnt = 0
                for (i2 in 0..uv!!.NumUVindex - 1) {
                    uv!!.AlignedUV!![cnt++] = uv!!.UV!![uv!!.UVindex!![i2] * 2]//UVindexはUVの2値を一組としてのインデックスなので×2で計算
                    uv!!.AlignedUV!![cnt++] = uv!!.UV!![uv!!.UVindex!![i2] * 2 + 1]
                }
            }
            setParentPointerOfSubDeformer(Mesh!![i])
        }
    }

    fun setParentPointerOfNoneMeshSubDeformer() {
        for (i in 0..NumDeformer) {
            var defo: Deformer? = null
            if (i < NumDeformer)
                defo = deformer[i]
            else
                defo = rootDeformer

            for (i1 in 0..defo!!.NumChild - 1) {
                for (i2 in 0..NumDeformer - 1) {
                    //登録した子Deformer名と一致するDeformerに自身のポインタを登録
                    if (nameComparison(defo!!.childName[i1], deformer[i2]!!.thisName)) {
                        deformer[i2]!!.parentNode = defo
                    }
                }
            }
        }
    }

    fun getNoneMeshSubDeformer(node: NodeRecord?) {
        if (0 == node!!.className.toString().compareTo("Model")) {
            deformer[NumDeformer] = Deformer()
            var defo = deformer[NumDeformer]
            NumDeformer++
            val len = node!!.nodeName[0]!!.size
            defo!!.thisName = CharArray(len)
            defo!!.thisName = node!!.nodeName[0]!!.copyOf()
            getAnimation(node, defo)
            //子ノードのModelName登録
            for (i in 0..node!!.connectionNode!!.size - 1) {
                var n1 = node!!.connectionNode[i]
                if (0 == n1!!.className.toString().compareTo("Model")) {
                    val ln = n1!!.nodeName[0]!!.size
                    defo!!.childName[defo!!.NumChild] = CharArray(ln)
                    defo!!.childName[defo!!.NumChild++] = n1!!.nodeName[0]!!.copyOf()
                    getNoneMeshSubDeformer(n1)
                }
            }
        }
    }

    fun getNoneMeshDeformer() {
        for (i in 0..rootNode!!.connectionNode!!.size - 1) {
            var n1 = rootNode!!.connectionNode[i]
            if (0 == n1!!.className.toString().compareTo("Model") && n1!!.nodeName[1] != null) {
                if (0 == n1!!.nodeName[1].toString().compareTo("Root") || 0 == n1!!.nodeName[1].toString().compareTo("Limb")) {
                    rootDeformer = Deformer()
                    val len = n1!!.nodeName[0]!!.size
                    rootDeformer!!.thisName = CharArray(len)
                    rootDeformer!!.thisName = n1!!.nodeName[0]!!.copyOf()
                    getAnimation(n1, rootDeformer)
                    //子ノードのModelName登録
                    for (i1 in 0..n1!!.connectionNode!!.size - 1) {
                        var n2 = n1!!.connectionNode[i1]
                        if (0 == n2!!.className.toString().compareTo("Model")) {
                            val ln = n2!!.nodeName[0]!!.size
                            rootDeformer!!.childName[rootDeformer!!.NumChild] = CharArray(ln)
                            rootDeformer!!.childName[rootDeformer!!.NumChild++] == n2!!.nodeName[0]!!.copyOf()
                            //ついでに子ノードのDeformer生成
                            getNoneMeshSubDeformer(n2)
                        }
                    }
                }
            }
        }
        setParentPointerOfNoneMeshSubDeformer()
    }

    fun getCol(pro70Child: NodeRecord?, ColStr: String): DoubleArray {
        var out = DoubleArray(3, { 0.0 })
        if (0 == pro70Child!!.className.toString().compareTo("P") &&
            0 == pro70Child!!.nodeName[0].toString().compareTo(ColStr)
        ) {
            var proInd = 1u
            for (i in 0..3) {
                proInd += convertUCHARtoUINT(pro70Child!!.Property, proInd.toInt()) + 1u + 4u
            }
            for (i in 0..2) {
                var ou = DoubleArray(1, { 0.0 })
                ou = ConvertUCHARtoDouble(pro70Child!!.Property, proInd.toInt(), 1)
                out[i] = ou[0]
                proInd += 9u
            }
        }
        return out
    }

    fun getLcl(pro70Child: NodeRecord?, anim: Array<AnimationCurve?>, LclStr: String) {
        if (0 == pro70Child!!.className.toString().compareTo("P") &&
            0 == pro70Child!!.nodeName[0].toString().compareTo(LclStr)
        ) {
            var proInd = 1u
            for (i in 0..3) {
                proInd += convertUCHARtoUINT(pro70Child!!.Property, proInd.toInt()) + 1u + 4u
            }
            for (i in 0..2) {
                var ou = DoubleArray(1, { 0.0 })
                ou = ConvertUCHARtoDouble(pro70Child!!.Property, proInd.toInt(), 1)
                anim[i]!!.Lcl = ou[0]
                proInd += 9u
            }
        }
    }

    fun getAnimationCurve(animNode: NodeRecord?, anim: Array<AnimationCurve?>, Lcl: String) {
        var animInd = 0u
        if (0 == animNode!!.className.toString().compareTo("AnimationCurveNode") &&
            0 == animNode!!.nodeName[0].toString().compareTo(Lcl)
        ) {
            for (i in 0..animNode!!.connectionNode!!.size - 1) {
                if (0 == animNode!!.connectionNode[i]!!.className.toString().compareTo("AnimationCurve")) {
                    var animCurve = animNode!!.connectionNode[i]
                    for (i1 in 0..animCurve!!.NumChildren - 1) {
                        if (0 == animCurve!!.nodeChildren!![i1]!!.className.toString().compareTo("Default")) {
                            if (anim[animInd.toInt()]!!.def) continue
                            anim[animInd.toInt()]!!.DefaultKey =
                                    convertUCHARtoDouble(animCurve!!.nodeChildren!![i1]!!.Property, 1)
                            anim[animInd.toInt()]!!.def = true
                        }
                        if (0 == animCurve!!.nodeChildren!![i1]!!.className.toString().compareTo("KeyTime")) {
                            if (anim[animInd.toInt()]!!.KeyTime != null) continue
                            var out = Decompress(animCurve!!.nodeChildren!![i1], 8)
                            if (out.first) {
                                anim[animInd.toInt()]!!.NumKey = out.third
                                anim[animInd.toInt()]!!.KeyTime = LongArray(out.third)
                                anim[animInd.toInt()]!!.KeyTime = ConvertUCHARtoint64_t(out.second, 0, out.third)
                            }

                        }
                        if (0 == animCurve!!.nodeChildren!![i1]!!.className.toString().compareTo("KeyValueFloat")) {
                            if (anim[animInd.toInt()]!!.KeyValueFloat != null) continue
                            var out = Decompress(animCurve!!.nodeChildren!![i1], 4)
                            if (out.first) {
                                anim[animInd.toInt()]!!.NumKey = out.third
                                anim[animInd.toInt()]!!.KeyValueFloat = FloatArray(out.third)
                                anim[animInd.toInt()]!!.KeyValueFloat = ConvertUCHARtofloat(out.second, 0, out.third)
                            }
                            animInd++
                        }
                    }
                }
            }
        }
    }

    fun getAnimation(model: NodeRecord?, defo: Deformer?) {
        //Lcl Translation, Lcl Rotation, Lcl Scaling取得
        for (i in 0..model!!.NumChildren - 1) {
            if (0 == model!!.nodeChildren!![i]!!.className.toString().compareTo("Properties70")) {
                var pro70 = model!!.nodeChildren!![i]
                for (i1 in 0..pro70!!.NumChildren - 1) {
                    getLcl(pro70!!.nodeChildren!![i1], defo!!.Translation, "Lcl Translation")
                    getLcl(pro70!!.nodeChildren!![i1], defo!!.Rotation, "Lcl Rotation")
                    getLcl(pro70!!.nodeChildren!![i1], defo!!.Scaling, "Lcl Scaling")
                }
            }
        }
        //Animation関連
        for (i in 0..model!!.connectionNode.size - 1) {
            getAnimationCurve(model!!.connectionNode[i], defo!!.Translation, "T")
            getAnimationCurve(model!!.connectionNode[i], defo!!.Rotation, "R")
            getAnimationCurve(model!!.connectionNode[i], defo!!.Scaling, "S")
        }
    }

    fun setFbxFile(pass: String): Boolean {
        fp.setFile(pass)
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