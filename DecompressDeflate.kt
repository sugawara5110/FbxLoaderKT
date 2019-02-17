package jp.sugasato.fbxloaderkt

class DecompressDeflate {

    private val dest = ushortArrayOf(
        1u, 2u, 3u, 4u, 5u, 7u, 9u, 13u, 17u, 25u, 33u, 49u, 65u, 97u, 129u, 193u,
        257u, 385u, 513u, 769u, 1025u, 1537u, 2049u, 3073u, 4097u, 6145u, 8193u, 12289u, 16385u, 24577u
    )

    private val NumBit = ubyteArrayOf(
        0u, 0u, 0u, 0u, 1u, 1u, 2u, 2u, 3u, 3u, 4u, 4u, 5u, 5u, 6u, 6u,
        7u, 7u, 8u, 8u, 9u, 9u, 10u, 10u, 11u, 11u, 12u, 12u, 13u, 13u
    )

    private val byteArrayNumbit = 8u

    private var strSign = UShortArray(286, { 0u })//size286の配列を全要素0で初期化
    private var strNumSign = UByteArray(286, { 0u })
    private var lenSign = UShortArray(30, { 0u })
    private var lenNumSign = UByteArray(30, { 0u })

    private val LEFT_INV_BIT1 = 0b0101010101010101
    private val RIGHT_INV_BIT1 = 0b1010101010101010
    private val LEFT_INV_BIT2 = 0b0011001100110011
    private val RIGHT_INV_BIT2 = 0b1100110011001100
    private val LEFT_INV_BIT4 = 0b0000111100001111
    private val RIGHT_INV_BIT4 = 0b1111000011110000

    private val bitMask: IntArray =
        intArrayOf(
            0b0000000000000000,
            0b0000000000000001,
            0b0000000000000011,
            0b0000000000000111,
            0b0000000000001111,
            0b0000000000011111,
            0b0000000000111111,
            0b0000000001111111,
            0b0000000011111111,
            0b0000000111111111,
            0b0000001111111111,
            0b0000011111111111,
            0b0000111111111111,
            0b0001111111111111,
            0b0011111111111111,
            0b0111111111111111,
            0b1111111111111111
        )

    private fun intInversion(ba: Int, numBit: Int): Int {//Max16bit
        var oBa = 0
        val baseNum = 16
        //shl 左シフト  shr 符号付き右シフト ushr 符号無し右シフト
        oBa = ((ba and LEFT_INV_BIT1) shl 1) or ((ba and RIGHT_INV_BIT1) ushr 1)
        oBa = ((oBa and LEFT_INV_BIT2) shl 2) or ((oBa and RIGHT_INV_BIT2) ushr 2)
        oBa = ((oBa and LEFT_INV_BIT4) shl 4) or ((oBa and RIGHT_INV_BIT4) ushr 4)
        return ((oBa shl 8) or (oBa ushr 8)) ushr (baseNum - numBit)
    }

    private fun bitInversion(ba: UByteArray, size: Int): UByteArray {
        var baB = ba.copyOf()
        for (i: Int in 0..size - 1 step 1) {
            var baI: Int = baB[i].toInt()
            baB[i] = intInversion(baI, 8).toUByte()
        }
        return baB
    }

    private fun getLength(value: UShort): Pair<UShort, UByte> {
        var va = value
        var len: UShort = 0u
        var bitlen: UByte = 0u
        if (257u <= va && va <= 264u) {
            len = (va - 254u).toUShort()
        }
        if (265u <= va && va <= 268u) {
            len = ((va - 265u) * 2u + 11u).toUShort()
            bitlen = 1u
        }
        if (269u <= va && va <= 272u) {
            len = ((va - 269u) * 4u + 19u).toUShort()
            bitlen = 2u
        }
        if (273u <= va && va <= 276u) {
            len = ((va - 273u) * 8u + 35u).toUShort()
            bitlen = 3u
        }
        if (277u <= va && va <= 280u) {
            len = ((va - 277u) * 16u + 67u).toUShort()
            bitlen = 4u
        }
        if (281u <= va && va <= 284u) {
            len = ((va - 281u) * 32u + 131u).toUShort()
            bitlen = 5u
        }
        if ((285u).toUShort() == va) {
            len = 258u
            bitlen = 0u
        }
        return Pair(len, bitlen)
    }

