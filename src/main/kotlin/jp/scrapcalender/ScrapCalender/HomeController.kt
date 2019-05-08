package jp.scrapcalender.ScrapCalender

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HomeController {
    @GetMapping("/hello")
    fun hello(): String {
        return "index"
    }
}