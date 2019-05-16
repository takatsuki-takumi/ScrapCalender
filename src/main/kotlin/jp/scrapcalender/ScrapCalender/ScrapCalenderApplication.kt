package jp.scrapcalender.ScrapCalender

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.jsoup.Jsoup
import java.sql.Connection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.jsoup.nodes.Element
import javax.print.Doc

@SpringBootApplication
class ScrapCalenderApplication

object URL_DATALINK: Table(){
    val url = text("url").primaryKey()
    val data_link = text("data_link").uniqueIndex()
}

//main function
fun main(args: Array<String>) {
    //scrap()
    //dbconnect()
	runApplication<ScrapCalenderApplication>(*args)
}

// scraping function
fun scrap() {
    val url = "https://www.nikkei.com/markets/kabu/"
    val document:Document = Jsoup.connect(url).get()
    //val ans = document.select("span").filter {it.text() == "トップ"}
    val an:Elements = document.select(".mkc-stock_prices")
    for(element:Element in an){
        println(element.text())
    }
    println("here")
}

// DB Control
fun dbconnect(){
    Database.connect("jdbc:sqlite:/Users/takatsuki.takumi/Mydev/ScrapCalender/SCDB.sqlite3", "org.sqlite.JDBC")
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
        /*URL_DATALINK.insert {
            it[url] = "https://www.yahoo.co.jp/"
            it[data_link] = "yahoo"
        }*/
        println(URL_DATALINK.selectAll().toList())
    }
}

/*
var str = "Thu May 16 09:54:10 JST 2019"
//str to date
var format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
var todate = format.parse(str)
//date to cal
val cal_int: Calendar = Calendar.getInstance ()
cal_int.setTime(todate)
//cal to date
var cal_to_date:Date = cal_int.getTime()
 */