package jp.scrapcalender.ScrapCalender

import org.hibernate.validator.constraints.URL
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.jsoup.Jsoup
import java.lang.Exception
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


//hash関数
fun sha256(input: String) = hashString(input)

private fun hashString(input: String): String{
    var output: String =MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString(separator = "") {
        "%02x".format(it)
    }
    return output
}

//define table
object URL_TIME_SPAN:Table(){
    var url = text("url").primaryKey()
    var date = text("date")
    var span = integer("span")
}

//view画面処理
@Controller
class ViewController {
    @GetMapping("/view")
    fun view(): String {
        println(sha256("rilakkuma"))
        return "view"
    }
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
        Database.connect("jdbc:sqlite:./SCDB.sqlite3", "org.sqlite.JDBC")
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

    @GetMapping("/check_time")
    fun check_time(@RequestParam geturl: String, @RequestParam time_span: String, model: Model): String{
        var rtn = "redirect:complete?geturl=" + geturl + "&time_span=" + time_span
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

    @GetMapping("/complete")
    fun complete(@RequestParam geturl: String, @RequestParam time_span: String, model: Model): String{
        Database.connect("jdbc:sqlite:./SCDB.sqlite3", "org.sqlite.JDBC")
        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, repetitionAttempts = 1) {
            val cal: Calendar = Calendar.getInstance (TimeZone.getDefault(), Locale.getDefault())
            cal.add(Calendar.MINUTE, time_span.toInt())
            var date_add:Date = cal.getTime()
            /*URL_TIME_SPAN.insert {
                it[url] = geturl
                it[date] = date_add.toString()
                it[span] = time_span.toInt()
            }*/
            var url_hash = sha256(geturl)
            var view_link = "./view?view_link=" + url_hash
            URL_DATALINK.insert {
                it[url] = geturl
                it[data_link] = view_link
            }
        }
        return "complete"
    }

}
