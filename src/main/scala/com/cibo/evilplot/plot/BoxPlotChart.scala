package com.cibo.evilplot.plot

import com.cibo.evilplot.colors.{Black, Blue, Color, White}
import com.cibo.evilplot.geometry.{Above, Align, BorderFillRect, Disc, Drawable, DrawableLaterMaker, EmptyDrawable, Extent, FlipY, Line, Rect, WrapDrawable}
import com.cibo.evilplot.layout.ChartLayout
import com.cibo.evilplot.numeric.{AxisDescriptor, BoxPlot}
import com.cibo.evilplot.plot.BoxPlotData.{AllPoints, BoxPlotPoints, NoPoints, OutliersOnly}
import com.cibo.evilplot.{StrokeStyle, Style, Utils}
import org.scalajs.dom.CanvasRenderingContext2D


// TODO: ggplot2 provides a `geom_jitter` which makes the outliers a bit easier to read off the plot.
// TODO: Continuous x option?

object BoxPlotData {
  sealed trait BoxPlotPoints
  case object AllPoints extends BoxPlotPoints
  case object OutliersOnly extends BoxPlotPoints
  case object NoPoints extends BoxPlotPoints
}

case class BoxPlotData[T](labels: Seq[T], distributions: Seq[Seq[Double]], drawPoints: BoxPlotPoints = AllPoints,
                          rectWidth: Option[Double] = None, rectSpacing: Option[Double] = None, rectColor: Color = Blue,
                          pointColor: Color = Black, pointSize: Double = 2.0) extends PlotData {
  require(labels.length == distributions.length)
  val numBoxes: Int = labels.length
  override def yBounds: Option[Bounds] = {
    val pointsFromAllDistributions: Seq[Double] = distributions.flatten
    Some(Bounds(pointsFromAllDistributions.min, pointsFromAllDistributions.max))
  }

  override def createPlot(extent: Extent, options: PlotOptions): Drawable = new BoxPlotChart(extent, this, options)
}

class BoxPlotChart[T](override val extent: Extent, data: BoxPlotData[T], options: PlotOptions) extends Drawable {
  val yAxisDrawBounds: Bounds = options.yAxisBounds.getOrElse(data.yBounds
    .getOrElse(throw new IllegalArgumentException))

  val (getRectWidth, getRectSpacing) = DiscreteChartDistributable
    .widthAndSpacingFunctions(data.numBoxes, data.rectWidth, data.rectSpacing)

  val yAxisDescriptor = AxisDescriptor(yAxisDrawBounds, options.numYTicks.getOrElse(10))

  private def createDiscs(pointsData: Seq[Double], vScale: Double): Drawable = {
    val points = for {point <- pointsData} yield Disc(data.pointSize, 0, (point - yAxisDrawBounds.min) * vScale)
    FlipY(points.group) transY ((yAxisDrawBounds.max - pointsData.max) * vScale - data.pointSize)
  }

  private val _drawable = {
    val xAxis = DiscreteChartDistributable.XAxis(data.labels, getRectWidth, getRectSpacing,
      label = options.xAxisLabel, drawAxis = options.drawXAxis)
    val yAxis = ContinuousChartDistributable
      .YAxis(yAxisDescriptor, label = options.yAxisLabel, drawTicks = options.drawYAxis)
    val topLabel = Utils.maybeDrawableLater(options.topLabel, (text: String) => Label(text))
    val rightLabel = Utils.maybeDrawableLater(options.rightLabel, (text: String) => Label(text, rotate = 90))
    def chartArea(extent: Extent): Drawable = {
      val _rectWidth = getRectWidth(extent)
      val _rectSpacing = getRectSpacing(extent)
      val vScale = extent.height / yAxisDrawBounds.range
      val xGridLines = DiscreteChartDistributable.VerticalGridLines(data.numBoxes, getRectWidth, getRectSpacing)(extent)
      val yGridLines = ContinuousChartDistributable.HorizontalGridLines(yAxisDescriptor, lineSpacing = 1000)(extent)
      val background = Rect(extent) filled options.backgroundColor
      val boxes = (for { distribution <- data.distributions
                        boxPlot = new BoxPlot(distribution)
                        box = new Box(yAxisDrawBounds, _rectWidth, vScale, boxPlot)
                        discs = data.drawPoints match {
                          case AllPoints => createDiscs(distribution, vScale)
                          case OutliersOnly => createDiscs(boxPlot.outliers, vScale)
                          case NoPoints => EmptyDrawable()
                        }
      } yield Align.center(box, discs).group).seqDistributeH(_rectSpacing) padLeft _rectSpacing / 2.0
      background behind xGridLines behind yGridLines behind boxes
    }

    new ChartLayout(extent = extent, preferredSizeOfCenter = extent * .8, center = new DrawableLaterMaker(chartArea),
      left = yAxis, bottom = xAxis, top = topLabel, right = rightLabel) titled(options.title.getOrElse(""), 20.0)
  }

  override def draw(canvas: CanvasRenderingContext2D): Unit = _drawable.draw(canvas)
}

private class Box(yBounds: Bounds, rectWidth: Double, vScale: Double, data: BoxPlot, strokeColor: Color = Blue)
  extends WrapDrawable {
  private val _drawable = {
    val rectangles = {
      val lowerRectangleHeight: Double = (data.middleQuantile - data.lowerQuantile) * vScale
      val upperRectangleHeight: Double = (data.upperQuantile - data.middleQuantile) * vScale
      StrokeStyle(strokeColor)(Style(White)
        (BorderFillRect(rectWidth, lowerRectangleHeight) below BorderFillRect(rectWidth, upperRectangleHeight)))
    }
    val upperWhisker = Line((data.upperWhisker - data.upperQuantile) * vScale, 2) rotated 90
    val lowerWhisker = Line((data.lowerQuantile - data.lowerWhisker) * vScale, 2) rotated 90
    val nudgeBoxY = (yBounds.max - data.upperWhisker) * vScale
    StrokeStyle(strokeColor)(Align.center(upperWhisker, rectangles, lowerWhisker).reduce(Above)) transY nudgeBoxY
  }
  override val drawable: Drawable = _drawable
}