    private fun getDestLength(va: UShort): Pair<UShort, UByte> {
        val len = dest[va.toInt()]
        val bitlen = NumBit[va.toInt()]
        return Pair(len, bitlen)
    }

    private fun DecompressLZSS(
        outArray: UByteArray,
        outIndex: Int,
        MatchLen: UShort,
        destLen: UShort
    ): Int {
        var srcInd = outIndex - destLen.toInt()
        for (i: Int in 0..(MatchLen - 1u).toInt() step 1) {
            var sind = i % destLen.toShort()
            outArray[i + outIndex] = outArray[sind + srcInd]
        }
        val outInd: Int = outIndex + MatchLen.toInt()
        return outInd
    }

    private fun getBit(
        CurSearchBit: ULong,
        byteArray: UByteArray,
        NumBit: UByte,
        firstRight: Boolean
    ): Pair<ULong, UShort> {
        var curSearchBit = CurSearchBit
        val baind: UInt = (curSearchBit / byteArrayNumbit).toUInt()//配列インデックス
        val bitPos: UInt = (curSearchBit % byteArrayNumbit).toUInt()//要素内bit位置インデックス
        val shiftBit = byteArrayNumbit * 3u - NumBit - bitPos
        var outBitArr = (((byteArray[baind.toInt()].toInt() shl 16) or
                (byteArray[baind.toInt() + 1].toInt() shl 8) or
                byteArray[baind.toInt() + 2].toInt()) ushr shiftBit.toInt()) and bitMask[NumBit.toInt()]
        if (firstRight) outBitArr = intInversion(outBitArr, NumBit.toInt())
        curSearchBit += NumBit
        return Pair(curSearchBit, outBitArr.toUShort())
    }

    private fun createFixedHuffmanSign() {
        var init286s = UShortArray(286, { 0u })
        var init286b = UByteArray(286, { 0u })
        var init30s = UShortArray(30, { 0u })
        var init30b = UByteArray(30, { 0u })
        strSign = init286s
        strNumSign = init286b
        lenSign = init30s
        lenNumSign = init30b
        for (i: Int in 0..286 - 1 step 1) {
            if (0 <= i && i <= 143) {
                strSign[i] = (i + 48).toUShort()
                strNumSign[i] = 8u
            }
            if (144 <= i && i <= 255) {
                strSign[i] = (i + 256).toUShort()
                strNumSign[i] = 9u
            }
            if (256 <= i && i <= 279) {
                strSign[i] = (i - 256).toUShort()
                strNumSign[i] = 7u
            }
            if (280 <= i && i <= 287) {
                strSign[i] = (i - 88).toUShort()
                strNumSign[i] = 8u
            }
        }
        for (i: Int in 0..30 - 1 step 1) {
            lenSign[i] = i.toUShort()
            lenNumSign[i] = 5u
        }
    }

