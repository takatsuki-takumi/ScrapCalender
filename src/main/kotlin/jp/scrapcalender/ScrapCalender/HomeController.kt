package jp.scrapcalender.ScrapCalender

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
    @GetMapping("/")
    fun home(): String {
        return "home"
    }
}

@Controller
class ViewController {
    @GetMapping("/view")
    fun view(): String {
        return "view"
    }
}