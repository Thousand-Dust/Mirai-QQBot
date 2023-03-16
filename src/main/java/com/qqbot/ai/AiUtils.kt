package com.qqbot.ai

import com.qqbot.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.stream.Collectors

/**
 * 将@的QQ号删除
 */
fun String.removeAt(): String {
    //正咋表达式替换@和后面的6位以上数字
    return this.replace("@\\d{6,}".toRegex(), "")
}

/**
 * 将字符串格式化成文本分类器需要的格式
 */
fun String.formatForClassifier(): String {
    val str = this.replace("[\n +]".toRegex(), "")
    val regex = Regex("[\u4e00-\u9fa5]+|[a-zA-Z]+|\\d+|[^\u4e00-\u9fa5a-zA-Z\\d]+")
    val parts = regex.findAll(str).map { it.value }.toList()
    val result = parts.joinToString(" ") {
        //如果是中文字符，就在每个字符之间加上空格
        if (it.matches("[\u4e00-\u9fa5]+".toRegex())) {
            it.chunked(1).joinToString(" ")
        } else {
            it
        }
    }
    return result
}

/**
 * 删除文件中重复的行
 */
fun deleteDuplicateLines(readPath: String, outPath: String) {
    //先将文件中每行重复的内容删掉输出到另一个文件
    val fileInput = FileInputStream(readPath)
    val fileOutput = FileOutputStream(outPath)
    val lines = fileInput.bufferedReader().lines().collect(Collectors.toList())
    val set = HashSet<String>()
    for (line in lines) {
        if (set.contains(line)) continue
        set.add(line)
        fileOutput.write((line + "\n").toByteArray())
    }
    fileInput.close()
    fileOutput.close()
}

/**
 * 将未分词的数据分词
 */
fun dataParticiple(classifier: TextClassifier, readPath: String, outPath: String) {
    val dataIn = FileInputStream(readPath)
    //将文件内容按行循环读取
    dataIn.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            //将每行按空格分割成两部分
            val parts = line.split(" ", limit = 2)
            //第一部分是类别
            val label = parts[0]
            //第二部分是文本
            val text = parts[1]
            //将文本格式化
            val formattedText = classifier.participle(text)
            //将格式化后的文本和类别保存到文件
            Utils.writeFile(
                outPath,
                "$label $formattedText\n".toByteArray(),
                true
            )
        }
    }
    dataIn.close()
}

/**
 * 对数据进行修正，输出到新文件
 */
fun dataCorrection(classifier: TextClassifier, modelFile: File, dataFile: File, outDataFile: File) {
    val dataIn = FileInputStream(dataFile)
    val dataOut = FileOutputStream(outDataFile)
    //将文件内容按行循环读取
    dataIn.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            //将每行按空格分割成两部分
            val parts = line.split(" ", limit = 2)
            //第一部分是类别
            val label = parts[0]
            //第二部分是文本
            val text = parts[1]

            //先将文本进行分词
            val formatText = classifier.participle(text.removeAt())
            //将文本格式化
            val label1 = classifier.categorize(formatText.split(" ").toTypedArray()).first

            //将重新分类后的类别和文本保存到文件
            dataOut.write("$label1 $text\n".toByteArray())
        }
    }

    dataIn.close()
    dataOut.close()
}