package com.qqbot.ai

/**
 * 将字符串格式化成文本分类器需要的格式
 */
fun String.formatForClassifier(): String {
    val str = this.replace("[，。！,.!\n +]".toRegex(), "")
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