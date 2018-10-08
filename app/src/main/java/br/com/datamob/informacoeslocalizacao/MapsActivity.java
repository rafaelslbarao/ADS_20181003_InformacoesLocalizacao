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
    //
    private GoogleMap mMap;
    private AppCompatTextView tvDistancia;
    private AppCompatTextView tvArea;
    //
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
        /**
         * Só configura a tela se possuir as permissões
         */
        if (verificaPermissoesNecessarias())
        {
            iniciaAplicacao();
        }
    }

    private boolean verificaPermissoesNecessarias()
    {
        /**
         * Verifica se o app tem as permissões necessárias para utilizar o sistema
         */
        ArrayList<String> permissoesNecessarias = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissoesNecessarias.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissoesNecessarias.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        /**
         * Se já possui todas, retorna true
         */
        if (permissoesNecessarias.size() <= 0)
            return true;
        else
        {
            /**
             * Se não possui todas, chama método responsável por solicitar as permissões
             */
            String[] permissoes = new String[permissoesNecessarias.size()];
            ActivityCompat.requestPermissions(MapsActivity.this, permissoesNecessarias.toArray(permissoes), REQUEST_PERMISSOES);
            return false;
        }
    }

    /**
     * Método disparado após solicitar permissões. Funcionamento pareceido com o OnActivityResult
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_PERMISSOES:
                /**
                 * Verifica se todas as permissões foram autorizadas
                 */
                for (int result : grantResults)
                {
                    if (result != PackageManager.PERMISSION_GRANTED)
                    {
                        /**
                         * Se alguma permissão não foi autorizada, avisa o usuário e fecha a tela
                         */
                        Toast.makeText(context, R.string.FalhaPermissoes, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                //
                /**
                 * Se todas as permissões foram dadas, continuar normalmente
                 */
                iniciaAplicacao();
                break;
        }
    }

    private void iniciaAplicacao()
    {
        setContentView(R.layout.activity_maps);
        //
        /**
         * Inicia referência para o mapa
         */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mMap);
        if (mapFragment != null)
        {
            mapFragment.getMapAsync(this);
        }
        //
        tvArea = findViewById(R.id.tvArea);
        tvDistancia = findViewById(R.id.tvDistancia);
        //
        iniciaLeituraGPS();

    }

    /**
     * Método disparado quando o mapa está pronto para ser utilizado
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        // Configura o tipo do mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // Adiciona evento para o click do mapa
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng position)
            {
                // A cada click um marcador é adicionado e é refeito o controle dos tipos de objetos exibidos e também dos calculos
                listPontos.add(position);
                adicionaMarcador(position);
                controlaObjeto();
                calculaMedidas();
            }
        });
        // Adiciona evento para o click nos marcadores
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker)
            {
                //Verifica se é o marcador de posição e se for não faz nada
                if (marker.equals(myLocation))
                    return true;
                // Se não for, remover o marcador e refaz os calculos
                removeMarker(marker);
                calculaMedidas();
                return true;
            }
        });
        // Adiciona evento para o click nos polígonos
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener()
        {
            @Override
            public void onPolygonClick(Polygon polygon)
            {
                // Quando clicado, o poligono é removido
                removePolygon();
            }
        });
        // Adiciona evento para o click na linhas
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {
                //Quando clicada a linha é removida
                removePolyline();
            }
        });
    }

    /**
     * Cria o marcador para a posição.
     * @param position
     */
    private void adicionaMarcador(LatLng position)
    {
        MarkerOptions markerOptions = new MarkerOptions();
        //Seta um ícone
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        //Seta a posição
        markerOptions.position(position);
        //Seta se o marcador pode ser arrastado
        markerOptions.draggable(false);
        //Adiciona um novo marcador no mapa
        Marker marker = mMap.addMarker(markerOptions);
        listMarker.add(marker);
    }

    /**
     * Cria os objetos no mapa. Se possui 2 pontos, cria-se panes uma linha. Se possuir mais de 2 pontos, cria0-se um poligono
     */
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
                //Adiciona todos os pontos ao poligono
                polygonOptions.addAll(listPontos);
                //Define a cor da linha
                polygonOptions.strokeColor(Color.BLACK);
                //Define a cor de preenchimento
                polygonOptions.fillColor(Color.WHITE);
                // Define a espessura
                polygonOptions.strokeWidth(3);
                // Define se é clicavel
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
            //Adiciona todos os pontos ao poligono
            polylineOptions.addAll(listPontos);
            //Define a cor da linha
            polylineOptions.color(Color.BLACK);
            // Define a espessura
            polylineOptions.width(3);
            // Define se é clicavel
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
        // Remove linha
        mPolyline.remove();
        mPolyline = null;
        limpaMarcadores();
        calculaMedidas();
    }

    private void removePolygon()
    {
        //Remove poligono
        mPolygon.remove();
        mPolygon = null;
        limpaMarcadores();
        calculaMedidas();
    }

    private void limpaMarcadores()
    {
        //Remove marcadores
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
        /**
         * Calcula distancia de todos os pontos e soma
         */
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
        if (distancia > 0)
            tvDistancia.setText("Distância: " + String.valueOf(distancia) + "m");
        else
            tvDistancia.setText("");
        //
        /**
         * Calcula área do polígono
         */
        double area = SphericalUtil.computeArea(listPontos);
        if (area > 0)
            tvArea.setText("Área: " + String.valueOf(area) + "m2");
        else
            tvArea.setText("");
    }

    /**
     * Objeto responsável por receber as atualizações de localização
     */
    private LocationCallback mLocationCallback = new LocationCallback()
    {
        @Override
        public void onLocationResult(LocationResult locationResult)
        {
            if (locationResult == null)
                return;
            //Utiliza a última posição válida
            atualizaMeuLocal(locationResult.getLastLocation());
        }
    };

    private void iniciaLeituraGPS()
    {
        /**
         * Configura a solicitação
         */
        LocationRequest mLocationRequest = new LocationRequest();
        // Intervalo mínimo de 1000 milisegundo para a solicitação explícita
        mLocationRequest.setInterval(1000);
        // Intervalo mínimo de 1000 milisegundos para a solicitação implícita
        mLocationRequest.setFastestInterval(1000);
        // Configura como máxima prioridade
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Pega instância do provider de localização (responsável por fazer o tratamento de localização e uso dos hardwares responsáveis
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Confirma se o app possui as permissões necessárias
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        //
        // Registra a solicitação passando as configurações e o responsável por receber as informações atualizadas
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }


    /**
     * Cria um marcador para representar a posição atual
     * Só cria se já não existir, caso contrário, apenas atualiza posição do marcador
     * @param lastLocation
     */
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}
