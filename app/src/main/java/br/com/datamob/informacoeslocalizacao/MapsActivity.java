package br.com.datamob.informacoeslocalizacao;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback
{
    private Context context = MapsActivity.this;
    private GoogleMap mMap;
    private AppCompatTextView tvDistancia;
    private AppCompatTextView tvArea;
    private ArrayList<Marker> listMarker = new ArrayList<>();
    private ArrayList<LatLng> listPontos = new ArrayList<>();
    private Marker myLocation;
    private Polygon mPolygon;
    private Polyline mPolyline;
    //
    private FusedLocationProviderClient mFusedLocationClient;
    //
    private static final int REQUEST_PERMISSOES = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //
        if (verificaPermissoesNecessarias())
        {
            iniciaAplicacao();
        }
    }

    private boolean verificaPermissoesNecessarias()
    {
        ArrayList<String> permissoesNecessarias = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissoesNecessarias.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissoesNecessarias.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissoesNecessarias.size() <= 0)
            return true;
        else
        {
            String[] permissoes = new String[permissoesNecessarias.size()];
            ActivityCompat.requestPermissions(MapsActivity.this, permissoesNecessarias.toArray(permissoes), REQUEST_PERMISSOES);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_PERMISSOES:
                for (int result : grantResults)
                {
                    if (result != PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(context, R.string.FalhaPermissoes, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                //
                iniciaAplicacao();
                break;
        }
    }

    private void iniciaAplicacao()
    {
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mMap);
        //
        mapFragment.getMapAsync(this);
        //
        tvArea = findViewById(R.id.tvArea);
        tvDistancia = findViewById(R.id.tvDistancia);
        //
        iniciaLeituraGPS();

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng position)
            {
                listPontos.add(position);
                adicionaMarcador(position);
                controlaObjeto();
                calculaMedidas();
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker)
            {
                if (marker.equals(myLocation))
                    return true;
                //
                removeMarker(marker);
                calculaMedidas();
                return true;
            }
        });
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener()
        {
            @Override
            public void onPolygonClick(Polygon polygon)
            {
                removePolygon();
            }
        });
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {
                removePolyline();
            }
        });
    }


    private void adicionaMarcador(LatLng position)
    {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerOptions.position(position);
        markerOptions.draggable(false);
        Marker marker = mMap.addMarker(markerOptions);
        listMarker.add(marker);
    }

    private void controlaObjeto()
    {
        if (listMarker.size() > 2)
        {
            if (mPolyline != null)
            {
                mPolyline.remove();
                mPolyline = null;
            }
            //
            if (mPolygon == null)
            {
                PolygonOptions polygonOptions = new PolygonOptions();
                polygonOptions.addAll(listPontos);
                polygonOptions.strokeColor(Color.BLACK);
                polygonOptions.fillColor(Color.WHITE);
                polygonOptions.strokeWidth(3);
                polygonOptions.clickable(true);
                mPolygon = mMap.addPolygon(polygonOptions);
            }
            else
            {
                mPolygon.setPoints(listPontos);
            }
        }
        else if (listMarker.size() == 2)
        {
            if (mPolygon != null)
            {
                mPolygon.remove();
                mPolygon = null;
            }
            //
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(listPontos);
            polylineOptions.color(Color.BLACK);
            polylineOptions.width(3);
            polylineOptions.clickable(true);
            mPolyline = mMap.addPolyline(polylineOptions);
        }
        else if (listMarker.size() == 1)
        {
            if (mPolyline != null)
            {
                mPolyline.remove();
                mPolyline = null;
            }
        }
    }

    private void removePolyline()
    {
        mPolyline.remove();
        mPolyline = null;
        limpaMarcadores();
        calculaMedidas();
    }

    private void removePolygon()
    {
        mPolygon.remove();
        mPolygon = null;
        limpaMarcadores();
        calculaMedidas();
    }

    private void limpaMarcadores()
    {
        for (Marker marker : listMarker)
        {
            marker.remove();
        }
        listMarker.clear();
        listPontos.clear();
    }

    private void removeMarker(Marker marker)
    {
        marker.remove();
        listPontos.remove(listMarker.indexOf(marker));
        listMarker.remove(marker);
        controlaObjeto();
    }

    private void calculaMedidas()
    {
        double distancia = 0.0;
        if (listMarker.size() > 1)
        {
            Marker markerAnterior = null;
            for (Marker marker : listMarker)
            {
                float[] retorno = new float[1];
                if (markerAnterior != null)
                    Location.distanceBetween(marker.getPosition().latitude, marker.getPosition().longitude, markerAnterior.getPosition().latitude, markerAnterior.getPosition().longitude, retorno);
                distancia += retorno[0];
                markerAnterior = marker;
            }
        }
        //
        if (distancia > 0)
            tvDistancia.setText("Distância: " + String.valueOf(distancia) + "m");
        else
            tvDistancia.setText("");
        //
        double area = SphericalUtil.computeArea(listPontos);
        if (area > 0)
            tvArea.setText("Área: " + String.valueOf(area) + "m2");
        else
            tvArea.setText("");
    }

    private LocationCallback mLocationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            if (locationResult == null)
            {
                return;
            }
            atualizaMeuLocal(locationResult.getLastLocation());
        }
    };

    private void iniciaLeituraGPS()
    {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        //
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void atualizaMeuLocal(Location lastLocation)
    {
        if (myLocation == null)
        {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            markerOptions.position(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
            markerOptions.draggable(false);
            myLocation = mMap.addMarker(markerOptions);
        }
        else
            myLocation.setPosition(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));

    }
}
