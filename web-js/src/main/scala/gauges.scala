package uk.co.sprily
package btf.webjs

import scala.collection.mutable.{Map => MMap}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

trait gauges {

  case class Gauge(
      majorTicks: Int, minorTicks: Int,
      minValue: Double, maxValue: Double)

  val corner = ReactComponentB[Gauge]("Gauge")
    .render { gauge =>
      <.div(
        ^.width := "50%", ^.height := "50%",
        <.svg.svg(
          ^.svg.x := "0px",
          ^.svg.y := "0px",
          ^.svg.viewBox := "0 0 258.336 258.336",
          Face(),
          Layer1(),
          dial(gauge.majorTicks, gauge.minorTicks, gauge.minValue, gauge.maxValue)
        )
      )
    }
    .shouldComponentUpdate { case (self,gauge,_) => gauge != self.props }
    .build

  val Face = ReactComponentB[Unit]("Face")
    .render { _ =>
      <.svg.g(
        ^.id := "face",
        <.svg.rect(
          ^.svg.x := "23.991",
          ^.svg.y := "23.991",
          ^.svg.fill := "#EBE6EB",
          ^.svg.width := "210.354",
          ^.svg.height := "210.354"
        )
      )
    }
    .buildU

  val Layer1 = ReactComponentB[Unit]("Layer1")
    .render { _ =>
      <.svg.g(
        ^.svg.id := "Layer_1",

        // Defines the fill for the meter's arm and ball
        <.svg.radialgradient(
          ^.svg.id := "SVGID_1_",
          ^.svg.cx := "144.686",
          ^.svg.cy := "146.7139",
          ^.svg.r := "70.7689",
          ^.svg.gradientUnits := "userSpaceOnUse",
          <.svg.stop(^.svg.offset := "0", ^.svg.stopColor := "#FFFFFF", ^.svg.stopOpacity := "0"),
          <.svg.stop(^.svg.offset := "0.9725", ^.svg.stopColor := "#292526", ^.svg.stopOpacity := "0.1167"),
          <.svg.stop(^.svg.offset := "1", ^.svg.stopColor := "#000000", ^.svg.stopOpacity := "0.12")
        ),

        // The following path is the arm and "ball" of the meter, it's fill is the 
        // gradient defined above
        // TODO: do we really need the above gradient defined, as the arm and ball are black, and so maybe are being overwritten later.
        <.svg.path(
          ^.svg.fill := "url(#SVGID_1_)",
          ^.svg.d := "M190.44,165.374c-4.911,0.085-9.474,1.554-13.325,4.024L82.871,85.876l-9.246-8.93l-0.351,0.363 l-0.345,0.356l9.229,8.911l86.723,91.352c-2.327,3.933-3.632,8.537-3.546,13.441c0.246,14.113,11.886,25.353,25.998,25.106 c7.066-0.124,13.411-3.104,17.955-7.819c4.532-4.703,7.273-11.133,7.15-18.179C216.192,176.367,204.553,165.127,190.44,165.374z"
        ),
        <.svg.text(
          ^.svg.transform := "matrix(1 0 0 1 36.1465 52.4482)",
          ^.svg.fontFamily := "'BebasNeueBold'",
          ^.svg.fontSize := "24.919", "V"
          //^.svg.fontSize := "24.919", "@(device.unitLabelFor(regAddr))"
        ),

        // TODO the following seems redundant
        <.svg.line(
          ^.svg.fill := "none",
          ^.svg.x1 := "197.772",
          ^.svg.y1 := "165.579",
          ^.svg.x2 := "193.562",
          ^.svg.y2 := "157.661"
        ),
        <.svg.text(
          ^.svg.transform := "matrix(1 0 0 1 192.3672 208.9766)",
          ^.svg.fontFamily := "'BebasNeueRegular'",
          ^.svg.fontSize := "20",
          "1,023"
        )
      )
    }
    .buildU

  case class P(x: Double, y: Double)

