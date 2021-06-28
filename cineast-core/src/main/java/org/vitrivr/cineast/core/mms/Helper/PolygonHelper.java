package org.vitrivr.cineast.core.mms.Helper;

public class PolygonHelper {
    public boolean IsPointInPolygon( Point p, Point[] polygon )
    {
        double minX = polygon[ 0 ].x;
        double maxX = polygon[ 0 ].x;
        double minY = polygon[ 0 ].y;
        double maxY = polygon[ 0 ].y;
        for ( int i = 1 ; i < polygon.length ; i++ )
        {
            Point q = polygon[ i ];
            minX = Math.min( q.x, minX );
            maxX = Math.max( q.x, maxX );
            minY = Math.min( q.y, minY );
            maxY = Math.max( q.y, maxY );
        }

        if ( p.x < minX || p.x > maxX || p.y < minY || p.y > maxY )
        {
            return false;
        }

        // https://wrf.ecse.rpi.edu/Research/Short_Notes/pnpoly.html
        boolean inside = false;
        for ( int i = 0, j = polygon.length - 1 ; i < polygon.length ; j = i++ )
        {
            if ( ( polygon[ i ].y > p.y ) != ( polygon[ j ].y > p.y ) &&
                    p.x < ( polygon[ j ].x - polygon[ i ].x ) * ( p.y - polygon[ i ].y ) / ( polygon[ j ].y - polygon[ i ].y ) + polygon[ i ].x )
            {
                inside = !inside;
            }
        }

        return inside;
    }
}