    private fun SortIndex(sortedIndex: UShortArray, hclens: UByteArray, size: Int): Pair<UShortArray, UByteArray> {
        val topSize: Int = (size * 0.5).toInt()
        val halfSize = size - topSize

        var topSortedIndex = sortedIndex.copyOfRange(0, topSize)
        var tophclens = hclens.copyOfRange(0, topSize)
        var halfSortedIndex = sortedIndex.copyOfRange(topSize, topSize + halfSize)
        var halfhclens = hclens.copyOfRange(topSize, topSize + halfSize)

        if (topSize > 1) {
            val pair = SortIndex(topSortedIndex, tophclens, topSize)
            topSortedIndex = pair.first
            tophclens = pair.second
        }
        if (halfSize > 1) {
            val pair = SortIndex(halfSortedIndex, halfhclens, halfSize)
            halfSortedIndex = pair.first
            halfhclens = pair.second
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

    private fun CreateSign(clens: UShortArray, hclens: UByteArray, SortedIndex: UShortArray, size: Int): UShortArray {
        var firstIndex = 0
        while (hclens[SortedIndex[firstIndex++].toInt()] == 0.toUByte());
        firstIndex--
        var clensVal: UShort = 0u
        var NumBit = hclens[SortedIndex[firstIndex].toInt()]
        for (i: Int in firstIndex..size - 1 step 1) {
            clensVal = (clensVal.toUInt() shl (hclens[SortedIndex[i].toInt()] - NumBit).toInt()).toUShort()
            clens[SortedIndex[i].toInt()] = clensVal++
            NumBit = hclens[SortedIndex[i].toInt()]
        }
        return clens
    }

    private fun createCustomHuffmanSign(CurSearchBit: ULong, byteArray: UByteArray): ULong {
        var HLIT: UShort = 0u//文字/一致長符号の個数5bit
        var HDIST: UShort = 0u//距離符号の個数5bit
        var HCLEN: UShort = 0u//符号長表の符号長表のサイズ4bit
        val MaxBit = ubyteArrayOf(5u, 5u, 4u)
        var tmp = UShortArray(3, { 0u })
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
        val NumSign = 19
        //符号長表の符号長表作成
        var hclens = UByteArray(NumSign, { 0u })//符号長配列
        var clens = UShortArray(NumSign, { 0u })//符号配列
        //16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15の順番で要素データが並んでる,符号長を表している
        val SignInd = ubyteArrayOf(16u, 17u, 18u, 0u, 8u, 7u, 9u, 6u, 10u, 5u, 11u, 4u, 12u, 3u, 13u, 2u, 14u, 1u, 15u)
        val numroop = HCLEN + 4u
        //読み込んだ順に上記のインデックス順に配列に格納, 上記の符号長の符号の長さを表している
        for (i: Int in 0..numroop.toInt() - 1 step 1) {
            var pair = getBit(curSearchBit, byteArray, 3u, true)//符号長数を読み込む
            curSearchBit = pair.first
            hclens[SignInd[i].toInt()] = pair.second.toUByte()
        }
        //符号長が小さい順に並べる, (0個以外)
        var SortedIndex = UShortArray(NumSign, { 0u })//ソート後のインデックス配列
        var hclensCopy = hclens.copyOf()

        for (i: Int in 0..NumSign - 1 step 1) SortedIndex[i] = i.toUShort()//ソートインデックス初期化
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
        val strSigLen = HLIT + 257u
        val destSigLen = HDIST + 1u
        var sigLenList = UByteArray((strSigLen + destSigLen).toInt(), { 0u })
        var firstIndex = 0
        while (hclens[SortedIndex[firstIndex++].toInt()] == 0.toUByte());
        firstIndex--
        var prevBit = 0
        var sigLenInd = 0
        while (sigLenInd < (strSigLen + destSigLen).toInt()) {
            for (i1 in firstIndex..NumSign - 1 step 1) {//bit探索
                val pair = getBit(curSearchBit, byteArray, hclens[SortedIndex[i1].toInt()].toUByte(), false)//符号を読み込む
                curSearchBit = pair.first
                val outBin = pair.second
                if (clens[SortedIndex[i1].toInt()] == outBin.toUShort()) {//一致した場合
                    if (SortedIndex[i1] == 16.toUShort()) {
                        //直前に取り出された符号長を3~6回繰り返す。16の後の2bit 00=3回 11=6回繰り返す
                        val pair = getBit(curSearchBit, byteArray, 2u, true)//反復長を取り出す符号ではない為,通常順序
                        curSearchBit = pair.first
                        val obit = pair.second.toInt()
                        for (i16 in 0..obit + 2 step 1) sigLenList[sigLenInd++] = prevBit.toUByte()
                        break
                    }
                    if (SortedIndex[i1] == 17.toUShort()) {
                        //0符号を3~10回繰り返す17の後3bit
                        val pair = getBit(curSearchBit, byteArray, 3u, true)
                        curSearchBit = pair.first
                        val obit = pair.second.toInt()
                        for (i17 in 0..obit + 2 step 1) sigLenList[sigLenInd++] = 0u
                        break
                    }
                    if (SortedIndex[i1] == 18.toUShort()) {
                        //0符号を11~138回繰り返す18の後7bit
                        val pair = getBit(curSearchBit, byteArray, 7u, true)
                        curSearchBit = pair.first
                        var obit = pair.second.toInt()
                        for (i18 in 0..obit + 11 - 1 step 1) sigLenList[sigLenInd++] = 0u
                        break
                    }
                    //符号長の場合
                    sigLenList[sigLenInd++] = SortedIndex[i1].toUByte()
                    prevBit = SortedIndex[i1].toInt()
                    break
                }
                curSearchBit -= hclens[SortedIndex[i1].toInt()].toULong()//一致しなかった場合読み込んだ分のbit位置を戻す
            }
        }
        //文字/一致長符号長表,距離符号長表からそれぞれの符号表を生成する
        //文字/一致長符号長表,距離符号長表に分割する
        var strSigLenList = sigLenList.copyOfRange(0, strSigLen.toInt())
        var destSigLenList = sigLenList.copyOfRange(strSigLen.toInt(), strSigLen.toInt() + destSigLen.toInt())

        var strSigLenListCopy = strSigLenList.copyOf()
        var destSigLenListCopy = destSigLenList.copyOf()

        var strSigLenListSortedIndex = UShortArray(strSigLen.toInt(), { 0u })
        var destSigLenListSortedIndex = UShortArray(destSigLen.toInt(), { 0u })
        var strSigList = UShortArray(strSigLen.toInt(), { 0u })
        var destSigList = UShortArray(destSigLen.toInt(), { 0u })
        //初期化
        for (i in 0..strSigLen.toInt() - 1 step 1) strSigLenListSortedIndex[i] = i.toUShort()
        for (i in 0..destSigLen.toInt() - 1 step 1) destSigLenListSortedIndex[i] = i.toUShort()
        //符号生成前に小さい順番にソート
        var si1 = SortIndex(strSigLenListSortedIndex, strSigLenListCopy, strSigLen.toInt())
        strSigLenListSortedIndex = si1.first
        var si2 = SortIndex(destSigLenListSortedIndex, destSigLenListCopy, destSigLen.toInt())
        destSigLenListSortedIndex = si2.first
        //文字/一致長符号表,距離符号表生成
        strSigList = CreateSign(strSigList, strSigLenList, strSigLenListSortedIndex, strSigLen.toInt());
        destSigList = CreateSign(destSigList, destSigLenList, destSigLenListSortedIndex, destSigLen.toInt());

        strSign = strSigList
        strNumSign = strSigLenList
        lenSign = destSigList
        lenNumSign = destSigLenList

        return curSearchBit
    }

    private fun DecompressHuffman(
        CurSearchBit: ULong,
        byteArray: UByteArray,
        OutIndex: Int,
        OutArray: UByteArray
    ): Triple<ULong, Int, UByteArray> {
        var curSearchBit = CurSearchBit
        var outIndex = OutIndex
        var outArray = OutArray.copyOf()
        var roop = true
        while (roop) {
            for (va in 0..286 - 1) {
                val pair = getBit(curSearchBit, byteArray, strNumSign[va].toUByte(), false);//符号を読み込む,符号なのでビッグエンディアン
                curSearchBit = pair.first
                val outBin = pair.second
                if (strNumSign[va] > 0u && strSign[va] == outBin) {//一致した場合
                    if (va <= 255) {
                        //0~255の値はそのまま置換無しでoutArray代入
                        outArray[outIndex++] = va.toUByte();//置換無しで出力(1byte)
                        break
                    }
                    if (va == 256) {
                        roop = false
                        break
                    }
                    if (256 < va) {
                        //文字一致長取得//257~264は拡張ビット無し
                        val pair = getLength(va.toUShort())
                        var MatchLen: UShort = pair.first //取り出した一致長
                        val bitlen: UByte = pair.second
                        val pair1 = getBit(curSearchBit, byteArray, bitlen, true)//拡張ビット読み込み,数値なのでリトルエンディアン
                        curSearchBit = pair1.first
                        var outExpansionBit: UShort = pair1.second.toUShort()
                        val matchlen = MatchLen + outExpansionBit//拡張ビット有った場合, 一致長に足す
                        MatchLen = matchlen.toUShort()
                        //距離値処理
                        for (destVal in 0..30 - 1) {
                            val pair = getBit(curSearchBit, byteArray, lenNumSign[destVal].toUByte(), false)
                            curSearchBit = pair.first
                            val lenBin = pair.second
                            if (lenNumSign[destVal] > 0u && lenSign[destVal] == lenBin) {
                                val pair = getDestLength(destVal.toUShort())
                                var destLen: UShort = pair.first
                                val destbitlen: UByte = pair.second
                                val pair2 = getBit(curSearchBit, byteArray, destbitlen, true)
                                curSearchBit = pair2.first
                                val outExpansionlenBit = pair2.second
                                destLen = (destLen + outExpansionlenBit).toUShort()
                                //取り出した一致長, 距離から値を読み出す
                                val dec = DecompressLZSS(outArray, outIndex, MatchLen, destLen)
                                outIndex = dec
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
        CurSearchBit: ULong,
        byteArray: UByteArray,
        OutIndex: Int,
        OutArray: UByteArray
    ): Triple<ULong, Int, UByteArray> {
        var curSearchBit = CurSearchBit
        var outIndex = OutIndex
        var outArray = OutArray.copyOf()
        while (curSearchBit % byteArrayNumbit != 0uL) {
            curSearchBit++
        }//次のbyte境界までdata無しなので飛ばす
        var gb1 = getBit(curSearchBit, byteArray, 16u, true)
        curSearchBit = gb1.first
        val LEN = gb1.second//2byte NLENの後から続くdataのbyte数
        val gb2 = getBit(curSearchBit, byteArray, 16u, true)
        curSearchBit = gb2.first
        val NLEN = gb2.second//2byte LENの1の補数
        //ここからデータ
        for (i in 0..LEN.toInt() - 1) {
            val gb3 = getBit(curSearchBit, byteArray, 8u, true)
            curSearchBit = gb3.first
            val va = gb3.second
            outArray[outIndex++] = va.toUByte()
        }
        return Triple(curSearchBit, outIndex, outArray)
    }

    private fun blockFinal(curSearchBit: ULong, byteArray: UByteArray): Pair<ULong, UShort> {
        return getBit(curSearchBit, byteArray, 1u, true)
    }

    private fun blockType(curSearchBit: ULong, byteArray: UByteArray): Pair<ULong, UShort> {
        return getBit(curSearchBit, byteArray, 2u, true)
    }

    fun getDecompressArray(ba: UByteArray, size: Int, outSize: Int): UByteArray {
        var curSearchBit: ULong = 0u//現bit位置
        var outIndex: Int = 0//outArrayはBYTE単位で書き込み
        var bA = bitInversion(ba, size)
        var outArray = UByteArray(outSize, { 0u })
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
                    outArray = un.third
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
                outArray = dh.third
            }
        }
        return outArray
    }
}