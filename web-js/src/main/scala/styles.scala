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

//class MaterialiseStyles(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {
//  import dsl._
//
//
//}
