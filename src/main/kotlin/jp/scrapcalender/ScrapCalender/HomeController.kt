package jp.scrapcalender.ScrapCalender

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestParam
import java.sql.Connection
import org.springframework.web.servlet.ModelAndView
import java.util.ArrayList
import org.jsoup.Jsoup
import java.lang.Exception


//view画面処理
@Controller
class ViewController {
    @GetMapping("/view")
    fun view(): String {
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
    fun select(@RequestParam geturl : String, @RequestParam(defaultValue = "") id_box: String, @RequestParam(defaultValue = "") class_box: String, @RequestParam(defaultValue = "") tag_box: String, model : Model): String {
        var search_with:ArrayList<String> = arrayListOf()
        model.addAttribute("geturl", geturl)
        if (id_box != ""){
            search_with.add("id")
            search_with.add(id_box)
            search_list.add(search_with)
            search_with = arrayListOf()
        }
        if (class_box != ""){
            search_with.add("class")
            search_with.add(class_box)
            search_list.add(search_with)
            search_with = arrayListOf()
        }
        if (tag_box != ""){
            search_with.add("tag")
            search_with.add(tag_box)
            search_list.add(search_with)
            search_with = arrayListOf()
        }
        model.addAttribute("id_box", id_box)
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
    fun confirm(@RequestParam geturl: String, model:Model): String{
        model.addAttribute("geturl",geturl)
        model.addAttribute("search_list", search_list)
        return "confirm"
    }

    @GetMapping("/check_time")
    fun check_time(@RequestParam geturl: String, @RequestParam time_span: String, model: Model): String{
        var rtn = "redirect:complete?geturl=" + geturl
        var time_span_int = 0
        if (time_span == "0"){
            rtn = "redirect:confirm?geturl=" + geturl
        }
        if (time_span == ""){
            rtn = "redirect:confirm?geturl=" + geturl
        }
        try{
            time_span_int = time_span.toInt()
        }catch (e:Exception){
            rtn =  "redirect:confirm?geturl=" + geturl
        }
        if (time_span_int < 1){
            rtn = "redirect:confirm?geturl=" + geturl
        }
        return rtn
    }
}
