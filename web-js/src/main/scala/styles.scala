package uk.co.sprily
package btf.webjs

import scalacss.Defaults._
import scalacss.ScalaCssReact._

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(50.px))
  )

}

object grid extends StyleSheet.Inline {
  import dsl._

  val row = style(
    addClassName("row")
  )

  val col = 
    intStyle(1 to 12)(i => styleS(
      addClassName(s"col-sm-$i")
  ))
}
//class MaterialiseStyles(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {
//  import dsl._
//
//
//}
