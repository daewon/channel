package io.daewon.util

import com.softwaremill.tagging._

object Util {

  implicit class TaggerOps[T](val t: T) extends AnyVal {
    def tag[U]: T @@ U = t.asInstanceOf[T @@ U]
  }

}
