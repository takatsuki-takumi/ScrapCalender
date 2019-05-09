package jp.scrapcalender.ScrapCalender

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.jsoup.Jsoup

@SpringBootApplication
class ScrapCalenderApplication

fun main(args: Array<String>) {
    scrap()
	//runApplication<ScrapCalenderApplication>(*args)
}

fun scrap() {
    val url = "https://www.nikkei.com/markets/kabu/"
    val document = Jsoup.connect(url).get()
    println(document.select(".mkc-stock_prices").first().text())
}
