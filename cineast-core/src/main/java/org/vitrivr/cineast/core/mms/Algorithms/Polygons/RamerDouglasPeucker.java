package org.vitrivr.cineast.core.mms.Algorithms.Polygons;

import org.vitrivr.cineast.core.mms.Helper.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ramer–Douglas–Peucker algorithm (RDP) is an algorithm for reducing the number of points in a
 * curve that is approximated by a series of points.
https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
 */
public class RamerDouglasPeucker {

    private RamerDouglasPeucker() { }

    private static final double sqr(double x) {
        return Math.pow(x, 2);
    }

    private static final double distanceBetweenPoints(Point a, Point b) {
        return sqr(a.x - b.x) + sqr(a.y - b.y);
    }

    private static final double distanceToSegmentSquared(Point a, Point b, Point c) {
        final double l2 = distanceBetweenPoints(b, c);
        if (l2 == 0)
            return distanceBetweenPoints(a, b);
        final double t = ((a.x - b.x) * (c.x - b.x) + (a.y - b.y) * (c.y - b.y)) / l2;
        if (t < 0)
            return distanceBetweenPoints(a, b);
        if (t > 1)
            return distanceBetweenPoints(a, c);

        Point np = new Point((b.x + t * (c.x - b.x)), (b.y + t * (c.y - b.y)));

        return distanceBetweenPoints(a, np);
    }

    private static final double perpendicularDistance(Point a, Point b, Point c) {
        return Math.sqrt(distanceToSegmentSquared(a,b,c));
    }

    private static final void douglasPeucker(List<Point> list, int s, int e, double epsilon, List<Point> resultList) {
        // Find the point with the maximum distance
        double dmax = 0;
        int index = 0;

        final int start = s;
        final int end = e-1;
        for (int i=start+1; i<end; i++) {
            // Point
            Point a = list.get(i);

            // Start
            Point b = list.get(start);

            // End
            Point c = list.get(end);
            final double d = perpendicularDistance(a, b, c);
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }
        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            // Recursive call
            douglasPeucker(list, s, index, epsilon, resultList);
            douglasPeucker(list, index, e, epsilon, resultList);
        } else {
            if ((end-start)>0) {
                resultList.add(list.get(start));
                resultList.add(list.get(end));
            } else {
                resultList.add(list.get(start));
            }
        }
    }

    /**
     * Given a curve composed of line segments find a similar curve with fewer points.
     *
     * @param list List of Double[] points (x,y)
     * @param epsilon Distance dimension
     * @return Similar curve with fewer points
     */
    public static final List<Point> douglasPeucker(List<Point> list, double epsilon) {
        final List<Point> resultList = new ArrayList<Point>();
        douglasPeucker(list, 0, list.size(), epsilon, resultList);
        return resultList;
    }

    public static void ramerDouglasPeucker(List<Point> pointList, double epsilon, List<Point> out) {

        if (pointList.size() < 2) throw new IllegalArgumentException("Not enough points to simplify");

        // Find the point with the maximum distance from line between the start and end
        double dmax = 0.0;
        int index = 0;
        int end = pointList.size() - 1;
        for (int i = 1; i < end; ++i) {
            double d = perpendicularDistance(pointList.get(i), pointList.get(0), pointList.get(end));
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            List<Point> recResults1 = new ArrayList<>();
            List<Point> recResults2 = new ArrayList<>();
            List<Point> firstLine = pointList.subList(0, index + 1);
            List<Point> lastLine = pointList.subList(index, pointList.size());
            ramerDouglasPeucker(firstLine, epsilon, recResults1);
            ramerDouglasPeucker(lastLine, epsilon, recResults2);

            // build the result list
            out.addAll(recResults1.subList(0, recResults1.size() - 1));
            out.addAll(recResults2);
            if (out.size() < 2) throw new RuntimeException("Problem assembling output");
        } else {
            // Just return start and end points
            out.clear();
            out.add(pointList.get(0));
            out.add(pointList.get(pointList.size() - 1));
        }
    }
}
