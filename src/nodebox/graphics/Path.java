package nodebox.graphics;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * Base class for all geometric (vector) data.
 */
public class Path implements IGeometry, Colorizable {

    private Color fillColor = null;
    private Color strokeColor = null;
    private float strokeWidth = 1f;
    private Contour[] contours = null;
    private Contour currentContour = null;

    public Path() {
        fillColor = new Color();
        strokeColor = null;
        strokeWidth = 1f;
        contours = null;
        currentContour = null;
    }

    public Path(Path other) {
        fillColor = other.fillColor == null ? null : other.fillColor.clone();
        strokeColor = other.strokeColor == null ? null : other.strokeColor.clone();
        strokeWidth = other.strokeWidth;
        contours = new Contour[other.contours.length];
        for (int i = 0; i < other.contours.length; i++) {
            contours[i] = other.contours[i].clone();
        }
        // Set the current contour to the last contour.
        currentContour = contours[contours.length - 1];
    }

    public Path(Contour c) {
        this();
        add(c);
    }

    //// Create geometric objects ////

    public void rect(float x, float y, float width, float height) {
        float w2 = width / 2;
        float h2 = height / 2;
        addPoint(x - w2, y - h2);
        addPoint(x + w2, y - h2);
        addPoint(x + w2, y + h2);
        addPoint(x - w2, y + h2);
    }

    public void ellipse(float x, float y, float width, float height) {
        Ellipse2D.Float e = new Ellipse2D.Float(x, y, width, height);
        PathIterator iter = e.getPathIterator(null);
        float[] coords = new float[6];
        while (!iter.isDone()) {
            int type = iter.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                addPoint(coords[0], coords[1]);
            } else if (type == PathIterator.SEG_CUBICTO) {
                addPoint(new Point(coords[0], coords[1], Point.CURVE_DATA));
                addPoint(new Point(coords[2], coords[3], Point.CURVE_DATA));
                addPoint(new Point(coords[4], coords[5], Point.CURVE_TO));
            }
            iter.next();
        }
    }

    //// Container operations ////

    /**
     * Add the given contour. This will also make it active,
     * so all new drawing operations will operate on the given contour.
     * <p/>
     * The given contour is not cloned.
     *
     * @param c the contour to add.
     */
    public void add(Contour c) {
        if (contours == null) {
            contours = new Contour[1];
            contours[0] = c;
        } else {
            Contour[] newContours = new Contour[contours.length + 1];
            System.arraycopy(contours, 0, newContours, 0, contours.length);
            newContours[contours.length] = c;
            contours = newContours;
        }
        currentContour = c;
    }

    public int size() {
        return contours.length;
    }

    public void clear() {
        contours = null;
        currentContour = null;
    }

    /**
     * Get the contours of a geometry object.
     * <p/>
     * This method returns live references to the geometric objects.
     * Changing them will change the original geometry.
     *
     * @return a list of contours
     */
    public Contour[] getContours() {
        return contours;
    }

    //// Color operations ////

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setFill(Color c) {
        setFillColor(c);
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStroke(Color c) {
        setStrokeColor(c);
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    //// Point operations ////

    public int getPointCount() {
        if (contours == null) return 0;
        int pointCount = 0;
        for (Contour c : contours) {
            pointCount += c.getPointCount();
        }
        return pointCount;
    }

    /**
     * Get the points for this geometry.
     * <p/>
     * This returns a live reference to the points of the geometry. Changing the points will change the geometry.
     *
     * @return a list of Points.
     */
    public Point[] getPoints() {
        if (contours == null) return new Point[0];
        Point[] points = null;
        Point[] cPoints = null;
        for (Contour c : contours) {
            cPoints = c.getPoints();
            // Add them to the points array.
            if (points == null) {
                points = new Point[cPoints.length];
                System.arraycopy(cPoints, 0, points, 0, cPoints.length);
            } else {
                // Create a new array that can include the original points and the points from the contour.
                Point[] newPoints = new Point[points.length + cPoints.length];
                System.arraycopy(points, 0, newPoints, 0, points.length);
                System.arraycopy(cPoints, 0, newPoints, points.length, cPoints.length);
                points = newPoints;
            }
        }
        return points;
    }

    public void addPoint(Point pt) {
        ensureCurrentContour();
        currentContour.addPoint(pt);
    }

    public void addPoint(float x, float y) {
        ensureCurrentContour();
        currentContour.addPoint(x, y);
    }

    /**
     * Ensure that there is a contour available.
     */
    private void ensureCurrentContour() {
        if (currentContour != null) return;
        currentContour = new Contour();
        add(currentContour);
    }

    //// Geometric queries ////

//    public static Path load(InputStream is) {
//        throw new UnsupportedOperationException("Not implemented.");
//    }
//
//    public void save(OutputStream os) {
//        throw new UnsupportedOperationException("Not implemented.");
//    }

    public Rect getBounds() {
        if (contours.length == 0) return new Rect();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float px, py;
        for (Contour c : contours) {
            for (Point p : c.getPoints()) {
                px = p.getX();
                py = p.getY();
                if (px < minX) minX = px;
                if (py < minY) minY = py;
                if (px > maxX) maxX = px;
                if (py > maxY) maxY = py;
            }
        }
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    //// Transformations ////

    public void transform(Transform t) {
        t.map(getPoints());
    }

    //// Path math ////

    /**
     * Flatten the geometry.
     */
    public void flatten() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Make a flattened copy of the geometry.
     *
     * @return a flattened copy.
     */
    public Path flattened() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    //// Operations on the current context. ////

    public void draw(Graphics2D g) {
        // If we can't fill or stroke the path, there's nothing to draw.
        if (fillColor == null && strokeColor == null) return;
        int pointCount = getPointCount();
        GeneralPath gp = new GeneralPath(Path2D.WIND_EVEN_ODD, pointCount);
        for (Contour c : contours) {
            c._extendPath(gp);
        }
        if (fillColor != null) {
            g.setColor(fillColor.getAwtColor());
            g.fill(gp);
        }
        if (strokeWidth > 0 && strokeColor != null) {
            try {
                g.setColor(strokeColor.getAwtColor());
                g.setStroke(new BasicStroke(strokeWidth));
                g.draw(gp);
            } catch (Exception e) {
                // Invalid transformations can cause the pen to not display.
                // Catch the exception and throw it away.
                // The path would be too small to be displayed anyway.
            }
        }
    }

    public void inheritFromContext(GraphicsContext ctx) {
    }

    public Path clone() {
        return new Path(this);
    }
}