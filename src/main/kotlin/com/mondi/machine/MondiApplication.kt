package com.mondi.machine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MondiApplication

fun main(args: Array<String>) {
  runApplication<MondiApplication>(*args)
}
