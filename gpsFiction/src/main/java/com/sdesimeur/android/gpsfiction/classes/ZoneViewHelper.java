package com.sdesimeur.android.gpsfiction.classes;

import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.MarkerItem;

/**
 * Created by sam on 22/10/16.
 */

public class ZoneViewHelper {
    public MarkerItem markerItem;
    public PathLayer pathLayer;
    public Zone zone;

    public ZoneViewHelper(Zone zn) {
        zone = zn;
    }
}