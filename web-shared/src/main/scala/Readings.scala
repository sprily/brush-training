package uk.co.sprily
package btf.webshared

trait PanelReadings {
  def current: Int
  def activePower: Int
  def reactivePower: Int
  def powerFactor: Int
  def voltage: Int
  def frequency: Int
}

case class GridReadings(
    current: Int,
    activePower: Int,
    reactivePower: Int,
    powerFactor: Int,
    voltage: Int,
    frequency: Int) extends PanelReadings

case class GeneratorReadings(
    current: Int,
    activePower: Int,
    reactivePower: Int,
    powerFactor: Int,
    voltage: Int,
    frequency: Int) extends PanelReadings

object GridReadings {
  def init = GridReadings(0,0,0,0,0,0)
}

object GeneratorReadings {
  def init = GeneratorReadings(0,0,0,0,0,0)
}
