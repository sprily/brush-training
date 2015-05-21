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

  type Degrees = Double

  case class GaugeLayout(
      majorTicks: Int, minorTicks: Int,
      minValue: Double, maxValue: Double) {

    def degrees(scaled: Double) = {
      (Math.max(Math.min(scaled, maxValue), minValue) - minValue) / (maxValue - minValue) * 90.0 - 45.0
    }

  }

  case class DataConfig(unitLabel: String, scaleBy: Double) {
    def scale(raw: Double) = raw * scaleBy
  }

  case class Gauge(layout: GaugeLayout, config: DataConfig)

  val corner = ReactComponentB[(Gauge,Double)]("Gauge")
    .render { S => {
      val (gauge, data) = S
        <.div(
          ^.width := "50%", ^.height := "50%",
          <.svg.svg(
            ^.svg.x := "0px",
            ^.svg.y := "0px",
            ^.svg.viewBox := "0 0 258.336 258.336",
            Face(),
            Layer1(),
            Dial(gauge.layout),
            Arm(gauge.layout.degrees(gauge.config.scale(data))),
            Frame(),
            ArmLid(),
            InnerShadow(),
            Layer9(),
            OuterFrame()
          )
        )
      }
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
    .shouldComponentUpdate { case _ => false }
    .buildU

  val Layer1 = ReactComponentB[Unit]("Layer1")
    .render { _ =>
      <.svg.g(
        ^.svg.id := "Layer_1",

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
    .shouldComponentUpdate { case _ => false }
    .buildU

  val Arm = ReactComponentB[Degrees]("Arm")
    .render { theta =>
      <.svg.g(

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
        ^.svg.id := "arm",
        ^.svg.transform := s"rotate($theta, 190.936, 190.936)",
        <.svg.path(
          ^.svg.d := "M216.442,190.926c0-14.114-11.441-25.556-25.556-25.556c-4.912,0-9.499,1.389-13.394,3.791L84.721,84.007l-9.089-9.089 l-0.356,0.357l133.701,133.702C213.59,204.354,216.442,197.973,216.442,190.926z"
        ),
        <.svg.path(
          ^.svg.fill := "#1E1E1E",
          ^.svg.d := "M75.275,75.275l-0.351,0.35l9.071,9.071l85.115,92.852c-2.396,3.892-3.78,8.473-3.78,13.378 c0,14.114,11.441,25.556,25.556,25.556c7.067,0,13.464-2.869,18.09-7.505L75.275,75.275z"
        ),
        <.svg.line(
          ^.svg.fill := "none",
          ^.svg.stroke := "#464646",
          ^.svg.strokeWidth := "0.5",
          ^.svg.strokeMiterlimit := "10",
          ^.svg.x1 := "181.681",
          ^.svg.y1 := "181.681",
          ^.svg.x2 := "75.278",
          ^.svg.y2 := "75.271"
        )
      )
    }
    .build

  val Frame = ReactComponentB[Unit]("Frame")
    .render { _ =>
      <.svg.g( ^.svg.id := "frame",
        <.svg.g(
          <.svg.polygon( ^.svg.fill := "#E9E4ED", ^.svg.points := "234.345,23.991 248.336,10 10,10 23.991,23.991"),
          <.svg.polygon( ^.svg.fill := "#BCBBC3", ^.svg.points := "23.991,23.991 10,10 10,248.336 23.991,234.345"),
          <.svg.polygon( ^.svg.fill := "#F3EEF3", ^.svg.points := "23.991,234.345 10,248.336 248.336,248.336 234.345,234.345"),
          <.svg.polygon( ^.svg.fill := "#FBF6FB", ^.svg.points := "234.345,23.991 234.345,234.345 248.336,248.336 248.336,10")
          )
        )
    }
    .shouldComponentUpdate { case _ => false }
    .buildU

  val ArmLid = ReactComponentB[Unit]("ArmLid")
    .render { _ =>
      <.svg.g( ^.svg.id := "arm_lid",
        <.svg.path( ^.svg.fill := "#F2EDF2", ^.svg.d := "M181.681,181.681l52.663,52.664h0.001v-77.876c-13.22,0-25.667,3.298-36.572,9.11L181.681,181.681z"),
        <.svg.path( ^.svg.fill := "#F2EDF2", ^.svg.d := "M165.556,197.816c-5.797,10.895-9.086,23.326-9.086,36.528h77.875l-52.663-52.664L165.556,197.816z"),
        <.svg.path( ^.svg.fill := "#F6F2F7", ^.svg.d := "M234.345,147.502c-14.742,0-28.623,3.678-40.783,10.159l4.211,7.918 c10.905-5.812,23.353-9.11,36.572-9.11V147.502z"),
        <.svg.path( ^.svg.fill := "#F7F2F7", ^.svg.d := "M157.635,193.61c-6.465,12.149-10.133,26.013-10.133,40.734h8.967c0-13.202,3.289-25.634,9.086-36.528 L157.635,193.61z")
      )
    }
    .shouldComponentUpdate { case _ => false }
    .buildU

  val InnerShadow = ReactComponentB[Unit]("InnerShadow")
    .render { _ =>
      <.svg.g(
        ^.svg.id := "inner_shadow",
        <.svg.polygon(
          ^.svg.fill := "#F5F5F5",
          ^.svg.points := "234.345,34.46 234.345,23.991 23.991,23.991 23.991,234.345 34.46,234.345 34.46,34.46"
        )
      )
    }
    .shouldComponentUpdate { case _ => false }
    .buildU

  val Layer9 = ReactComponentB[Unit]("Layer9")
    .render { _ =>
      <.svg.g( ^.svg.id := "Layer_9",
        <.svg.radialgradient( ^.svg.id := "SVGID_2_", ^.svg.cx := "129.168", ^.svg.cy := "129.168", ^.svg.r := "169.2006", ^.svg.gradientUnits := "userSpaceOnUse",
          <.svg.stop(^.svg.offset := "0", ^.svg.stopColor := "#FFFFFF", ^.svg.stopOpacity := 0),
          <.svg.stop(^.svg.offset := "0.9725", ^.svg.stopColor := "#292526", ^.svg.stopOpacity := 0.1167),
          <.svg.stop(^.svg.offset := "1", ^.svg.stopColor := "#000000", ^.svg.stopOpacity := 0.12)
        ),
        <.svg.polygon(^.svg.fill := "url(#SVGID_2_)", ^.svg.points := "248.336,248.336 10,10 248.336,10")
      )
    }
    .shouldComponentUpdate { case _ => false }
    .buildU

  val OuterFrame = ReactComponentB[Unit]("OuterFrame")
    .render { _ =>
      <.svg.g(^.svg.id := "outer_frame",
        <.svg.path(^.svg.d := "M248.336,10v238.336H10V10H248.336 M258.336,0h-10H10H0v10v238.336v10h10h238.336h10v-10V10V0L258.336,0z")
      )
    }
    .shouldComponentUpdate { case _ => false }
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
  val Dial = ReactComponentB[GaugeLayout]("Dial")
    .render { layout => {
      val major = layout.majorTicks
      val minor = layout.minorTicks
      val minValue = layout.minValue
      val maxValue = layout.maxValue

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

      )
    }
  }
  .shouldComponentUpdate { case (self,layout,_) => layout != self.props }
  .build

}
