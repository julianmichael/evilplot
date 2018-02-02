package com.cibo.evilplot.plot2d

import com.cibo.evilplot.colors.{Color, ColorBar, Colors, DefaultColors}
import com.cibo.evilplot.geometry.{Disc, Drawable, Extent, Translate}
import com.cibo.evilplot.numeric.{Bounds, Point}

object Scatter {

  /** The default point function.
    * @param size The size of the point.
    * @param color A function to get the color of a point by index.
    */
  def defaultPointFunction(
    size: Double = 2.5,
    color: Int => Color = _ => DefaultColors.barColor
  )(index: Int): Drawable = Disc(size) filled color(index)

  private def renderScatter(pointFunction: Int => Drawable)(plot: Plot2D[Seq[Point]], extent: Extent): Drawable = {
    val xtransformer = plot.xtransform(plot, extent)
    val ytransformer = plot.ytransform(plot, extent)
    plot.data.zipWithIndex.map { case (point, index) =>
      val x = xtransformer(point.x)
      val y = ytransformer(point.y)
      Translate(pointFunction(index), x = x, y = y)
    }.group
  }

  /** Create a scatter plot from some data.
    * @param data The points to plot.
    * @param pointFunction A function to create a Drawble for each point to plot.
    * @return A Plot2D representing a scatter plot.
    */
  def apply(
    data: Seq[Point],
    pointFunction: Int => Drawable = defaultPointFunction()
  ): Plot2D[Seq[Point]] = {
    val xbounds = Bounds(data.minBy(_.x).x, data.maxBy(_.x).x)
    val ybounds = Bounds(data.minBy(_.y).y, data.maxBy(_.y).y)
    Plot2D[Seq[Point]](data, xbounds, ybounds, renderScatter(pointFunction))
  }
}
