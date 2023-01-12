package com.qqbot.ai

import com.qqbot.Utils
import opennlp.tools.doccat.*
import opennlp.tools.ml.maxent.GISTrainer
import opennlp.tools.util.ObjectStream
import opennlp.tools.util.PlainTextByLineStream
import opennlp.tools.util.TrainingParameters
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.wltea.analyzer.lucene.IKAnalyzer
import java.io.*
import java.util.*

/**
 * 基于OpenNLP的文本分类器
 * @author Thousand-Dust
 */
class TextClassifier(modelPath: String, val dataPaths: Array<String> = arrayOf()) {

    // 使用训练过的分类器预测句子的类别
    private var classifier: DocumentCategorizerME
    // 分词器
    private val anal = IKAnalyzer(true)

    init {
        val model = loadModel(modelPath)
        classifier = DocumentCategorizerME(model)
    }

    /**
     * 文本分类
     */
    fun categorize(text: String): String {
        val formatText = participle(text)
        if (formatText.isEmpty()) {
            return "无意义"
        }
        val probs = classifier.categorize(formatText.split(" ").toTypedArray())
        //获取probs中最大的概率值的索引
        val maxProb = probs.maxOrNull() ?: probs[0]
        val maxProbIndex = probs.indexOfFirst {
            it == maxProb
        }
        var label = classifier.getCategory(maxProbIndex)
        if (maxProb < 0.6) {
            for (probIndex in probs.indices) {
                val prob = probs[probIndex]
                if (probIndex != maxProbIndex && prob * 2 >= maxProb) {
                    label = "其他"
                    break
                }
            }
        }
        //保存结果用于调整模型
        saveData(label, formatText)
        return label
    }

    /**
     * 文本分词
     */
    fun participle(text: String): String {
        val result = StringBuilder()
        val reader = StringReader(text)
        val ts = anal.tokenStream("", reader)
        try {
            //分词
            val term = ts.getAttribute(CharTermAttribute::class.java)
            ts.reset()
            //遍历分词数据
            while (ts.incrementToken()) {
                result.append(term.toString()).append(" ")
            }
            if (result.isNotEmpty()) {
                result.deleteCharAt(result.length - 1)
            } else {
                result.clear()
                result.append(text.formatForClassifier())
            }
        } finally {
            ts.close()
            reader.close()
        }

        return result.toString()
    }

    /**
     * 将分类结果保存到文件
     */
    fun saveData(label: String, text: String) {
        Utils.writeFile(
            "ai/classifier.$label",
            "$label $text\n".toByteArray(),
            true
        )
    }

    /**
     * 训练
     */
    fun drillModel(inFiles: Array<String>, outFile: String, iterations: Int = 10000): DoccatModel {
        if (inFiles.isEmpty()) {
            throw IllegalArgumentException("No training files")
        }
        // 准备训练数据
        var dataIn: InputStream = FileInputStream(inFiles[0])
        for (i in 1 until inFiles.size) {
            dataIn = SequenceInputStream(dataIn, FileInputStream(inFiles[i]))
        }
        val lineStream: ObjectStream<String> = PlainTextByLineStream({ dataIn }, "UTF-8")
        val sampleStream: ObjectStream<DocumentSample> = DocumentSampleStream(lineStream)
        // 训练文本分类器
        val mlParams = TrainingParameters()
        mlParams.put(TrainingParameters.ALGORITHM_PARAM, GISTrainer.MAXENT_VALUE)
//        mlParams.put(TrainingParameters.CUTOFF_PARAM, 7)
        mlParams.put(TrainingParameters.CUTOFF_PARAM, 0)
        mlParams.put(TrainingParameters.ITERATIONS_PARAM, iterations)
        mlParams.put(TrainingParameters.THREADS_PARAM, 5)
        mlParams.put(TrainingParameters.TRAINER_TYPE_PARAM, "NAIVE_BAYES")
        val factory = DoccatFactory()
        val model = DocumentCategorizerME.train("ch", sampleStream, mlParams, factory)
        val modelOut: OutputStream = FileOutputStream(outFile)
        model.serialize(modelOut)

        sampleStream.close()
        lineStream.close()
        dataIn.close()
        return model
    }

    /**
     * 读取训练数据
     */
    fun loadModel(fileName: String): DoccatModel {
        //判断文件是否存在
        val file = File(fileName)
        if (!file.exists()) {
            drillModel(dataPaths, fileName)
        }
        // 读取训练数据
        val dataIn: InputStream = FileInputStream(file.path)
        return DoccatModel(dataIn)
    }

}