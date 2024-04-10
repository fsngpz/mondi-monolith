package com.mondi.machine

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The controller class for root.
 *
 * @author Ferdinand Sangap
 * @since 2024-04-10
 */
@RestController
@RequestMapping
class RootController {

  /**
   * a REST request to check health of service.
   *
   * @return the String OK.
   */
  @GetMapping("/")
  fun root(): String{
    return "OK"
  }
}