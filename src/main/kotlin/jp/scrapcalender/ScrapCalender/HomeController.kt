package jp.scrapcalender.ScrapCalender

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
    @GetMapping("/hello")
    fun hello(): String {
        return "index"
    }
}