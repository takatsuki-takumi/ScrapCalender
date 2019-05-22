package jp.scrapcalender.ScrapCalender

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.servlet.ModelAndView
import org.jsoup.Jsoup
import java.lang.Exception
import java.security.MessageDigest
import java.util.*
import java.util.Date
import kotlin.collections.ArrayList
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json




//hash関数
fun sha256(input: String) = hashString(input)

private fun hashString(input: String): String{
    var output: String =MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString(separator = "") {
        "%02x".format(it)
    }
    return output
}

//define table
object URL_DATALINK: Table(){
    val url = text("url").primaryKey()
    val data_link = text("data_link").uniqueIndex()
}
object URL_TIME_SPAN_SAME:Table(){
    var url = text("url").primaryKey()
    var date = text("date")
    var span = integer("span")
    var same = text("same")
}
object URLHASH_TYPE_SELECTER_ID:Table(){
    var urlhash = text("urlhash")
    var type = text("type")
    var selecter = text("selecter")
    var id = integer("id").autoIncrement()
}
object URLHASH_DATE_DATA_ID:Table(){
    var urlhash = text("urlhash")
    var date = text("date")
    var data = text("data")
    var id = integer("id").autoIncrement()
}


//select画面処理
@Controller
class Controller {
    var search_list:ArrayList<ArrayList<String>> = arrayListOf()

    //home画面処理
    @GetMapping("/")
    fun home(mav: ModelAndView, model: Model, @RequestParam(defaultValue = "") error: String): ModelAndView {
        search_list = arrayListOf()
        //ビューの設定
        mav.setViewName("home")
        //エラー文の表示
        if (error != ""){
            model.addAttribute("error", "不適切なURLです。")
        } else {
            model.addAttribute("error", "")
        }
        //データーベース内容のオブジェクト化
        var urllist : ArrayList<ArrayList<String>> = arrayListOf()
        Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
        transaction (transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
            for (url in URL_DATALINK.selectAll()) {
                var templist : ArrayList<String> = arrayListOf()
                templist.add(url[URL_DATALINK.url].toString())
                templist.add(url[URL_DATALINK.data_link].toString())
                urllist.add(templist)
            }
            mav.addObject("urllist",urllist)
        }
        return mav
    }

    //select画面処理
    @GetMapping("/select")
    fun select(@RequestParam geturl : String, @RequestParam(defaultValue = "") selecter_box: String, @RequestParam(defaultValue = "") tags_box: String, model: Model): String {
        var search_with:ArrayList<String> = arrayListOf()
        model.addAttribute("geturl", geturl)
        if (selecter_box != ""){
            search_with.add("selecter")
            search_with.add(selecter_box)
            search_list.add(search_with)
            search_with = arrayListOf()
        }
        if (tags_box != ""){
            search_with.add("tags")
            search_with.add(tags_box)
            search_list.add(search_with)
            search_with = arrayListOf()
        }
        model.addAttribute("search_list",search_list)
        return "select"
    }

    //urlのエラーチェック
    @GetMapping("/check_url")
    fun checkurl(@RequestParam geturl: String, model: Model): String{
        search_list = arrayListOf()
        var rtn = "redirect:select?geturl=" + geturl
        if (geturl == ""){
            rtn = "redirect:?error=url"
        }
        try {
            Jsoup.connect(geturl).get()
        }catch (e:Exception){
            rtn = "redirect:?error=url"
        }
        return rtn
    }

    //confirm画面処理
    @GetMapping("/confirm")
    fun confirm(@RequestParam geturl: String,@RequestParam(defaultValue = "") error: String,model:Model): String{
        if (error != ""){
            model.addAttribute("error", "不適切な数値です。")
        } else {
            model.addAttribute("error", "")
        }
        model.addAttribute("geturl",geturl)
        model.addAttribute("search_list", search_list)
        return "confirm"
    }

