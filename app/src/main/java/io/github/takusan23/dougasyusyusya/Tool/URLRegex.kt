package io.github.takusan23.dougasyusyusya.ViewModel

import java.util.regex.Pattern

/**
 * 正規表現でURLを取り出す
 * @param text URLが入った文字列
 * @return なければnull
 * */
fun regexUrl(text: String): String? {
    //正規表現で取り出す
    val urlRegex = Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+").matcher(text)
    return if (urlRegex.find()) {
        urlRegex.group()
    } else {
        null
    }
}
