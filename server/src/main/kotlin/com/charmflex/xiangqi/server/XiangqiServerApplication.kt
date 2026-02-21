package com.charmflex.xiangqi.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class XiangqiServerApplication

fun main(args: Array<String>) {
    runApplication<XiangqiServerApplication>(*args)
}
