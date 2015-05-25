package uk.co.sprily
package btf.webjs

import scala.collection.mutable.{Map => MMap}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ScalazReact._
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

trait gauges {

  type Degrees = Double

  case class GaugeLayout(
      majorTicks: Int, minorTicks: Int,
      minValue: Double, maxValue: Double, labelPrecision: Int,
      unitLabel: String, gaugeLabel: String,
      scaleBy: Double) {

    def angleFor(raw: Double) = degrees(scale(raw))

    private def degrees(scaled: Double) = {
      (Math.max(Math.min(scaled, maxValue), minValue) - minValue) / (maxValue - minValue) * 90.0 - 45.0
    }

    private def scale(raw: Double) = raw * scaleBy
  }

  case class Gauge(layout: GaugeLayout)


  // Fixed layout for Power Factor Gauge
  case object PFGauge {
    def layout = GaugeLayout(majorTicks =  7,
                             minorTicks =  3,
                             minValue   =  0.2,
                             maxValue   =  1.0,
                             labelPrecision  =  1,
                             unitLabel = "cos φ",
                             gaugeLabel = "Power Factor",
                             scaleBy = 1.0)
  }

  object Gauge {
    def apply(gaugeLabel: String,
              unitLabel: String,
              minValue: Double,
              maxValue: Double,
              majorTicks: Int = 5,
              minorTicks: Int = 2,
              labelPrecision: Int = 0,
              scaleBy: Double = 1.0) = new Gauge(
      GaugeLayout(
        majorTicks = majorTicks,
        minorTicks = minorTicks,
        minValue = minValue,
        maxValue = maxValue,
        labelPrecision = labelPrecision,
        unitLabel = unitLabel,
        gaugeLabel = gaugeLabel,
        scaleBy = scaleBy)
    )
  }

  val powerFactor = ReactComponentB[(PFGauge.type,Double)]("PFGauge")
    .render { S => {
      val (gauge, data) = S
        <.div(
          <.div(grid.row,
            <.div(grid.col(12),
              <.svg.svg(
                ^.svg.x := "0px",
                ^.svg.y := "0px",
                ^.svg.viewBox := "0 0 258.336 258.336",
                Face(),
                UnitLabel(gauge.layout),
                Dial(gauge.layout),
                PFDialLabels(gauge.layout),
                Arm(gauge.layout.angleFor(data)),
                Frame(),
                ArmLid(),
                InnerShadow(),
                Layer9(),
                OuterFrame()
              )
            ),
            <.div(grid.row,
              <.div(grid.col(12),
                GaugeTitle(gauge.layout.gaugeLabel)
              )
            )
          )
        )
      }
    }
    .shouldComponentUpdate { case (self,gauge,_) => gauge != self.props }
    .build

