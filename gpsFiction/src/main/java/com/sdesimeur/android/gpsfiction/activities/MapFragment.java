package com.sdesimeur.android.gpsfiction.activities;


import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.BikeFlagEncoder;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.sdesimeur.android.gpsfiction.R;
import com.sdesimeur.android.gpsfiction.classes.GpsFictionData;
import com.sdesimeur.android.gpsfiction.classes.GpsFictionThing;
import com.sdesimeur.android.gpsfiction.classes.MyLocationListener;
import com.sdesimeur.android.gpsfiction.classes.PlayerBearingEvent;
import com.sdesimeur.android.gpsfiction.classes.PlayerBearingListener;
import com.sdesimeur.android.gpsfiction.classes.PlayerLocationEvent;
import com.sdesimeur.android.gpsfiction.classes.PlayerLocationListener;
import com.sdesimeur.android.gpsfiction.classes.Zone;
import com.sdesimeur.android.gpsfiction.classes.ZoneChangeListener;
import com.sdesimeur.android.gpsfiction.classes.ZoneSelectListener;
import com.sdesimeur.android.gpsfiction.classes.ZoneViewHelper;
import com.sdesimeur.android.gpsfiction.geopoint.MyGeoPoint;
import com.sdesimeur.android.gpsfiction.views.ImageViewWithId;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.theme.styles.LineStyle;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static android.view.ViewGroup.LayoutParams;
import static org.oscim.android.canvas.AndroidGraphics.drawableToBitmap;
import static org.oscim.layers.marker.MarkerSymbol.HotspotPlace;


public class MapFragment extends MyTabFragment implements PlayerBearingListener, ZoneChangeListener, PlayerLocationListener, ZoneSelectListener, ItemizedLayer.OnItemGestureListener<MarkerItem> {
    MapView mapView;
    Map mMap;
    MapPreferences mPrefs;
    //private MapPosition mapPosition = null;
    private static final int INITZOOMLEVEL = 14;
    private static final int SELECTEDBUTTON = 255;
    private static final int UNSELECTEDBUTTON = 100;
    //private Drawable vehiculeSelectedDrawable = null;
    private int vehiculeSelectedId = R.drawable.compass;
    private File mapsFolder;
    private File ghFolder;
    private GraphHopper hopper;
    //private GraphHopperAPI hopper;
    private String currentArea = "jeu";
    private volatile boolean shortestPathRunning = false;
    private volatile boolean shortestPathRunningFirst = false;
    private volatile boolean prepareInProgress = false;
    private MarkerItem playerMarkerItem;
    private ViewGroup viewGroupForVehiculesButtons = null;
    private Zone selectedZone = null;
    private MyGeoPoint playerLocation = null;
    private HashMap<Integer,FlagEncoder> vehiculeGHEncoding = new HashMap <Integer, FlagEncoder> () {{
        put(R.drawable.compass, null);
        put(R.drawable.pieton, new FootFlagEncoder());
        put(R.drawable.cycle, new BikeFlagEncoder());
        put(R.drawable.auto, new CarFlagEncoder());
    }};
    private MarkerSymbol playerMarkerSymbol = null;
    private HashMap<Zone,ZoneViewHelper> zoneViewHelperHashMap = null;
    private ItemizedLayer<MarkerItem> mMarkerLayer=null;
    private float playerBearing=0;
    private Drawable playerDrawable = null;
    private PathLayer routePathLayer;
//    private Bitmap playerBitmap = null;
//    private RotateDrawable playerRotateDrawable = null;

    public MapFragment() {
        super();
        //	this.setNameId(R.string.tabMapTitle);
    }

    public MapView getMapView() {
        return this.mapView;
    }

