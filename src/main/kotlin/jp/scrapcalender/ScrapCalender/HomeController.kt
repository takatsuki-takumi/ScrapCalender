package jp.scrapcalender.ScrapCalender

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
    @GetMapping("/")
    fun hello(): String {
        return "index"
    }
}