package com.qqbot.ai

import com.qqbot.Utils
import opennlp.tools.doccat.*
import opennlp.tools.util.ObjectStream
import opennlp.tools.util.PlainTextByLineStream
import opennlp.tools.util.TrainingParameters
import java.io.*
import java.util.*

/**
 * 基于OpenNLP的文本分类器
 * @author Thousand-Dust
 */
class TextClassifier {

    companion object {
        const val modelFileName = "ai/classifier"
    }

    private val model = loadModel()

    // 使用训练过的分类器预测句子的类别
    private val classifier = DocumentCategorizerME(model)

    /**
     * 文本分类
     */
    fun categorize(text: String): String {
        val formatText = text.formatForClassifier()
        if (formatText.isEmpty()) {
            return "无意义"
        }
        val probs = classifier.categorize(formatText.split(" ").toTypedArray())
        val label = classifier.getBestCategory(probs)
        //获取probs中最大的概率值的索引
        val maxProb = probs.maxOrNull() ?: probs[0]
        val maxProbIndex = probs.indexOfFirst {
            it == probs.maxOrNull()
        }
        //保存结果用于调整模型
        saveData(if (maxProb < 0.3) "其他" else label, formatText)
        for (probIndex in probs.indices) {
            if (probIndex != maxProbIndex && probs[probIndex] >= maxProb/2) {
                return "其他"
            }
        }
        return label
    }

    /**
     * 将分类结果保存到文件
     */
    fun saveData(label: String, text: String) {
        Utils.writeFile(
            "$modelFileName.$label",
            "$label ${text.formatForClassifier()}\n".toByteArray(),
            true
        )
    }

    /**
     * 训练
     */
    private fun drillModel(): DoccatModel {
        // 准备训练数据
        val fileInput1 = FileInputStream("${modelFileName}.txt")
        val fileInput2 = FileInputStream("ai/data2.txt")
        val dataIn: InputStream = SequenceInputStream(fileInput1, fileInput2)
        val lineStream: ObjectStream<String> = PlainTextByLineStream({ dataIn }, "UTF-8")
        val sampleStream: ObjectStream<DocumentSample> = DocumentSampleStream(lineStream)
        // 训练文本分类器
        val mlParams = TrainingParameters()
        mlParams.put(TrainingParameters.ALGORITHM_PARAM, "MAXENT")
        mlParams.put(TrainingParameters.CUTOFF_PARAM, 32)
        mlParams.put(TrainingParameters.ITERATIONS_PARAM, 300)
        mlParams.put(TrainingParameters.THREADS_PARAM, 5)
        mlParams.put(TrainingParameters.TRAINER_TYPE_PARAM, 1)
        val factory = DoccatFactory()
        val model = DocumentCategorizerME.train("ch", sampleStream, mlParams, factory)
        val modelOut: OutputStream = FileOutputStream("$modelFileName.bin")
        model.serialize(modelOut)
        return model
    }


    /**
     * 读取训练数据
     */
    private fun loadModel(): DoccatModel {
        //判断文件是否存在
        val file = File("$modelFileName.bin")
        if (!file.exists()) {
            return drillModel()
        }
        // 读取训练数据
        val dataIn: InputStream = FileInputStream(file.path)
        return DoccatModel(dataIn)
    }

}