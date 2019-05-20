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

//最新のスケジュールと今の時間の比較
fun check_time(): Boolean{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var checker = true
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
        var date_now: Date = cal.getTime()
        for(column in URL_TIME_SPAN_SAME.selectAll().orderBy(URL_TIME_SPAN_SAME.date).limit(1)){
            var latest_date = column[URL_TIME_SPAN_SAME.date].toString()
            var format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
            var latest_date_date = format.parse(latest_date)
            if(date_now.before(latest_date_date)){
                checker = false
            }
        }
    }
    return checker
}

//スケジュールが最近のURL取得
fun get_latest_url(): String{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var latest_url = ""
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for(column in URL_TIME_SPAN_SAME.selectAll().orderBy(URL_TIME_SPAN_SAME.date).limit(1)){
            latest_url = column[URL_TIME_SPAN_SAME.url].toString()
        }
    }
    return latest_url
}

//最近のスケジュールの更新
fun delete_latest(latest_url: String){
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        var span_get = 0
        var same_get = ""
        for(column in URL_TIME_SPAN_SAME.select(URL_TIME_SPAN_SAME.url eq latest_url)){
            span_get = column[URL_TIME_SPAN_SAME.span]
            same_get = column[URL_TIME_SPAN_SAME.same]
        }
        val now_cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
        now_cal.add(Calendar.MINUTE, span_get)
        var update_date:Date = now_cal.getTime()
        URL_TIME_SPAN_SAME.deleteWhere {
            URL_TIME_SPAN_SAME.url eq latest_url
        }
        //一度きりかどうか
        if (span_get != 0){
            URL_TIME_SPAN_SAME.insert {
                it[url] = latest_url
                it[date] = update_date.toString()
                it[span] = span_get
                it[same] = same_get
            }
        }
    }
}

//スクレイピング
fun scrape(url: String) {
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    val document: Document = Jsoup.connect(url).get()
    var url_hash = sha256(url)
    var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
    var date_now: Date = cal.getTime()
    var counter = true
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for (column in URLHASH_TYPE_SELECTER_ID.select(URLHASH_TYPE_SELECTER_ID.urlhash eq url_hash)){
            //スクレイピングの結果取得
            var elems: Elements = document.select(column[URLHASH_TYPE_SELECTER_ID.selecter].toString())
            for(elem: Element in elems){
                //同じデータが連続するのを許すかどうか
                for(getsame in URL_TIME_SPAN_SAME.select(URL_TIME_SPAN_SAME.url eq url)){
                    if(getsame[URL_TIME_SPAN_SAME.same].toString() == "true"){
                        //同じデータが連続しているかどうか
                        for (get_before in URLHASH_DATE_DATA_ID.selectAll().orderBy(URLHASH_DATE_DATA_ID.date).limit(1)){
                            counter = false
                            if(get_before[URLHASH_DATE_DATA_ID.data].toString() != elem.text()){
                                URLHASH_DATE_DATA_ID.insert {
                                    it[urlhash] = url_hash
                                    it[date] = date_now.toString()
                                    it[data] = elem.text()
                                }
                            }
                        }
                        if(counter){
                            URLHASH_DATE_DATA_ID.insert {
                                it[urlhash] = url_hash
                                it[date] = date_now.toString()
                                it[data] = elem.text()
                            }
                        }
                    }else{
                        URLHASH_DATE_DATA_ID.insert {
                            it[urlhash] = url_hash
                            it[date] = date_now.toString()
                            it[data] = elem.text()
                        }
                    }
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