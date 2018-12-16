package com.droxt.officemapper

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class OfficemapperApplication

fun main(args: Array<String>) {
    SpringApplication.run(OfficemapperApplication::class.java, *args)
}