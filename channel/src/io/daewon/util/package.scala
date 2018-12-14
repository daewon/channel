package io.daewon

import com.softwaremill.tagging._

package object util {

  implicit class TaggerOps[T](val t: T) extends AnyVal {
    // shortcut for com.softwaremill.tagging.taggedWith[T] _
    def tag[U]: T @@ U = t.asInstanceOf[T @@ U]
  }

}
