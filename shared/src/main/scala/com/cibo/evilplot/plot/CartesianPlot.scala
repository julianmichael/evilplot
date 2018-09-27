package com.cibo.evilplot.plot

import com.cibo.evilplot.colors.Color
import com.cibo.evilplot.geometry.{Drawable, EmptyDrawable, Extent, LineStyle}
import com.cibo.evilplot.numeric.{Bounds, BoxPlotSummaryStatistics, Datum2d, Point}
import com.cibo.evilplot.plot.LinePlot.LinePlotRenderer
import com.cibo.evilplot.plot.ScatterPlot.ScatterPlotRenderer
import com.cibo.evilplot.plot.aesthetics.Theme
import com.cibo.evilplot.plot.renderers.BoxRenderer.BoxRendererContext
import com.cibo.evilplot.plot.renderers._

object CartesianPlot {

  type ContextToDrawable[X <: Datum2d[X]] = CartesianDataRenderer[X] => PlotContext => PlotRenderer

  def apply[X <: Datum2d[X]](
    data: Seq[X],
    xboundBuffer: Option[Double] = None,
    yboundBuffer: Option[Double] = None,
    legendContext: LegendContext = LegendContext()
  )(contextToDrawable: ContextToDrawable[X]*)(implicit theme: Theme): Plot = {

    val (xbounds, ybounds) =
      PlotUtils.bounds(data, theme.elements.boundBuffer, xboundBuffer, yboundBuffer)

    val cartesianDataRenderer = CartesianDataRenderer(data)

    Plot(
      xbounds,
      ybounds,
      CompoundPlotRenderer(
        contextToDrawable.map(x => x(cartesianDataRenderer)),
        xbounds,
        ybounds,
        legendContext
      )
    )
  }
}

case class CartesianDataRenderer[X <: Datum2d[X]](data: Seq[X]) {

  def manipulate(x: Seq[X] => Seq[X]): Seq[X] = x(data)

  def filter(x: X => Boolean): CartesianDataRenderer[X] = this.copy(data.filter(x))

  def scatter(pointToDrawable: X => Drawable, legendCtx: LegendContext = LegendContext.empty)(
    pCtx: PlotContext)(implicit theme: Theme): PlotRenderer = {
    ScatterPlotRenderer(data, PointRenderer.custom(pointToDrawable, Some(legendCtx)))
  }

  def scatter(pCtx: PlotContext)(implicit theme: Theme): ScatterPlotRenderer[X] = {
    ScatterPlotRenderer(data, PointRenderer.default())
  }

  def scatter(pointRenderer: PointRenderer[X])(pCtx: PlotContext)(
    implicit theme: Theme): ScatterPlotRenderer[X] = {
    ScatterPlotRenderer(data, pointRenderer)
  }

  def line(
    strokeWidth: Option[Double] = None,
    color: Option[Color] = None,
    label: Drawable = EmptyDrawable(),
    lineStyle: Option[LineStyle] = None,
    legendCtx: LegendContext = LegendContext.empty
  )(pCtx: PlotContext)(implicit theme: Theme): PlotRenderer = {
    LinePlotRenderer(data, PathRenderer.default(strokeWidth, color, label, lineStyle))
  }

  def line(pCtx: PlotContext)(implicit theme: Theme): PlotRenderer = {
    LinePlotRenderer(data, PathRenderer.default())
  }

  def line(pathRenderer: PathRenderer[X])(pCtx: PlotContext)(
    implicit theme: Theme): PlotRenderer = {
    LinePlotRenderer(data, pathRenderer)
  }

}
