package jp.scrapcalender.ScrapCalender

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.lang.Exception

@SpringBootApplication
class ScrapCalenderApplication

//main function
fun main(args: Array<String>) {
	try{
		var test:String = args[0]
		if(test == "args"){
			var bool = true
			while(bool) {
				if (check_time()) {
					var url = get_latest_url()
					scrape(url)
					delete_latest(url)
				}
				Thread.sleep(5000)
			}
		}
	} catch (e:Exception) {
		runApplication<ScrapCalenderApplication>(*args)
	}
}