    private void finishPrepare() {
        prepareInProgress = false;
    }
    boolean isReady()
    {
        // only return true if already loaded
        if (hopper != null)
            return true;

        if (prepareInProgress)
        {
            return false;
        }
        return false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.vehiculeSelectedDrawable = this.getResources().getDrawable(R.drawable.compass);
        prepareInProgress = true;
            boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
            File dir = null;
            if (greaterOrEqKitkat) {
                //dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                dir = Environment.getExternalStorageDirectory();
            } else {
                dir = Environment.getExternalStorageDirectory();
            }
            dir = new File (dir, "/sdesimeur/");
            this.mapsFolder = new File (dir , "/mapsforge/");
            this.ghFolder = new File (dir , "/graphhopper/");
    }

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRootView(inflater.inflate(R.layout.map_view, container, false));
        vehiculeSelectedId=R.drawable.compass;
        mapView=(MapView) this.getRootView().findViewById(R.id.mapView);
        mMap = mapView.map();
        zoneViewHelperHashMap = new HashMap<>();
        playerDrawable = getResources().getDrawable(R.drawable.player_marker);
//        playerRotateDrawable = (RotateDrawable) getResources().getDrawable(R.drawable.playerrotatemarker);
//        playerBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.player_marker);
        MarkerSymbol ms = new MarkerSymbol(drawableToBitmap(getResources(),R.drawable.transparent), HotspotPlace.CENTER);
        mMarkerLayer = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), ms , this);
        mMap.layers().add(mMarkerLayer);
        mPrefs = new MapPreferences(MapFragment.class.getName(), this.getContext());
        MapFileTileSource tileSource = new MapFileTileSource();
        tileSource.setPreferredLanguage("en");
        File file = new File(mapsFolder, currentArea + ".map");
        if (tileSource.setMapFile(file.toString())) {
            VectorTileLayer l = mMap.setBaseMap(tileSource);
            mMap.setTheme(VtmThemes.DEFAULT);
            mMap.layers().add(new BuildingLayer(mMap, l));
            mMap.layers().add(new LabelLayer(mMap, l));
            mPrefs.clear();
            MapPosition pos = mMap.getMapPosition();
            pos.setZoomLevel(INITZOOMLEVEL);
            mMap.setMapPosition(pos);
        }
        ViewGroup vg = (ViewGroup) getRootView();
        addViewGroupForVehiculesButtons(vg);
        addViewGroupMapSCaleBar(vg);
        addViewGroupForZoomButtons(vg);
        loadGraphStorage();
        return getRootView();
    }

    private void registerAllZones() {
        Zone zone=null;
        Iterator<GpsFictionThing> itZone = this.getGpsFictionActivity().getGpsFictionData().getGpsFictionThing(Zone.class).iterator();
        while (itZone.hasNext()) {
            zone = (Zone) itZone.next();
            onZoneChanged(zone);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefs.load(mapView.map());
        mapView.onResume();
        getGpsFictionActivity().getMyLocationListener().addPlayerLocationListener(MyLocationListener.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getMyLocationListener().addPlayerBearingListener(MyLocationListener.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getGpsFictionData().addZoneSelectListener(GpsFictionData.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getGpsFictionData().addZoneChangeListener(this);
        registerAllZones();
    }

    @Override
    public void onPause() {
        super.onPause();
        getGpsFictionActivity().getMyLocationListener().removePlayerLocationListener(MyLocationListener.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getMyLocationListener().removePlayerBearingListener(MyLocationListener.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getGpsFictionData().removeZoneSelectListener(GpsFictionData.REGISTER.FRAGMENT, this);
        getGpsFictionActivity().getGpsFictionData().removeZoneChangeListener(this);
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        this.hopper = null;
        mMarkerLayer = null;
        zoneViewHelperHashMap = null;
        playerMarkerItem = null;
        // necessary?
        System.gc();
        super.onDetach();
    }

    void loadGraphStorage() {
        new AsyncTask<Void, Void, Path>() {
            String error = "Pas d'erreur";
            protected Path saveDoInBackground(Void... v) {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                hopper = tmpHopp;
                //if (vehiculeSelectedId == R.drawable.pieton) encoder = new FootFlagEncoder();
                //if (vehiculeSelectedId == R.drawable.cycle) encoder = new BikeFlagEncoder();
                //if (vehiculeSelectedId == R.drawable.auto) encoder = new CarFlagEncoder();
                //hopper.setEncodingManager(new EncodingManager(encoder));
                //hopper.setCHEnable(false);
                //hopper.setOSMFile(ghFolder + "/" + currentArea + "-gh/" + currentArea + ".pbf");
                //hopper.setDataReaderFile(ghFolder + "/" + currentArea + ".pbf");
                //hopper.setEncodingManager(new EncodingManager("FOOT,BIKE,CAR"));
                //hopper.setCHWeighting("fastest");
                //hopper.importOrLoad();
                hopper.load(new File(ghFolder, currentArea).getAbsolutePath());
                return null;
            }
            @Override
            protected Path doInBackground(Void... params) {
                try {
                    return saveDoInBackground(params);
                } catch (Throwable t) {
                    error = t.getMessage();
                    Log.e("AsynTask GraphHopper", error);
                    return null;
                }
            }
            protected void onPostExecute(Path o) {
                finishPrepare();
            }
        }.execute();
    }

    private void addViewGroupForZoomButtons(ViewGroup vg) {
        ZoomControls zoom = (ZoomControls) vg.findViewById(R.id.zoomControls);
        zoom.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapPosition pos = mMap.getMapPosition();
                pos.setZoomLevel(pos.getZoomLevel()+1);
                mMap.setMapPosition(pos);
            }
        });
        zoom.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapPosition pos = mMap.getMapPosition();
                pos.setZoomLevel(pos.getZoomLevel()-1);
                mMap.setMapPosition(pos);
            }
        });
    }

    private void onVehiculeChange(View v) {
            int res = ((ImageViewWithId) v).getDrawableId();
            if (vehiculeSelectedId != res) {
                vehiculeSelectedId = res;
                for (int position = 0; position < this.viewGroupForVehiculesButtons.getChildCount(); position++) {
                    View vi = this.viewGroupForVehiculesButtons.getChildAt(position);
                    int alpha = ((vi == v) ? MapFragment.SELECTEDBUTTON : MapFragment.UNSELECTEDBUTTON);
                    ((ImageView) vi).getDrawable().setAlpha(alpha);
                    vi.invalidate();
                }
            }
            calcPath();
    }

    private void addViewGroupForVehiculesButtons(ViewGroup vg) {
        viewGroupForVehiculesButtons = (ViewGroup) vg.findViewById(R.id.forVehiculesButtons);
        TypedArray vehicules = getResources().obtainTypedArray(R.array.vehicules_array);
        for (int index = 0; index < vehicules.length(); index++) {
            ImageViewWithId img = new ImageViewWithId(getActivity());
            int pad = getResources().getDimensionPixelSize(R.dimen.buttonsVehiculesPadding);
            img.setPadding(pad, pad, pad, pad);
            img.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onVehiculeChange(v);
                }
            });
            int res = vehicules.getResourceId(index, -1);
            img.setDrawableId(res);
            int alpha;
            //if ((playerLocation == null) || (selectedZone == null)) {
            //    alpha = (res==R.drawable.compass)?MapFragment.SELECTEDBUTTON :MapFragment.UNSELECTEDBUTTON;
            //} else {
                alpha = ((res == vehiculeSelectedId) ? MapFragment.SELECTEDBUTTON : MapFragment.UNSELECTEDBUTTON);
                calcPath();
            //}
            img.getDrawable().setAlpha(alpha);
            viewGroupForVehiculesButtons.addView(img);
        }
        vehicules.recycle();
    }

    private void addViewGroupMapSCaleBar(ViewGroup vg) {
        LinearLayout l = (LinearLayout) vg.findViewById(R.id.forScaleBar);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_LEFT | RelativeLayout.ALIGN_BOTTOM);
    }


    private List<GeoPoint> createRouteGeoPointList(GHResponse resp) {
        ArrayList<GeoPoint> listOfPoints = new ArrayList<>();
        List<PathWrapper> paths = resp.getAll();
        for (int i = 0; i < paths.size(); i++) {
            PointList pointsList = paths.get(i).getPoints();
            for (int j=0; j<pointsList.size(); j++)
                listOfPoints.add(new GeoPoint(pointsList.toGHPoint(j).getLat(), pointsList.toGHPoint(j).getLon()));
        }
        // TODO Auto-generated method stub
        return listOfPoints;
    }

    public void calcRoutePath(final double fromLat, final double fromLon, final double toLat, final double toLon ) {
        shortestPathRunning = true;
        new AsyncTask<Void, Void, GHResponse>() {
            float time;

            protected GHResponse doInBackground( Void... v ) {
                while ( ! (isReady()))  {}
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon);
                req.setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
                req.getHints().put("instructions", "true");
                req.getHints().put("calc_points", true);
                req.setLocale(Locale.FRENCH);
                req.setVehicle(vehiculeGHEncoding.get(vehiculeSelectedId).toString());
                req.setOptimize("true");
                //req.setWeighting("fastest");
                //hopper.getGraphHopperStorage();
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp;
            }

            protected void onPostExecute( GHResponse resp ) {
                if (!resp.hasErrors()) {
                    addRoute(createRouteGeoPointList(resp));
                    shortestPathRunningFirst = false;
                } else { }
                shortestPathRunning = false;
            }
        }.execute();
    }
    private void calcLinePath (){
        List<GeoPoint> listOfPoints = new ArrayList<>();
        listOfPoints.add(this.playerLocation);
        listOfPoints.add(this.selectedZone.getCenterPoint());
        addRoute(listOfPoints);
    }
    private void calcPath() {
        if ((playerLocation != null) && (selectedZone != null))
        if (vehiculeSelectedId == R.drawable.compass) {
            shortestPathRunningFirst = true;
            calcLinePath();
        } else {
            if (shortestPathRunningFirst) calcLinePath();
            final double fromLat = playerLocation.getLatitude();
            final double fromLon = playerLocation.getLongitude();
            final double toLat = selectedZone.getCenterPoint().getLatitude();
            final double toLon = selectedZone.getCenterPoint().getLongitude();
            if (! (shortestPathRunning)) calcRoutePath(fromLat, fromLon, toLat, toLon);
        }
    }

    private void addRoute(List<GeoPoint> newroute) {
        if (routePathLayer == null) {
            routePathLayer = new PathLayer(mMap,Color.TRANSPARENT);
            mMap.layers().add(routePathLayer);
        }
        routePathLayer.setPoints(newroute);
        int lineWidth=2;
        int lineColor = Color.BLUE;
        LineStyle ls = new LineStyle(lineColor, lineWidth, Paint.Cap.BUTT);
        routePathLayer.setStyle(ls);

    }

    @Override
    public void onZoneSelectChanged(Zone sZn) {
        // TODO Auto-generated method stub
        selectedZone = sZn;
        calcPath();
    }
    @Override
    public void onLocationPlayerChanged(PlayerLocationEvent playerLocationEvent) {
        // TODO Auto-generated method stub
        playerLocation = playerLocationEvent.getLocationOfPlayer();
        if (playerLocation != null) {
            if (playerMarkerItem != null) {
//                mMarkerLayer.removeItem(playerMarkerItem);
                playerMarkerItem.geoPoint=playerLocation;
            } else {
                playerMarkerItem = new MarkerItem("Player", "", playerLocation);
                playerMarkerSymbol = new MarkerSymbol(drawableToBitmap(playerDrawable), HotspotPlace.CENTER, false);
                playerMarkerItem.setMarker(playerMarkerSymbol);
                mMarkerLayer.addItem(playerMarkerItem);
            }
            mMarkerLayer.populate();
//            this.playerMarkerItem = new PlayerRotatingMarker(this.playerLocation, getResources(), R.drawable.player_marker);
                //this.playerMarker = new PlayerRotatingMarker  (playerPosition);
                //this.playerMarker.setResource(getResources(), R.drawable.player_marker);
                //this.playerMarker.register(this.getGpsFictionActivity());
            MapPosition pos = mMap.getMapPosition();
            pos.setPosition(playerLocation);
            mMap.setMapPosition(pos);
            if (selectedZone != null) updateCalcPath();
        }
    }

    private void updateCalcPath() {
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        if (item != playerMarkerItem) {
            Zone zn = (Zone) (item.getUid());
            zn.setVisible(!zn.isVisible());
        }
        return true;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        if (item != playerMarkerItem) {
            Zone zn = (Zone) (item.getUid());
            getGpsFictionActivity().getGpsFictionData().setSelectedZone(zn);
        }
        return true;
    }

    @Override
    public void onZoneChanged(Zone zone) {
        MarkerSymbol zoneMarkerSymbol = new MarkerSymbol(drawableToBitmap(getResources(),zone.getIconId()), HotspotPlace.CENTER);
        ZoneViewHelper zvh = zoneViewHelperHashMap.get(zone);
        if (zvh == null) {
            zvh = new ZoneViewHelper(zone);
            zoneViewHelperHashMap.put(zone,zvh);
        }
        if (zvh.markerItem == null) {
            zvh.markerItem = new MarkerItem(zone, zone.getName(), "", zone.getCenterPoint());
            mMarkerLayer.addItem(zvh.markerItem);
        }
        if (zvh.pathLayer == null) {
            zvh.pathLayer = new PathLayer(mMap,Color.TRANSPARENT);
            mMap.layers().add(zvh.pathLayer);
        }
        zvh.markerItem.setMarker(zone.isVisible()?zoneMarkerSymbol:null);
        mMarkerLayer.populate();

//        int lineWidth = (zone.isVisible()?2:0);
        int lineWidth=2;
        int lineColor = (zone.isVisible()?
                (zone.isSelectedZone()?Color.RED:Color.YELLOW):
                Color.TRANSPARENT);
        LineStyle ls = new LineStyle(lineColor, lineWidth, Paint.Cap.BUTT);
        zvh.pathLayer.setStyle(ls);
        if (zone.isVisible()) {
            List<GeoPoint> temp = zone.getShape().getAllGeoPoints();
            temp.add(temp.get(0));
            zvh.pathLayer.setPoints(temp);
        } else {
            zvh.pathLayer.clearPath();
        }
//        mMap.updateMap(true);
    }
    private Drawable getRotateDrawable(final Drawable d, final float angle) {
        final Drawable[] arD = { d };
        return new LayerDrawable(arD) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, d.getBounds().width() / 2, d.getBounds().height() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }

    public void onBearingPlayerChanged(PlayerBearingEvent playerBearingEvent) {
//        float playerBearingOld = playerBearing;
        playerBearing = (180+playerBearingEvent.getBearing())%360-180;
        MapPosition pos = mMap.getMapPosition();
        pos.setBearing(-playerBearing);
        mMap.setMapPosition(pos);

     /*   playerRotateDrawable.setFromDegrees(playerBearingOld);
        playerRotateDrawable.setToDegrees(playerBearing);
        playerRotateDrawable.setLevel(5);
        playerMarkerSymbol = new MarkerSymbol(drawableToBitmap(playerRotateDrawable), HotspotPlace.CENTER, false);
        */
     /*   Matrix matrix = new Matrix();
        matrix.postRotate(playerBearing);
        Bitmap bm = Bitmap.createBitmap(playerBitmap, 0, 0, playerBitmap.getWidth(), playerBitmap.getHeight(), matrix, true);
        Drawable d = new BitmapDrawable(bm);
        playerMarkerSymbol = new MarkerSymbol(drawableToBitmap(d), HotspotPlace.CENTER, false);
        */
        playerMarkerSymbol = new MarkerSymbol(drawableToBitmap(getRotateDrawable(playerDrawable,playerBearing)), HotspotPlace.CENTER, false);

        playerMarkerItem.setMarker(playerMarkerSymbol);
        mMarkerLayer.populate();
    }
}
