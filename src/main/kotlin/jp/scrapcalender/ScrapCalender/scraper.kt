//./gradlew bootRun --args "args"
package jp.scrapcalender.ScrapCalender

import com.opencsv.CSVWriter
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.Selector
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import org.slf4j.LoggerFactory
import java.io.FileWriter
import java.io.IOException

//最新のスケジュールと今の時間の比較
fun check_time(): Boolean{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var checker = true
    var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
    var date_now: Date = cal.getTime()
    var latest_date = get_latest().first
    var format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
    var latest_date_date = format.parse(latest_date)
    if(date_now.before(latest_date_date)){
        checker = false
    }
    return checker
}

//スケジュールが最近のURL取得
fun get_latest_url(): String{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var latest_url = get_latest().second
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
    val logger = LoggerFactory.getLogger("log")
    val document: Document = Jsoup.connect(url).get()
    var url_hash = sha256(url)
    var cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
    var date_now: Date = cal.getTime()
    var counter = true
    transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for (column in URLHASH_TYPE_SELECTER_ID.select(URLHASH_TYPE_SELECTER_ID.urlhash eq url_hash)) {
            //スクレイピングの結果取得
            try {
                var elems: Elements = document.select(column[URLHASH_TYPE_SELECTER_ID.selecter].toString())
                for (elem: Element in elems) {
                    //同じデータが連続するのを許すかどうか
                    for (getsame in URL_TIME_SPAN_SAME.select(URL_TIME_SPAN_SAME.url eq url)) {
                        if (getsame[URL_TIME_SPAN_SAME.same].toString() == "true") {
                            //同じデータが連続しているかどうか
                            for (get_before in URLHASH_DATE_DATA_ID.select(URLHASH_DATE_DATA_ID.urlhash eq sha256(url)).orderBy(URLHASH_DATE_DATA_ID.id,isAsc = false).limit(1)) {
                                counter = false
                                if (get_before[URLHASH_DATE_DATA_ID.data].toString() != elem.text()) {
                                    URLHASH_DATE_DATA_ID.insert {
                                        it[urlhash] = url_hash
                                        it[date] = date_now.toString()
                                        it[data] = elem.text()
                                    }
                                }
                            }
                            if (counter) {
                                URLHASH_DATE_DATA_ID.insert {
                                    it[urlhash] = url_hash
                                    it[date] = date_now.toString()
                                    it[data] = elem.text()
                                }
                            }
                        } else {
                            URLHASH_DATE_DATA_ID.insert {
                                it[urlhash] = url_hash
                                it[date] = date_now.toString()
                                it[data] = elem.text()
                            }
                        }
                    }
                }
            }catch (e: Selector.SelectorParseException){
                logger.info(e.toString())
            }
        }
    }
    add_csv(sha256(url))
}

fun get_latest():Pair<String,String>{
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var temp_latest = ""
    var temp_url = ""
    var first_flag = true
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
        for(column in URL_TIME_SPAN_SAME.selectAll()){
            if (first_flag) {
                temp_latest = column[URL_TIME_SPAN_SAME.date]
                temp_url = column[URL_TIME_SPAN_SAME.url]
                first_flag = false
            }else{
                var format = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                var temp = format.parse(column[URL_TIME_SPAN_SAME.date])
                if (temp.before(format.parse(temp_latest)) or temp.equals(format.parse(temp_latest))){
                    temp_latest = temp.toString()
                    temp_url = column[URL_TIME_SPAN_SAME.url]
                }
            }
        }
    }
    return Pair(temp_latest,temp_url)
}

fun add_csv(view_link: String){
    val logger = LoggerFactory.getLogger("log")
    Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
    var filename = view_link + ".csv"
    var filewriter = FileWriter(filename)
    var csvWriter = CSVWriter(filewriter,
            CSVWriter.DEFAULT_SEPARATOR,
            CSVWriter.NO_QUOTE_CHARACTER,
            CSVWriter.DEFAULT_ESCAPE_CHARACTER,
            CSVWriter.DEFAULT_LINE_END
    )
    var data = arrayOf<String>("ID","DATE","DATA")
    csvWriter.writeNext(data)
    var counter = 1
    transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
        for(column in URLHASH_DATE_DATA_ID.select(URLHASH_DATE_DATA_ID.urlhash eq view_link).orderBy(URLHASH_DATE_DATA_ID.id,isAsc = false)){
            //add csv
            data = arrayOf(counter.toString(), column[URLHASH_DATE_DATA_ID.date], column[URLHASH_DATE_DATA_ID.data])
            csvWriter.writeNext(data)
            counter = counter + 1
        }
    }
    try {
        filewriter.flush()
        filewriter.close()
        csvWriter.close()
    } catch (e: IOException) {
        println("Flushing/closing error!")
        logger.info(e.toString())
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