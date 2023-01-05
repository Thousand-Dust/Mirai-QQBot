package com.qqbot.ai

import com.qqbot.Utils
import opennlp.tools.doccat.*
import opennlp.tools.util.ObjectStream
import opennlp.tools.util.PlainTextByLineStream
import opennlp.tools.util.TrainingParameters
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
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
        val probs = classifier.categorize(formatText.split(" ").toTypedArray())
        val label = classifier.getBestCategory(probs)
        saveData(label, formatText)
        return label
    }

    /**
     * 将分类结果保存到文件
     */
    fun saveData(label: String, text: String) {
        Utils.writeFile(
            "$modelFileName.add",
            "$label ${text.formatForClassifier()}\n".toByteArray(),
            true
        )
    }

    /**
     * 训练
     */
    fun drillModel(): DoccatModel {
        // 准备训练数据
        val dataIn: InputStream = FileInputStream("${modelFileName}.txt")
        val lineStream: ObjectStream<String> = PlainTextByLineStream({ dataIn }, "UTF-8")
        val sampleStream: ObjectStream<DocumentSample> = DocumentSampleStream(lineStream)
        // 训练文本分类器
        val mlParams = TrainingParameters()
        mlParams.put(TrainingParameters.ALGORITHM_PARAM, "MAXENT")
        mlParams.put(TrainingParameters.CUTOFF_PARAM, 15)
        mlParams.put(TrainingParameters.ITERATIONS_PARAM, 128)
        val factory = DoccatFactory()
        val model = DocumentCategorizerME.train("ch", sampleStream, mlParams, factory)
        val modelOut: OutputStream = FileOutputStream("$modelFileName.bin")
        model.serialize(modelOut)
        return model
    }


    /**
     * 读取训练数据
     */
    fun loadModel(): DoccatModel {
        // 读取训练数据
        val dataIn: InputStream = FileInputStream("$modelFileName.bin")
        return DoccatModel(dataIn)
    }

}