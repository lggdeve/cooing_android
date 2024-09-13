package com.example.google_map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    SupportMapFragment mapFragment;
    GoogleMap map;
    Button btnLocation, btnKor2Loc, btnKor3Loc;
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkDangerousPermissions();

        editText = findViewById(R.id.editText);
        btnLocation = findViewById(R.id.button1);
        btnKor2Loc = findViewById(R.id.button2);
        btnKor3Loc = findViewById(R.id.button3);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) { //맵지도 api
                Log.d(TAG, "onMapReady: ");
                map = googleMap;

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }

                new FetchCoordinatesTask().execute("http://cooing.dothome.co.kr/test.php");
            }
        });
        MapsInitializer.initialize(this);

        btnLocation.setOnClickListener(new View.OnClickListener() { //위치확인 버튼
            @Override
            public void onClick(View view) {
                requestMyLocation();
            }
        });

        btnKor2Loc.setOnClickListener(new View.OnClickListener() { //지도 검색 버튼
            @Override
            public void onClick(View view) {
                if(editText.getText().toString().length() > 0) {
                    Location location = getLocationFromAddress(getApplicationContext(), editText.getText().toString());
                    showCurrentLocation(location);
                }
            }
        });
        btnKor3Loc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //화면전환
                Intent intent = new Intent(MainActivity.this, write_update.class);
                startActivity(intent);
            }
        });

    }
    //주소 검색 요청
    private Location getLocationFromAddress(Context context, String address) {
        Geocoder geocoder = new Geocoder(context);
        List<Address> addresses;
        Location resLocation = new Location("");
        try {
            addresses = geocoder.getFromLocationName(address, 5);
            if((addresses == null) || (addresses.size() == 0)) {
                return null;
            }
            Address addressLoc = addresses.get(0);
            resLocation.setLatitude(addressLoc.getLatitude());
            resLocation.setLongitude(addressLoc.getLongitude());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resLocation;
    }
    //내 위치 요청
    private void requestMyLocation() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            long minTime = 1000;
            float minDistance = 0;
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    showCurrentLocation(location);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) { }

                @Override
                public void onProviderEnabled(String s) { }

                @Override
                public void onProviderDisabled(String s) { }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    //결과처리
    private void showCurrentLocation(Location location) {
        LatLng curPoint = new LatLng(location.getLatitude(), location.getLongitude());
        String msg = "Latitude : " + curPoint.latitude
                + "\nLongitude : " + curPoint.longitude;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 15));
    }

    private class FetchCoordinatesTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result.toString();
        }
        //PHP에서 데이터를   json으로 받아 배열에 저장
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArray = jsonObject.getJSONArray("data");

                Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mark);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(markerBitmap, 100, 100, false);
                BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    double latitude = obj.getDouble("m_latitude");
                    double longitude = obj.getDouble("m_longitude");

                    LatLng position = new LatLng(latitude, longitude);
                    map.addMarker(new MarkerOptions()
                            .position(position)
                            .title("Marker " + (i + 1))
                            .icon(customIcon));  // 커스텀 마커 리사이즈
                }

                if (jsonArray.length() > 0) {
                    JSONObject firstLocation = jsonArray.getJSONObject(0);
                    LatLng firstPosition = new LatLng(firstLocation.getDouble("m_latitude"), firstLocation.getDouble("m_longitude"));
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 10));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkDangerousPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE
        };

        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        for (int i = 0; i < permissions.length; i++) {
            permissionCheck = ContextCompat.checkSelfPermission(this, permissions[i]);
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                break;
            }
        }

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한 있음", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "권한 없음", Toast.LENGTH_LONG).show();
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                Toast.makeText(this, "권한 설명 필요함.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, permissions[i] + " 권한이 승인됨.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, permissions[i] + " 권한이 승인되지 않음.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