    //spanタイムエラー処理
    @GetMapping("/check_time")
    fun check_time(@RequestParam geturl: String, @RequestParam time_span: String, @RequestParam(defaultValue = "false") getsame: String, model: Model): String{
        var rtn = "redirect:complete?geturl=" + geturl + "&time_span=" + time_span + "&getsame=" + getsame
        var time_span_int = 0
        if (time_span == "0"){
            rtn = "redirect:confirm?geturl=" + geturl + "&error=time"
        }
        if (time_span == ""){
            rtn = "redirect:confirm?geturl=" + geturl + "&error=time"
        }
        try{
            time_span_int = time_span.toInt()
        }catch (e:Exception){
            rtn =  "redirect:confirm?geturl=" + geturl + "&error=time"
        }
        if (time_span_int < 1){
            rtn = "redirect:confirm?geturl=" + geturl + "&error=time"
        }
        return rtn
    }

    //コンプリート画面処理
    @GetMapping("/complete")
    fun complete(@RequestParam geturl: String, @RequestParam time_span: String, @RequestParam(defaultValue = "false") getsame: String, model: Model): String{
        Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
            val cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
            cal.add(Calendar.MINUTE, time_span.toInt())
            var date_add:Date = cal.getTime()
            URL_TIME_SPAN_SAME.insert {
                it[url] = geturl
                it[date] = date_add.toString()
                it[span] = time_span.toInt()
                it[same] = getsame
            }
            var url_hash = sha256(geturl)
            var view_link = "./view?view_link=" + url_hash
            URL_DATALINK.insert {
                it[url] = geturl
                it[data_link] = view_link
            }
            for(list in search_list){
                URLHASH_TYPE_SELECTER_ID.insert{
                    it[urlhash] = url_hash
                    it[type] = list[0]
                    it[selecter] = list[1]
                }
            }
        }
        return "complete"
    }

    //view画面処理
    @GetMapping("/view")
    fun view(@RequestParam view_link:String, model: Model,mav: ModelAndView): ModelAndView {
        mav.setViewName("view")
        var listinlist:ArrayList<ArrayList<String>> = arrayListOf()
        Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
            for(column in URLHASH_DATE_DATA_ID.select(URLHASH_DATE_DATA_ID.urlhash eq view_link).orderBy(URLHASH_DATE_DATA_ID.date,isAsc = false)){
                var templist:ArrayList<String> = arrayListOf()
                templist.add(column[URLHASH_DATE_DATA_ID.date].toString())
                templist.add(column[URLHASH_DATE_DATA_ID.data].toString())
                listinlist.add((templist))
            }
            mav.addObject("data_list",listinlist)
        }
        return mav
    }

}

@RestController
class RestController{
    class Datas(
        var datalist: ArrayList<Data_one> = arrayListOf(),
        var tableid: String,
        var number_of_data: Int
    )

    class Data_one(
        var date:String = "",
        var data:String = ""
    )

    @RequestMapping("/view/db_data")
    @ResponseBody
    fun get_db(@RequestParam viewid: String,@RequestParam(defaultValue = "30") numberofdata:String): Datas{
        Database.connect("jdbc:sqlite:./SCDB.db", "org.sqlite.JDBC")
        var datas = Datas(datalist = arrayListOf(),number_of_data = 0,tableid = "")
        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1){
            for(column in URLHASH_DATE_DATA_ID.select(URLHASH_DATE_DATA_ID.urlhash eq viewid).orderBy(URLHASH_DATE_DATA_ID.date,isAsc = false).limit(numberofdata.toInt())){
                var one_data = Data_one(date = column[URLHASH_DATE_DATA_ID.date].toString(),data = column[URLHASH_DATE_DATA_ID.data].toString())
                datas.datalist.add(one_data)
                datas.tableid = column[URLHASH_DATE_DATA_ID.urlhash].toString()
            }
        }
        datas.number_of_data = datas.datalist.size
        return datas
    }
}
