package com.charmflex.xiangqi.server.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebController {
    @GetMapping("/intro")
    fun intro(): String {
        return "forward:/intro/index.html"
    }
}