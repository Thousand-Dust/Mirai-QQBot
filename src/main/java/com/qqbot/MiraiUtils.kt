package com.qqbot

/**
 * 菜单卡片，选项不可点击
 * @param prompt    预览消息
 * @param name      顶部名称
 * @param iconUrl   图片链接
 * @param buttons   按钮名称
 * @return
 */
/*fun getMenuCardNoAction(prompt: String, name: String, iconUrl: String, buttons: Array<String>): Message {
    val sb = StringBuilder()
    buttons.forEach {
        sb.append("{\"action\": \"\", \"name\": \"$it\"},")
    }
    val btn = sb.removeSuffix(",").toString()
    val value = """
            { "app": "com.tencent.miniapp",
            "desc": "",
            "view": "notification",
            "ver": "0.0.0.1",
            "prompt": "$prompt",
            "appID": "","sourceName": "","actionData": "","actionData_A": "","sourceUrl": "",
            "meta": {
              "notification": {
                "appInfo": {
                  "appName": "$name",
                  "appType": 4,
                  "ext": "",
                  "img": "",
                  "img_s": "",
                  "appid": 1108249016,
                  "iconUrl": "$iconUrl"
                },
                "button": [$btn],
                "emphasis_keyword": ""
              }
            },
              "text": "",
              "sourceAd": ""
            }
        """.trimIndent()
    return mif.jsonEx(value).toMessage()
}*/
