package jp.sugasato.fbxloaderkt

class ConnectionNo {

    var ConnectionID: Long = -1
    var ConnectionIDPointer: NodeRecord? = null
}

class ConnectionList {

    var ChildID: Long = -1
    var ParentID: Long = -1
}

class NodeRecord {

    val NUMNODENAME = 10
    //全てリトルエンディアン
    var EndOffset: UInt = 0u//次のファイルの先頭バイト数
    var NumProperties: Int = 0//プロパティの数
    var PropertyListLen: Int = 0//プロパティリストの大きさ(byte)
    var classNameLen = 0
    var className: CharArray? = null
    var Property: UByteArray? = null//(型type, そのdataの順で並んでる) * プロパティの数

    var nodeName: Array<CharArray?> = arrayOfNulls(NUMNODENAME)
    var NumChildren: Int = 0
    var nodeChildren: Array<NodeRecord?>? = null//{}内のノード, NodeRecord配列用

    var thisConnectionID: Long = -1
    var connectionNode: ArrayList<NodeRecord?> = arrayListOf() //NodeRecord接続用(.add(要素)で追加)

    fun searchName_Type(cn: ArrayList<ConnectionNo?>) {
        var swt = 0
        var ln = 0
        var nameNo = 0
        var addInd = 0
        var loop = 0
        while (loop < PropertyListLen) {
            when (swt) {
                3 -> {
                    //Lの処理
                    val cName: String = className.toString()
                    val st = classNameLen - 4
                    val end = cName.lastIndex
                    val sch: CharSequence = cName.subSequence(st, end)
                    val CName: String = sch.toString()
                    if (0 == CName.compareTo("Time")) {
                        loop += 7
                        swt = 0
                    } else {
                        if (thisConnectionID == -1L && loop == 1) {//ConnectionIDは一個目のProperty
                            thisConnectionID = convertUCHARtoint64(Property, loop)
                        }
                        loop += 7
                        swt = 0
                    }
                }
                2 -> {
                    for (i in 0..ln - 1) {
                        nodeName[nameNo]!!.set(i, Property!![loop + i].toByte().toChar())
                    }
                    nameNo++
                    if (nameNo >= NUMNODENAME) return
                    loop += ln - 1
                    swt = 0
                }
                1 -> {
                    ln = (Property!![loop + 3].toInt() shl 24) or (Property!![loop + 2].toInt() shl 16) or
                            (Property!![loop + 1].toInt() shl 8) or (Property!![loop].toInt())
                    if (ln > 0) {
                        nodeName[nameNo] = CharArray(ln + 1)
                        loop += 3
                        swt = 2
                    } else {
                        loop += 3
                        swt = 0
                    }
                }
                0 -> {
                    if (Property!![loop].toByte().toChar() == 'S') {
                        swt = 1;
                    }
                    if (Property!![loop].toByte().toChar() == 'L') {
                        swt = 3;
                    }
                    val sw = Property!![loop].toByte().toChar()
                    if (sw == 'C') loop++;
                    if (sw == 'Y') loop += 2
                    if (sw == 'I' || sw == 'F') loop += 4
                    if (sw == 'D') loop += 8
                    //特殊型の場合は
                    //Lenght:  4byte
                    //Data:    Length byte
                    if (sw == 'R') {
                        loop += 4 + ((Property!![loop + 4].toInt() shl 24) or (Property!![loop + 3].toInt() shl 16) or
                                (Property!![loop + 2].toInt() shl 8) or (Property!![loop + 1].toInt()))
                    }
                    /*
                ArrayLenght:     4byte
                Encoding:        4byte
                CompressedLenght:4byte
                Contents         可変  CompressedLenghtにサイズが入ってる
                配列の場合これらのbyte列分スキップする
                Property配列は9byte分要素をずらしCompressedLenghtにアクセスしContentsサイズを取り出す
                取り出したサイズ+ 4 * 3バイト分"i"を進める
                */
                    if (sw == 'f' || sw == 'd' || sw == 'l' || sw == 'i' || sw == 'b') {
                        loop += 12 + ((Property!![loop + 3 + 9].toInt() shl 24) or
                                (Property!![loop + 2 + 9].toInt() shl 16) or
                                (Property!![loop + 1 + 9].toInt() shl 8) or
                                (Property!![loop + 9].toInt()))
                    }
                }
            }
            if (thisConnectionID != -1L) {
                var tmp: ConnectionNo = ConnectionNo()
                tmp.ConnectionID = thisConnectionID
                tmp.ConnectionIDPointer = this
                cn.add(tmp)
            }
            loop++
        }
    }

    fun createConnectionList(cnLi: ArrayList<ConnectionList?>) {
        var cl: ConnectionList = ConnectionList()
        //S len "OO" L 計8byteの次にChildID
        cl.ChildID = convertUCHARtoint64(Property, 8)
        //L 1byteの次にParentID
        cl.ParentID = convertUCHARtoint64(Property, 17)
        cnLi.add(cl)
    }

    fun set(fp: FilePointer, cn: ArrayList<ConnectionNo?>, cnLi: ArrayList<ConnectionList?>) {
        EndOffset = fp.convertBYTEtoUINT()
        NumProperties = fp.convertBYTEtoUINT().toInt()
        PropertyListLen = fp.convertBYTEtoUINT().toInt()
        classNameLen = fp.getByte().toInt()
        className = CharArray(classNameLen)
        for (i in 0..classNameLen - 1) {
            className!!.set(i, fp.getByte().toChar())
        }
        if (PropertyListLen > 0) {
            Property = UByteArray(PropertyListLen)
            for (i in 0..PropertyListLen - 1) {
                Property!!.set(i, fp.getByte().toUByte())
            }
            searchName_Type(cn);
            if (0 == className.toString().compareTo("C") &&
                (0 == nodeName[0].toString().compareTo("OO") ||
                        0 == nodeName[0].toString().compareTo("OP"))
            ) {
                createConnectionList(cnLi);
            }
        }

        val curpos = fp.getPos()
        //現在のファイルポインタがEndOffsetより手前,かつ
        //現ファイルポインタから4byteが全て0ではない場合, 子ノード有り
        if (EndOffset > curpos.toUInt() && fp.convertBYTEtoUINT() != 0u) {
            var topChildPointer = curpos.toUInt()
            var childEndOffset = 0u
            //子ノードEndOffsetをたどり,個数カウント
            do {
                fp.seekPointer(fp.getPos() - 4)//"convertBYTEtoUINT(fp) != 0"の分戻す
                NumChildren++;
                childEndOffset = fp.convertBYTEtoUINT()
                fp.seekPointer(childEndOffset.toInt())
            } while (EndOffset > childEndOffset && fp.convertBYTEtoUINT() != 0u)
            //カウントが終わったので最初の子ノードのファイルポインタに戻す
            fp.seekPointer(topChildPointer.toInt())
            nodeChildren = arrayOfNulls(NumChildren)
            for (i in 0..NumChildren - 1) {
                nodeChildren!![i] = NodeRecord()
                nodeChildren!![i]!!.set(fp, cn, cnLi)
            }
        }
        //読み込みが終了したのでEndOffsetへポインタ移動
        fp.seekPointer(EndOffset.toInt())
    }
}