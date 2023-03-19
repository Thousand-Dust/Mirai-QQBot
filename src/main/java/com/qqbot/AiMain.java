package com.qqbot;

import com.qqbot.ai.AiUtilsKt;
import com.qqbot.ai.TextClassifier;
import opennlp.tools.doccat.DoccatModel;

import java.io.File;
import java.nio.file.attribute.PosixFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AiMain {

    public static void main(String[] args) {
        //数据文件的路径
        String[] paths = new String[]{Info.AI_DATA_PATH + "/data1.txt", Info.AI_DATA_PATH + "/data2.txt", Info.AI_DATA_PATH + "/toBeTrained.txt"};
        //将第三个文件的数据删重
        AiUtilsKt.deleteDuplicateLines(paths[2], paths[2] + ".temp");
        new File(paths[2]).delete();
        new File(paths[2] + ".temp").renameTo(new File(paths[2]));
        TextClassifier classifier = new TextClassifier(Info.AI_DATA_PATH + "/model.bin", paths);
        //模型准确率
        double accuracy = classifier.getAccuracy();
        System.out.println("现在的模型准确率：" + accuracy);

        //训练次数
        int trainCount = 30_000;

        //模型准确率无变化次数
        int count = 0;
        //最大无变化次数
        int maxCount = 2;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < 20; i++) {
            System.out.println("第" + (i + 1) + "次训练");
            int index = i % 2 + 1;
            String modelName = "model1.bin";
            DoccatModel model = classifier.drillModel(paths, Info.AI_DATA_PATH + "/"+modelName, trainCount, 5);
            double newAccuracy = classifier.getAccuracy();
            //准确率差值的绝对值
            double diff = Math.abs(newAccuracy - accuracy);
            //取小数点后9位
            diff = (int) (diff * 1000_000_000) / 1000_000_000.0;
            String logStr = sdf.format(new Date())+" 模型准确率为："+newAccuracy+"，";
            if (newAccuracy >= accuracy) {
                logStr += "相对提升了：" + diff;
            } else {
                logStr += "相对下降了：" + diff;
            }
            Utils.writeFile(Info.AI_DATA_PATH + "/log.txt", (logStr+"\n").getBytes(), true);
            System.out.println(logStr);

            if (diff == 0) {
                //无变化次数+1
                count++;
                //提高训练次数
                trainCount += 20_000;
            } else {
                count = 0;
            }

            classifier.setModel(model);
            //纠正temp1文件的数据
            File file = new File(paths[2]);
            File tempFile = new File(paths[2] + ".temp");
            AiUtilsKt.dataCorrection(classifier, file, tempFile);
            file.delete();
            tempFile.renameTo(file);

            //超过最大无变化次数，停止训练
            if (count >= maxCount) {
                break;
            }
            accuracy = newAccuracy;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("训练完毕，模型准确率：" + classifier.getAccuracy());
    }

}
