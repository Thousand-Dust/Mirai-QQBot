package com.qqbot.ai

import opennlp.tools.cmdline.doccat.DoccatFineGrainedReportListener
import opennlp.tools.doccat.*
import opennlp.tools.ml.maxent.GISTrainer
import opennlp.tools.ml.naivebayes.NaiveBayesTrainer
import opennlp.tools.util.ObjectStream
import opennlp.tools.util.PlainTextByLineStream
import opennlp.tools.util.TrainingParameters
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.wltea.analyzer.lucene.IKAnalyzer
import java.io.*

/**
 * 基于OpenNLP的文本分类器
 * @author Thousand-Dust
 */
class TextClassifier(modelPath: String, val dataPaths: Array<String> = arrayOf()) {

    // 使用训练过的分类器预测句子的类别
    var model: DoccatModel = loadModel(modelPath)
        set(value) {
            field = value
            classifier = DocumentCategorizerME(value)
        }
    // 分类器
    private var classifier: DocumentCategorizerME = DocumentCategorizerME(model)

    // 分词器
    private val anal = IKAnalyzer(true)

    /**
     * 文本分类
     * @return 分类结果 Pair<类别, 置信度>
     */
    fun categorize(text: Array<out String>): Pair<String, Double> {
        val probs = classifier.categorize(text)
        //获取probs中最大的概率值的索引
        val maxProb = probs.maxOrNull() ?: probs[0]
        val maxProbIndex = probs.indexOfFirst {
            it == maxProb
        }
        val label = classifier.getCategory(maxProbIndex)
        /*if (maxProb < 0.6) {
            for (probIndex in probs.indices) {
                val prob = probs[probIndex]
                if (probIndex != maxProbIndex && prob * 2 >= maxProb) {
                    label = "未知"
                    break
                }
            }
        }*/
        return Pair(label, maxProb)
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
     * 训练
     */
    fun drillModel(inFiles: Array<String>, outFile: String, cutoff: Int = 2, iterations: Int = 10000, threads: Int = 5): DoccatModel {
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
        // 训练参数
        val mlParams = TrainingParameters()
        mlParams.put(TrainingParameters.ALGORITHM_PARAM, GISTrainer.MAXENT_VALUE)
        mlParams.put(TrainingParameters.TRAINER_TYPE_PARAM, NaiveBayesTrainer.NAIVE_BAYES_VALUE)
        mlParams.put(TrainingParameters.CUTOFF_PARAM, cutoff)
        mlParams.put(TrainingParameters.ITERATIONS_PARAM, iterations)
        mlParams.put(TrainingParameters.THREADS_PARAM, threads)
        val factory = DoccatFactory()
        val model = DocumentCategorizerME.train("zho", sampleStream, mlParams, factory)
        val modelOut: OutputStream = FileOutputStream(outFile)
        model.serialize(modelOut)

        sampleStream.close()
        lineStream.close()
        dataIn.close()

        return model
    }

    /**
     * 评估模型
     */
    fun getAccuracy(): Double {
        if (dataPaths.isEmpty()) {
            throw IllegalArgumentException("No training files")
        }
        //评估模型
        val evaluator = DocumentCategorizerEvaluator(classifier, DoccatFineGrainedReportListener())
        // 准备训练数据
        var dataIn: InputStream = FileInputStream(dataPaths[0])
        for (i in 1 until dataPaths.size) {
            dataIn = SequenceInputStream(dataIn, FileInputStream(dataPaths[i]))
        }
        val lineStream: ObjectStream<String> = PlainTextByLineStream({ dataIn }, "UTF-8")
        val sampleStream: ObjectStream<DocumentSample> = DocumentSampleStream(lineStream)
        evaluator.evaluate(sampleStream)
        val result = evaluator.accuracy

        sampleStream.close()
        lineStream.close()
        dataIn.close()

        return result
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
        return DoccatModel(file)
    }

}