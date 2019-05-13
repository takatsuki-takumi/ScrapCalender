package jp.scrapcalender.ScrapCalender

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.ui.Model
import java.sql.Connection
import org.springframework.web.servlet.ModelAndView
import java.util.ArrayList


//home画面処理
@Controller
class HomeController {
    @GetMapping("/")
    fun home(mav: ModelAndView): ModelAndView {
        //ビューの設定
        mav.setViewName("home")
        //データーベース内容のオブジェクト化
        var urllist : ArrayList<ArrayList<String>> = arrayListOf()
        Database.connect("jdbc:sqlite:/Users/takatsuki.takumi/Mydev/ScrapCalender/SCDB.sqlite3", "org.sqlite.JDBC")
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
}

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
class SelectController {
    @GetMapping("/select")
    fun view(): String {
        return "select"
    }
}