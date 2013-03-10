package grif1252;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import spacewar2.shadows.Shadow;
import spacewar2.utilities.Position;
import spacewar2.utilities.Vector2D;

/**
 * Draws a line on the screen from one point to another
 * 
 * @author amy
 */
public class ColorLineShadow extends Shadow
{
	public static final Color DEFAULT_LINE_COLOR = Color.CYAN;
	public static final float DEFAULT_STROKE_WIDTH = 2f;
	Position startPoint, endPoint;
	Color lineColor;
	float strokeWidth;
	
	/**
	 * Draw a line segment from the starting point to the ending point
	 * 
	 * @param startPoint
	 *            starting point
	 * @param endPoint
	 *            end point
	 * @param startToFinish
	 *            vector pointing from start to end
	 */
	public ColorLineShadow(Position startPoint, Position endPoint)
	{
		this(startPoint, endPoint, DEFAULT_LINE_COLOR, DEFAULT_STROKE_WIDTH);
	}
	
	/**
	 * Draw a line segment from the starting point to the ending point
	 * 
	 * @param startPoint
	 *            starting point
	 * @param endPoint
	 *            end point
	 * @param startToFinish
	 *            vector pointing from start to end
	 * @Param color a color to make the line
	 */
	public ColorLineShadow(Position startPoint, Position endPoint, Color color)
	{
		this(startPoint, endPoint, color, DEFAULT_STROKE_WIDTH);
	}
	
	/**
	 * Draw a line segment from the starting point to the ending point
	 * 
	 * @param startPoint
	 *            starting point
	 * @param endPoint
	 *            end point
	 * @param startToFinish
	 *            vector pointing from start to end
	 * @Param color a color to make the line
	 * @Param strokeWidth how thick to make the line
	 */
	public ColorLineShadow(Position startPoint, Position endPoint, Color color, float strokeWidth)
	{	
		// height/width for the line segment comes from the vector
		super((int) Math.abs(new Vector2D(startPoint.getX() - endPoint.getX(), startPoint.getY() - endPoint.getY()).getXValue()),
				(int) Math.abs(new Vector2D(startPoint.getX() - endPoint.getX(), startPoint.getY() - endPoint.getY()).getYValue()));
		
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		
		lineColor = color;
		this.strokeWidth = strokeWidth;
	}
	
	@Override
	public Position getActualLocation()
	{
		return startPoint;
	}
	
	/**
	 * This handles the toroidal space wrapping internally. It probably shouldn't.
	 * 
	 * TODO: Fix it so it does the wrapping externally like the other shadows (but it is harder here)
	 */
	public void draw(Graphics2D graphics)
	{
		graphics.setColor(lineColor);
		graphics.setStroke(new BasicStroke(strokeWidth));
		
		graphics.drawLine((int) startPoint.getX(), (int) startPoint.getY(), (int) endPoint.getX(), (int) endPoint.getY());
	}
	
	/**
	 * Lines are always drawn
	 */
	public boolean isDrawable()
	{
		return true;
	}
	
	/**
	 * Change the line color
	 * 
	 * @param lineColor
	 */
	public void setLineColor(Color lineColor)
	{
		this.lineColor = lineColor;
	}
	
	/**
	 * Set the width of the line
	 * 
	 * @param strokeWidth
	 */
	public void setStrokeWidth(float strokeWidth)
	{
		this.strokeWidth = strokeWidth;
	}
	
}