  /** Draws the tick marks
    *
    * major is the number of bold ticks to place between an endpoint and the *centre*
    * of the gauge -- this is to ensure there is always a middle point on the gauge.
    * minor is the number of regular ticks to place between each major tick
    *
    * TODO: there's space for reducing the amount that's recomputed here
    *
    */
  def dial(major: Int, minor: Int, minValue: Double, maxValue: Double) = {

    val padding = P(27.368, 27.368)
    val minorTickLength = 12.063
    val majorTickLength = minorTickLength + 9.167
    val radius = 163.555
    val minorRadius = radius - minorTickLength
    val majorRadius = radius - majorTickLength

    val majorTicks = 2*major + 2
    val minorTicks = majorTicks * (minor+1)
    val majorMiddleIdx = (majorTicks+1)/2
    val minorAngles = (0 to minorTicks).map { i => (Math.PI * i.toDouble) / (2.0 * minorTicks) }
                                       .map { theta => (Math.cos(theta), Math.sin(theta)) }

    val majorAngles = (0 to majorTicks).map { i => (Math.PI * i.toDouble) / (2.0 * majorTicks) }
                                       .map { theta => (Math.cos(theta), Math.sin(theta)) }

    val labels = (0 to majorTicks).map { i => minValue + ((maxValue - minValue) / (majorTicks) * i) }
                                  .map { v => "%-,.0f".format(v) }

    val regFont = 16.919
    val largeFont = 24.919

    <.svg.g(
      ^.svg.id := "dial",

      <.svg.g(
        ^.svg.id := "minor",
        minorAngles.map { case (cosTheta, sinTheta) =>
          <.svg.line(^.svg.fill := "none", ^.svg.stroke := "#000000", ^.svg.strokeWidth := "0.25", ^.svg.strokeMiterlimit := "10",
                     ^.svg.x1 := padding.x + radius*(1.0-cosTheta), ^.svg.x2 := padding.x + radius - minorRadius*(cosTheta),
                     ^.svg.y1 := padding.y + radius*(1.0-sinTheta), ^.svg.y2 := padding.y + radius - minorRadius*(sinTheta)
          )
        }
      ),

      <.svg.g(
        ^.svg.id := "major-long",
        majorAngles.map { case (cosTheta, sinTheta) =>
          <.svg.line(^.svg.fill := "none", ^.svg.stroke := "#000000", ^.svg.strokeWidth := "0.5", ^.svg.strokeMiterlimit := "10",
                     ^.svg.x1 := padding.x + radius*(1.0-cosTheta), ^.svg.x2 := padding.x + radius - majorRadius*(cosTheta),
                     ^.svg.y1 := padding.y + radius*(1.0-sinTheta), ^.svg.y2 := padding.y + radius - majorRadius*(sinTheta)
          )
        }
      ),

      <.svg.g(
        ^.svg.id := "major-short",
        majorAngles.zipWithIndex.filter(_._2 != majorMiddleIdx).map { case ((cosTheta, sinTheta), _) =>
          <.svg.line(^.svg.fill := "none", ^.svg.stroke := "#000000", ^.svg.strokeWidth := "2", ^.svg.strokeMiterlimit := "10",
                     ^.svg.x1 := padding.x + radius*(1.0-cosTheta), ^.svg.x2 := padding.x + radius - minorRadius*(cosTheta),
                     ^.svg.y1 := padding.y + radius*(1.0-sinTheta), ^.svg.y2 := padding.y + radius - minorRadius*(sinTheta)
          )
        }
      ),

      <.svg.g(
        ^.svg.id := "major-labels",
        labels.zip(majorAngles).zipWithIndex.map { case ((l, (cosTheta, sinTheta)), idx) => {
          val fontSize = if (idx == majorMiddleIdx) largeFont else regFont
          //val t = new org.scalajs.dom.raw.SVGTransform()
          //t.setTranslate(padding.x + majorRadius*(1.0-cosTheta), padding.y + majorRadius*(1.0-sinTheta))
          println(l, cosTheta, sinTheta)
          <.svg.text(
            ^.svg.fill := "#666666",
            ^.svg.fontFamily := "BebasNeueBold",
            ^.svg.fontSize := fontSize,
            ^.svg.x := padding.x + radius - (majorRadius)*cosTheta - (fontSize/4.0)*sinTheta,
            ^.svg.y := padding.y + radius + fontSize/3.0 - (majorRadius - fontSize/2.0)*sinTheta,
            l
          )
        }}
      )

      //  <text transform="matrix(1 0 0 1 52.5288 196.9092)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(rangeMin)</text>
      //  <text transform="matrix(1 0 0 1 83.8057 107.2637)" font-family="'BebasNeueBold'" font-size="24.919">@tickLabel(((rangeMax-rangeMin) / 2.0) + rangeMin)</text>
      //  <text transform="matrix(1 0 0 1 182.6504 66.2188)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(rangeMax)</text>
      //  <text transform="matrix(1 0 0 1 145.8237 71.5547)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(((rangeMax-rangeMin) / 6.0 * 5.0) + rangeMin)</text>
      //  <text transform="matrix(1 0 0 1 60.0923 163.3389)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(((rangeMax-rangeMin) / 6.0) + rangeMin)</text>
      //  <text transform="matrix(1 0 0 1 72.0459 132.3086)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(((rangeMax-rangeMin) / 3.0) + rangeMin)</text>
      //  <g>
      //    <text transform="matrix(1 0 0 1 118.5869 85.5166)" fill="#666666" font-family="'BebasNeueBold'" font-size="16.919">@tickLabel(((rangeMax-rangeMin) / 6.0 * 4.0) + rangeMin)</text>
      //  </g>
    )
  }

}