  val corner = ReactComponentB[(Gauge,Double)]("Gauge")
    .render { S => {
      val (gauge, data) = S
        <.div(
          <.div(grid.row,
            <.div(grid.col(12),
              <.svg.svg(
                ^.svg.x := "0px",
                ^.svg.y := "0px",
                ^.svg.viewBox := "0 0 258.336 258.336",
                Face(),
                UnitLabel(gauge.layout),
                Dial(gauge.layout),
                DialLabels(gauge.layout),
                Arm(gauge.layout.angleFor(data)),
                Frame(),
                ArmLid(),
                InnerShadow(),
                Layer9(),
                OuterFrame()
              )
            ),
            <.div(grid.row,
              <.div(grid.col(12),
                GaugeTitle(gauge.layout.gaugeLabel)
              )
            )
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

  val UnitLabel = ReactComponentB[GaugeLayout]("UnitLabel")
    .render { layout =>
      <.svg.g(
        ^.svg.id := "UnitLabel",
        <.svg.text(
          ^.svg.transform := "matrix(1 0 0 1 36.1465 52.4482)",
          ^.svg.fontFamily := GaugeStyle.fontFamily,
          ^.svg.fontSize := "24.919",
          layout.unitLabel
        )
      )
    }
    .shouldComponentUpdate { case _ => false }
    .build

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
        ),
        ^.svg.opacity := 0.3
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

  val GaugeTitle = ReactComponentB[String]("GaugeLabel")
    .render { label => 
      <.p(
        label,
        ^.color := "white",
        ^.backgroundColor := "black",
        ^.cls := "text-center",
        ^.marginLeft := 15,
        ^.marginRight := 15)
    }
    .build

  object GaugeStyle {

    case class P(x: Double, y: Double)

    val padding = P(27.368, 27.368)
    val minorTickLength = 12.063
    val majorTickLength = minorTickLength + 9.167
    val radius = 163.555
    val majorRadius = radius - majorTickLength
    val minorRadius = radius - minorTickLength
    val regFont = 16.919
    val largeFont = 24.919
    //val fontFamily = "BebasNeueBold"
    val fontFamily = "Noto Sans"

    def angles(ticks: Int) =
      (0 to ticks).map { i => (Math.PI * i.toDouble) / (2.0 * ticks) }
                  .map { theta => (Math.cos(theta), Math.sin(theta)) }

    sealed trait TickLength { def radius: Double }
    case object LongTick extends TickLength { def radius = majorRadius }
    case object ShortTick extends TickLength { def radius = minorRadius }

    def tick(width: Double, length: TickLength)(cosTheta: Double, sinTheta: Double) =
      <.svg.line(
        ^.svg.fill := "none", ^.svg.stroke := "#000000", ^.svg.strokeWidth := width, ^.svg.strokeMiterlimit := "10",
        ^.svg.x1 := padding.x + radius*(1.0-cosTheta), ^.svg.x2 := padding.x + radius - length.radius*(cosTheta),
        ^.svg.y1 := padding.y + radius*(1.0-sinTheta), ^.svg.y2 := padding.y + radius - length.radius*(sinTheta)
      )

    def dialLabels(labels: IndexedSeq[String]) = {
      val majorAngles = angles(labels.length - 1)
      val majorMiddleIdx = labels.length / 2
      val hasMiddleLabel = labels.length % 2 == 1
      <.svg.g(
        labels.zip(majorAngles).zipWithIndex.map { case ((l, (cosTheta, sinTheta)), idx) => {
          val fontSize = if (hasMiddleLabel && idx == majorMiddleIdx) largeFont else regFont
          <.svg.text(
            ^.svg.fill := "#666666",
            ^.svg.fontFamily := fontFamily,
            ^.svg.fontSize := fontSize,
            ^.svg.x := padding.x + radius - (majorRadius)*cosTheta - (fontSize/4.0)*sinTheta,
            ^.svg.y := padding.y + radius + fontSize/3.0 - (majorRadius - fontSize/2.0)*sinTheta,
            l
          )
        }}
      )
    }

  }

  val DialLabels = ReactComponentB[GaugeLayout]("DialLabels")
    .render { layout => {
      import GaugeStyle._

      val major = layout.majorTicks
      val minValue = layout.minValue
      val maxValue = layout.maxValue
      val majorTicks = major + 1

      val labels = (0 to majorTicks).map { i => minValue + ((maxValue - minValue) / majorTicks * i) }
                                    .map { v => s"%-,.${layout.labelPrecision}f".format(v) }

      dialLabels(labels)

    }}
    .shouldComponentUpdate { case (self,layout,_) => layout != self.props }
    .build

  val PFDialLabels = ReactComponentB[GaugeLayout]("PFDialLabels")
    .render { layout => {
      import GaugeStyle._

      val major = layout.majorTicks / 2
      val minValue = layout.minValue
      val maxValue = layout.maxValue

      val values = 
        (0 to major).map { i => minValue + ((maxValue - minValue) / (major+1) * i) }

      val labels = (
        values ++ Seq(maxValue) ++ values.reverse
      ).map { v => s"%-,.${layout.labelPrecision}f".format(v) }

      <.svg.g(
        dialLabels(labels),

        <.svg.text(
          ^.svg.fill := "#666666",
          ^.svg.fontFamily := fontFamily,
          ^.svg.fontSize := regFont,
          ^.svg.x := padding.x + radius - majorRadius + regFont*2,
          ^.svg.y := padding.y + radius + regFont/3.0,
          "Lag."
        ),

        <.svg.text(
          ^.svg.fill := "#666666",
          ^.svg.fontFamily := fontFamily,
          ^.svg.fontSize := regFont,
          ^.svg.x := padding.x + radius - regFont/4.0,
          ^.svg.y := padding.y + radius + regFont/3.0 - majorRadius + regFont/2.0 + regFont*2,
          "Lead."
        )
      )

    }}
    .shouldComponentUpdate { case (self,layout,_) => layout != self.props }
    .build

  /** Draws the tick marks
    *
    * major is the number of bold ticks to place *between* the two endpoints.
    *
    * If the number of major ticks is odd, ie - there's a central major tick, then
    * it is rendered slightly differently to the other major ticks.
    *
    * TODO: there's space for reducing the amount that's recomputed here
    *
    */
  val Dial = ReactComponentB[GaugeLayout]("Dial")
    .render { layout => {
      import GaugeStyle._

      val major = layout.majorTicks
      val minor = layout.minorTicks
      val minValue = layout.minValue
      val maxValue = layout.maxValue

      val majorTicks = major + 1
      val minorTicks = majorTicks * (minor+1)
      val hasMiddleTick = majorTicks % 2 == 0
      val majorMiddleIdx = (majorTicks+1)/2
      val minorAngles = (0 to minorTicks).map { i => (Math.PI * i.toDouble) / (2.0 * minorTicks) }
                                         .map { theta => (Math.cos(theta), Math.sin(theta)) }

      val majorAngles = angles(majorTicks)

      val minorTick      = (tick(0.25, ShortTick) _).tupled
      val majorLongTick  = (tick(0.5,  LongTick) _).tupled
      val majorShortTick = (tick(2,    ShortTick) _).tupled

      <.svg.g(
        ^.svg.id := "dial",

        <.svg.g(
          ^.svg.id := "minor",
          minorAngles.map(minorTick)
        ),

        <.svg.g(
          ^.svg.id := "major-long",
          majorAngles.map(majorLongTick)
        ),

        <.svg.g(
          ^.svg.id := "major-short",
          if (hasMiddleTick) {
            majorAngles.zipWithIndex.filter(_._2 != majorMiddleIdx).map(_._1).map(majorShortTick)
          } else {
            majorAngles.map(majorShortTick)
          }
        )

      )
    }
  }
  .shouldComponentUpdate { case (self,layout,_) => layout != self.props }
  .build

}
