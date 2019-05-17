//./gradlew bootRun --args "args"
package jp.scrapcalender.ScrapCalender

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

fun check_time(): Boolean{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var checker = true
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
        var date_now: Date = cal.getTime()
        for(column in URL_TIME_SPAN.selectAll().orderBy(URL_TIME_SPAN.date).limit(1)){
            var latest_date = column[URL_TIME_SPAN.date].toString()
            var format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
            var latest_date_date = format.parse(latest_date)
            if(date_now.before(latest_date_date)){
                checker = false
            }
        }
    }
    return checker
}

fun get_latest_url(): String{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var latest_url = ""
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for(column in URL_TIME_SPAN.selectAll().orderBy(URL_TIME_SPAN.date).limit(1)){
            latest_url = column[URL_TIME_SPAN.url].toString()
        }
    }
    return latest_url
}

fun delete_latest(latest_url: String){
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        var span_get:Int = 0
        for(column in URL_TIME_SPAN.select(URL_TIME_SPAN.url eq latest_url)){
            span_get = column[URL_TIME_SPAN.span]
        }
        val now_cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
        now_cal.add(Calendar.MINUTE, span_get)
        var update_date:Date = now_cal.getTime()
        URL_TIME_SPAN.deleteWhere {
            URL_TIME_SPAN.url eq latest_url
        }
        URL_TIME_SPAN.insert {
            it[url] = latest_url
            it[date] = update_date.toString()
            it[span] = span_get
        }
    }
}

fun scrape(url: String) {
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    val document: Document = Jsoup.connect(url).get()
    var url_hash = sha256(url)
    var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
    var date_now: Date = cal.getTime()
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for (column in URLHASH_TYPE_SELECTER_ID.select(URLHASH_TYPE_SELECTER_ID.urlhash eq url_hash)){
            var elems: Elements = document.select(column[URLHASH_TYPE_SELECTER_ID.selecter].toString())
            for(elem: Element in elems){
                URLHASH_DATE_DATA_ID.insert {
                    it[urlhash] = url_hash
                    it[date] = date_now.toString()
                    it[data] = elem.text()
                }
            }
        }
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