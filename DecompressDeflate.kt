package jp.sugasato.fbxloaderkt

class DecompressDeflate {

    private val dest = shortArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    )

    private val NumBit = byteArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    )

    private val byteArrayNumbit = 8

    private var strSign = ShortArray(286, { 0 })//size286の配列を全要素0で初期化
    private var strNumSign = ByteArray(286, { 0 })
    private var lenSign = ShortArray(30, { 0 })
    private var lenNumSign = ByteArray(30, { 0 })

    private fun bitInversion(ba: ByteArray, size: Int): ByteArray {
        var baB = ba.copyOf()
        for (i: Int in 0..size - 1 step 1) {
            //shl 左シフト  shr 符号付き右シフト
            var baI: Int = baB[i].toInt()
            baI = ((baI and 0x55) shl 1) or ((baI and 0xAA) shr 1);//0x55:01010101, 0xAA:10101010
            baI = ((baI and 0x33) shl 2) or ((baI and 0xCC) shr 2);//0x33:00110011, 0xCC:11001100
            baI = (baI shl 4) or (baI shr 4);
            baB[i] = baI.toByte()
        }
        return baB
    }

    private fun getLength(value: Short): Pair<Short, Byte> {
        var va: Int = value.toInt()
        var len = 0
        var bitlen = 0
        if (257 <= va && va <= 264) {
            len = va - 254
        }
        if (265 <= va && va <= 268) {
            len = (va - 265) * 2 + 11
            bitlen = 1
        }
        if (269 <= va && va <= 272) {
            len = (va - 269) * 4 + 19
            bitlen = 2
        }
        if (273 <= va && va <= 276) {
            len = (va - 273) * 8 + 35
            bitlen = 3
        }
        if (277 <= va && va <= 280) {
            len = (va - 277) * 16 + 67
            bitlen = 4
        }
        if (281 <= va && va <= 284) {
            len = (va - 281) * 32 + 131
            bitlen = 5
        }
        if (285 == va) {
            len = 258
            bitlen = 0
        }
        return Pair(len.toShort(), bitlen.toByte())
    }

    private fun getDestLength(va: Short): Pair<Short, Byte> {
        val len = dest[va.toInt()]
        val bitlen = NumBit[va.toInt()]
        return Pair(len, bitlen)
    }

    private fun DecompressLZSS(
        outArray: ByteArray,
        outIndex: Int,
        MatchLen: Short,
        destLen: Short
    ): Pair<ByteArray, Int> {
        var srcInd = outIndex - destLen
        //一致長MatchLen > 自分からの距離destLenの場合一致長に達するまで同じとこを繰り返す
        for (i: Int in 0..MatchLen - 1 step 1) {
            var sind = i % destLen
            outArray[i + outIndex] = outArray[sind + srcInd]
        }
        val outInd = outIndex + MatchLen
        return Pair(outArray, outInd)
    }

    private fun getBit(CurSearchBit: Long, byteArray: ByteArray, NumBit: Byte, firstRight: Boolean): Pair<Long, Short> {
        var outBinArr: Short = 0
        var curSearchBit = CurSearchBit
        for (i: Int in 0..NumBit - 1 step 1) {
            val baind = curSearchBit / byteArrayNumbit//配列インデックス
            val searBit = curSearchBit % byteArrayNumbit//要素内bit位置インデックス
            val NumShift = byteArrayNumbit - 1 - searBit
            val popbit = (byteArray[baind.toInt()].toInt() shr NumShift.toInt()) and 0x01//目的bit取り出し, bit位置最右
            var NumShift16 = NumBit - 1 - i//符号用左から詰める
            if (firstRight) NumShift16 = i//右から詰める
            val posbit16 = popbit shl NumShift16
            outBinArr = (outBinArr.toInt() or posbit16.toInt()).toShort() //bit追加
            curSearchBit++
        }
        return Pair(curSearchBit, outBinArr)
    }

    private fun createFixedHuffmanSign() {
        for (i: Int in 0..286 - 1 step 1) {
            if (0 <= i && i <= 143) {
                strSign[i] = (i + 48).toShort()
                strNumSign[i] = 8
            }
            if (144 <= i && i <= 255) {
                strSign[i] = (i + 256).toShort()
                strNumSign[i] = 9;
            }
            if (256 <= i && i <= 279) {
                strSign[i] = (i - 256).toShort()
                strNumSign[i] = 7
            }
            if (280 <= i && i <= 287) {
                strSign[i] = (i - 88).toShort()
                strNumSign[i] = 8
            }
        }
        for (i: Int in 0..30 - 1 step 1) {
            lenSign[i] = i.toShort()
            lenNumSign[i] = 5
        }
    }

    private fun SortIndex(sortedIndex: ShortArray, hclens: ByteArray, size: Int): Pair<ShortArray, ByteArray> {
        val topSize = size * 0.5
        val halfSize = size - topSize
        var topSortedIndex = ShortArray(topSize.toInt())
        var halfSortedIndex = ShortArray(halfSize.toInt())
        var tophclens = ByteArray(topSize.toInt())
        var halfhclens = ByteArray(halfSize.toInt())

        for (i: Int in 0..topSize.toInt() - 1 step 1) {
            topSortedIndex[i] = sortedIndex[i]
            tophclens[i] = hclens[i]
        }
        for (i: Int in 0..halfSize.toInt() - 1 step 1) {
            halfSortedIndex[i] = sortedIndex[i + topSize.toInt()]
            halfhclens[i] = hclens[i + topSize.toInt()]
        }

        if (topSize > 1) {
            val pair = SortIndex(topSortedIndex, tophclens, topSize.toInt())
            topSortedIndex = pair.first.copyOf()
            tophclens = pair.second.copyOf()
        }
        if (halfSize > 1) {
            val pair = SortIndex(halfSortedIndex, halfhclens, halfSize.toInt())
            halfSortedIndex = pair.first.copyOf()
            halfhclens = pair.second.copyOf()
        }

        var topIndex = 0
        var halfIndex = 0
        var iInd = 0
        for (i: Int in 0..size - 1 step 1) {
            if (tophclens[topIndex] <= halfhclens[halfIndex]) {
                hclens[i] = tophclens[topIndex]
                sortedIndex[i] = topSortedIndex[topIndex]
                topIndex++
                if (topSize <= topIndex) {
                    iInd = i + 1
                    break
                }
            } else {
                hclens[i] = halfhclens[halfIndex]
                sortedIndex[i] = halfSortedIndex[halfIndex]
                halfIndex++
                if (halfSize <= halfIndex) {
                    iInd = i + 1
                    break
                }
            }
        }
        if (topSize > topIndex) {
            for (i: Int in iInd..size - 1 step 1) {
                hclens[i] = tophclens[topIndex]
                sortedIndex[i] = topSortedIndex[topIndex]
                topIndex++
            }
        }
        if (halfSize > halfIndex) {
            for (i: Int in iInd..size - 1 step 1) {
                hclens[i] = halfhclens[halfIndex]
                sortedIndex[i] = halfSortedIndex[halfIndex]
                halfIndex++
            }
        }
        return Pair(sortedIndex, hclens)
    }

    private fun CreateSign(clens: ShortArray, hclens: ByteArray, SortedIndex: ShortArray, size: Int): ShortArray {
        var firstIndex = 0
        while (hclens[SortedIndex[firstIndex++].toInt()].toInt() == 0)
            firstIndex--
        var clensVal: Short = 0
        var NumBit = hclens[SortedIndex[firstIndex].toInt()]
        for (i: Int in firstIndex..size - 1 step 1) {
            clensVal = (clensVal.toInt() shl (hclens[SortedIndex[i].toInt()] - NumBit).toInt()).toShort()
            clens[SortedIndex[i].toInt()] = clensVal++
            NumBit = hclens[SortedIndex[i].toInt()]
        }
        return clens
    }

    private fun createCustomHuffmanSign(CurSearchBit: Long, byteArray: ByteArray): Long {
        var HLIT: Short = 0//文字/一致長符号の個数5bit
        var HDIST: Short = 0//距離符号の個数5bit
        var HCLEN: Short = 0//符号長表の符号長表のサイズ4bit
        val MaxBit = byteArrayOf(5, 5, 4)
        var tmp = ShortArray(3, { 0 })
        //HLIT、HDIST、HCLEN、符号長表の符号長表、拡張ビットは値なので右詰め,
        //符号は左詰め
        var curSearchBit = CurSearchBit
        for (i: Int in 0..3 - 1 step 1) {
            var pair = getBit(curSearchBit, byteArray, MaxBit[i], true)
            curSearchBit = pair.first
            tmp[i] = pair.second
        }
        HLIT = tmp[0]
        HDIST = tmp[1]
        HCLEN = tmp[2]
        //(HCLEN + 4) * 3 のビット読み込み
        val NumSign = 19;
        //符号長表の符号長表作成
        var hclens = ByteArray(NumSign, { 0 })//符号長配列
        var clens = ShortArray(NumSign, { 0 })//符号配列
        //16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15の順番で要素データが並んでる,符号長を表している
        val SignInd = byteArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
        val numroop = HCLEN + 4;
        //読み込んだ順に上記のインデックス順に配列に格納, 上記の符号長の符号の長さを表している
        for (i: Int in 0..numroop - 1 step 1) {
            var pair = getBit(curSearchBit, byteArray, 3, true)//符号長数を読み込む
            curSearchBit = pair.first
            hclens[SignInd[i].toInt()] = pair.second.toByte()
        }
        //符号長が小さい順に並べる, (0個以外)
        var SortedIndex = ShortArray(NumSign, { 0 })//ソート後のインデックス配列
        var hclensCopy = hclens.copyOf()

        for (i: Int in 0..NumSign - 1 step 1) SortedIndex[i] = i.toShort()//ソートインデックス初期化
        //インデックスソート
        //符号生成をする前に符号長の短い順に整列する為のインデックス配列を生成
        //長さの短い符号から順に連番で符号を付与する
        //短い符号から長い符号へは短い符号→長い符号の上位『短い符号のビット数』ビットだけ短い符号の次の番号+残りのビットは0
        //例: 000 → 001 → 01000 → 01001
        var pair = SortIndex(SortedIndex, hclensCopy, NumSign)
        SortedIndex = pair.first

        //文字/一致長符号長表,距離符号長表生成の為の符号表生成
        clens = CreateSign(clens, hclens, SortedIndex, NumSign)

        //文字/一致長符号長表,距離符号長表生成
        val strSigLen = HLIT + 257
        val destSigLen = HDIST + 1
        var sigLenList = ByteArray(strSigLen + destSigLen, { 0 })
        var firstIndex = 0
        while (hclens[SortedIndex[firstIndex++].toInt()] == 0.toByte())
            firstIndex--
        var prevBit = 0
        var sigLenInd = 0
        while (sigLenInd < (strSigLen + destSigLen)) {
            for (i1 in firstIndex..NumSign - 1 step 1) {//bit探索
                val pair = getBit(curSearchBit, byteArray, hclens[SortedIndex[i1].toInt()], false)//符号を読み込む
                curSearchBit = pair.first
                val outBin = pair.second
                if (clens[SortedIndex[i1].toInt()] == outBin.toShort()) {//一致した場合
                    if (SortedIndex[i1] == 16.toShort()) {
                        //直前に取り出された符号長を3~6回繰り返す。16の後の2bit 00=3回 11=6回繰り返す
                        val pair = getBit(curSearchBit, byteArray, 2, true);//反復長を取り出す符号ではない為,通常順序
                        curSearchBit = pair.first
                        val obit = pair.second
                        for (i16 in 0..obit + 2 step 1) sigLenList[sigLenInd++] = prevBit.toByte()
                        break
                    }
                    if (SortedIndex[i1] == 17.toShort()) {
                        //0符号を3~10回繰り返す17の後3bit
                        val pair = getBit(curSearchBit, byteArray, 3, true)
                        curSearchBit = pair.first
                        val obit = pair.second
                        for (i17 in 0..obit + 2 step 1) sigLenList[sigLenInd++] = 0
                        break
                    }
                    if (SortedIndex[i1] == 18.toShort()) {
                        //0符号を11~138回繰り返す18の後7bit
                        val pair = getBit(curSearchBit, byteArray, 7, true)
                        curSearchBit = pair.first
                        var obit = pair.second
                        for (i18 in 0..obit + 11 - 1 step 1) sigLenList[sigLenInd++] = 0
                        break
                    }
                    //符号長の場合
                    sigLenList[sigLenInd++] = SortedIndex[i1].toByte()
                    prevBit = SortedIndex[i1].toInt()
                    break
                }
                curSearchBit -= hclens[SortedIndex[i1].toInt()];//一致しなかった場合読み込んだ分のbit位置を戻す
            }
        }

        //文字/一致長符号長表,距離符号長表からそれぞれの符号表を生成する
        //文字/一致長符号長表,距離符号長表に分割する
        var strSigLenList = ByteArray(strSigLen, { 0 })
        var destSigLenList = ByteArray(destSigLen, { 0 })
        for (i in 0..strSigLen - 1 step 1) {
            strSigLenList[i] = sigLenList[i]
        }
        for (i in 0..destSigLen - 1 step 1) {
            destSigLenList[i] = sigLenList[i + strSigLen]
        }

        var strSigLenListCopy = strSigLenList.copyOf()
        var destSigLenListCopy = destSigLenList.copyOf()

        var strSigLenListSortedIndex = ShortArray(strSigLen, { 0 })
        var destSigLenListSortedIndex = ShortArray(destSigLen, { 0 })
        var strSigList = ShortArray(strSigLen, { 0 })
        var destSigList = ShortArray(destSigLen, { 0 })
        //初期化
        for (i in 0..strSigLen - 1 step 1) strSigLenListSortedIndex[i] = i.toShort()
        for (i in 0..destSigLen - 1 step 1) destSigLenListSortedIndex[i] = i.toShort()
        //符号生成前に小さい順番にソート
        var si1 = SortIndex(strSigLenListSortedIndex, strSigLenListCopy, strSigLen)
        strSigLenListSortedIndex = si1.first
        var si2 = SortIndex(destSigLenListSortedIndex, destSigLenListCopy, destSigLen)
        destSigLenListSortedIndex = si2.first
        //文字/一致長符号表,距離符号表生成
        strSigList = CreateSign(strSigList, strSigLenList, strSigLenListSortedIndex, strSigLen);
        destSigList = CreateSign(destSigList, destSigLenList, destSigLenListSortedIndex, destSigLen);

        strSign = strSigList.copyOf()
        strNumSign = strSigLenList.copyOf()
        lenSign = destSigList.copyOf()
        lenNumSign = destSigLenList.copyOf()

        return curSearchBit
    }

    private fun DecompressHuffman(
        CurSearchBit: Long,
        byteArray: ByteArray,
        OutIndex: Int,
        OutArray: ByteArray
    ): Triple<Long, Int, ByteArray> {
        var curSearchBit = CurSearchBit
        var outIndex = OutIndex
        var outArray = OutArray.copyOf()
        var roop = true
        while (roop) {
            for (va in 0..286 - 1) {
                val pair = getBit(curSearchBit, byteArray, strNumSign[va], false);//符号を読み込む,符号なのでビッグエンディアン
                curSearchBit = pair.first
                val outBin = pair.second
                if (strNumSign[va] > 0 && strSign[va] == outBin) {//一致した場合
                    if (va <= 255) {
                        //0~255の値はそのまま置換無しでoutArray代入
                        outArray[outIndex++] = va.toByte();//置換無しで出力(1byte)
                        break
                    }
                    if (va == 256) {
                        roop = false
                        break
                    }
                    if (256 < va) {
                        //文字一致長取得//257~264は拡張ビット無し
                        val pair = getLength(va.toShort())
                        var MatchLen = pair.first.toInt() //取り出した一致長
                        val bitlen = pair.second
                        val pair1 = getBit(curSearchBit, byteArray, bitlen, true);//拡張ビット読み込み,数値なのでリトルエンディアン
                        curSearchBit = pair1.first
                        val outExpansionBit = pair.second
                        MatchLen += outExpansionBit//拡張ビット有った場合, 一致長に足す
                        //距離値処理
                        for (destVal in 0..30 - 1) {
                            val pair = getBit(curSearchBit, byteArray, lenNumSign[destVal], false)
                            curSearchBit = pair.first
                            val lenBin = pair.second
                            if (lenNumSign[destVal] > 0 && lenSign[destVal] == lenBin) {
                                val pair = getDestLength(destVal.toShort())
                                var destLen = pair.first.toInt()
                                val destbitlen = pair.second
                                val pair2 = getBit(curSearchBit, byteArray, destbitlen, true)
                                curSearchBit = pair2.first
                                val outExpansionlenBit = pair2.second
                                destLen += outExpansionlenBit
                                //取り出した一致長, 距離から値を読み出す
                                val dec = DecompressLZSS(outArray, outIndex, MatchLen.toShort(), destLen.toShort())
                                outArray = dec.first.copyOf()
                                outIndex = dec.second
                                break
                            }
                            curSearchBit -= lenNumSign[destVal]//一致しなかった場合読み込んだ分のbit位置を戻す
                        }
                        break
                    }
                }
                curSearchBit -= strNumSign[va]//一致しなかった場合読み込んだ分のbit位置を戻す
            }
        }
        return Triple(curSearchBit, outIndex, outArray)
    }

    private fun Uncompress(
        CurSearchBit: Long,
        byteArray: ByteArray,
        OutIndex: Int,
        OutArray: ByteArray
    ): Triple<Long, Int, ByteArray> {
        var curSearchBit = CurSearchBit
        var outIndex = OutIndex
        var outArray = OutArray.copyOf()
        while (curSearchBit % byteArrayNumbit != 0L) {
            curSearchBit++
        }//次のbyte境界までdata無しなので飛ばす
        var gb1 = getBit(curSearchBit, byteArray, 16, true)
        curSearchBit = gb1.first
        val LEN = gb1.second//2byte NLENの後から続くdataのbyte数
        val gb2 = getBit(curSearchBit, byteArray, 16, true)
        curSearchBit = gb2.first
        val NLEN = gb2.second//2byte LENの1の補数
        //ここからデータ
        for (i in 0..LEN - 1) {
            val gb3 = getBit(curSearchBit, byteArray, 8, true)
            curSearchBit = gb3.first
            val va = gb3.second
            outArray[outIndex++] = va.toByte()
        }
        return Triple(curSearchBit, outIndex, outArray)
    }

    private fun blockFinal(curSearchBit: Long, byteArray: ByteArray): Pair<Long, Short> {
        return getBit(curSearchBit, byteArray, 1, true)
    }

    private fun blockType(curSearchBit: Long, byteArray: ByteArray): Pair<Long, Short> {
        return getBit(curSearchBit, byteArray, 2, true)
    }

    fun getDecompressArray(ba: ByteArray, size: Int, outSize: Int): ByteArray {
        var curSearchBit: Long = 0//現bit位置
        var outIndex: Int = 0//outArrayはBYTE単位で書き込み
        var bA = bitInversion(ba, size)
        var outArray = ByteArray(outSize, { 0 })
        var roop = true
        while (roop) {
            val bf = blockFinal(curSearchBit, bA)
            curSearchBit = bf.first
            val bff = bf.second.toInt()
            if (bff == 1) roop = false
            val bt = blockType(curSearchBit, bA)
            curSearchBit = bt.first
            val btf = bt.second
            val sw = btf.toInt()
            when (sw) {
                0 -> {
                    val un = Uncompress(curSearchBit, bA, outIndex, outArray)
                    curSearchBit = un.first
                    outIndex = un.second
                    outArray = un.third.copyOf()
                }
                1 -> {
                    createFixedHuffmanSign()
                }
                2 -> {
                    curSearchBit = createCustomHuffmanSign(curSearchBit, bA)
                }
                3 -> {
                    //ブロックタイプエラー
                }
            }
            if (sw == 1 || sw == 2) {
                val dh = DecompressHuffman(curSearchBit, bA, outIndex, outArray)
                curSearchBit = dh.first
                outIndex = dh.second
                outArray = dh.third.copyOf()
            }
        }
        return outArray
    }
}