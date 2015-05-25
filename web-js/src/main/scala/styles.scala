package uk.co.sprily
package btf.webjs

import scalacss.Color
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(10.px))
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

object BrushTheme extends StyleSheet.Inline {
  import dsl._

  val title = style(
    color(Color.white),
    backgroundColor(Color.black),
    addClassName("text-center")
  )
}
