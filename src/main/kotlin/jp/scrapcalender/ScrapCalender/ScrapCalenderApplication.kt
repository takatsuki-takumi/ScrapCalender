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
    val document = Jsoup.connect(url).get()
    val ans = document.select("span").filter {it.text() == "トップ"}
    println(ans)
}

// DB Control
fun dbconnect(){
    Database.connect("jdbc:sqlite:/Users/takatsuki.takumi/Mydev/ScrapCalender/SCDB.sqlite3", "org.sqlite.JDBC")
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
        URL_DATALINK.insert {
            it[url] = "https://www.google.com/"
            it[data_link] = "google"
        }
    }
}