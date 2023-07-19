package com.example.weather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.URL;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {

    // Поля, что будут ссылаться на объекты из дизайна
    private EditText user_field;
    private Button main_btn;
    private Button english_btn;

    private Button russian_btn;
    private TextView result_info;

    private TextView cityInfo;
    private TextView temperatureInfo;
    private TextView feelsLikeInfo;


    // Переменная для хранения текущего языка
    private String currentLanguage = "en";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим SwipeRefreshLayout по его ID
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Устанавливаем обработчик для свайпа
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // В этом методе выполняем действия по обновлению данных
                // получение данных о погоде по местоположению
                getLocationWeather();

                // Завершаем обновление данных
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Устанавливаем ссылки на объекты из дизайна
        user_field = findViewById(R.id.user_field);
        main_btn = findViewById(R.id.main_btn);
        result_info = findViewById(R.id.result_info);

        cityInfo = findViewById(R.id.city);
        temperatureInfo = findViewById(R.id.temperature_info);
        feelsLikeInfo = findViewById(R.id.feels_like);

        // Получим местоположение и обновим текстовые элементы с данными о погоде
        getLocationWeather();

        // Новые кнопки для смены языка
        english_btn = findViewById(R.id.english_btn);
        russian_btn = findViewById(R.id.russian_btn);

        // Загрузка текущего языка
        currentLanguage = Locale.getDefault().getLanguage();



        // Обработчики нажатия на кнопки для смены языка
        english_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLocale(new Locale("en"));
                updateViews();
            }
        });

        russian_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLocale(new Locale("ru"));
                updateViews();
            }
        });

        // Обработчик нажатия на кнопку "main_btn"
        main_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Если ничего не ввели в поле, то выдаем всплывающую подсказку
                if(user_field.getText().toString().trim().equals(""))
                    Toast.makeText(MainActivity.this, R.string.no_user_input, Toast.LENGTH_LONG).show();
                else {
                    // Если ввели, то формируем ссылку для получения погоды
                    String city = user_field.getText().toString();
                    String key = "9cc5ebc7eaa7d1a94b306710e24f85b6";
                    String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + key + "&units=metric&lang=" + currentLanguage;

                    // Запускаем класс для получения погоды
                    new GetURLData().execute(url);
                }
            }
        });

        // Получаем местоположение пользователя
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Найдём кнопку для смены темы
        Button themeBtn = findViewById(R.id.theme_btn);
        // Установим обработчик нажатия для кнопки
        themeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleTheme();
            }
        });

        // Обновим вид кнопки в зависимости от текущей темы
        updateThemeButton();
    }

    // Метод для получения данных о погоде на основе текущего местоположения
    private void getLocationWeather() {
        // Проверяем наличие разрешения на доступ к местоположению
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Получаем экземпляр LocationManager
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                // Получаем последнее известное местоположение
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    // Запускаем класс для получения погоды по местоположению
                    new GetLocationWeatherData().execute(location);
                } else {
                    // Если последнее известное местоположение не доступно, запросим обновление местоположения
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            // При получении нового местоположения обновляем данные
                            // Запускаем класс для получения погоды по местоположению
                            new GetLocationWeatherData().execute(location);

                            // Удаляем обновление местоположения после первого получения
                            locationManager.removeUpdates(this);
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                    });
                }
            }
        } else {
            // Если разрешение не предоставлено, запрашиваем его у пользователя
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Запрос разрешения местоположения
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, получаем местоположение
                getLocationWeather();
            } else {
                // Разрешение не получено, выводим сообщение
                Toast.makeText(this, "Для получения погоды необходимо разрешить доступ к местоположению", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // Метод для получения местоположения
    private void getWeatherData() {
     //   String city = user_field.getText;
    }

    private void updateThemeButton() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            // Текущая светлая тема, установим фон кнопки для темной темы
            findViewById(R.id.theme_btn).setBackgroundResource(R.drawable.round_button_night);
            // Также можно установить другой цвет текста, если необходимо
            // findViewById(R.id.theme_btn).setTextColor(getResources().getColor(R.color.darkTextColor));
        } else {
            // Текущая темная тема, установим фон кнопки для светлой темы
            findViewById(R.id.theme_btn).setBackgroundResource(R.drawable.round_button);
            // Также можно установить другой цвет текста, если необходимо
            // findViewById(R.id.theme_btn).setTextColor(getResources().getColor(R.color.lightTextColor));
        }
    }

    private void toggleTheme() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            // Сейчас светлая тема, переключаем на темную
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // Сейчас темная тема, переключаем на светлую
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void updateViews() {
        // Обновляем текст на кнопках с учетом текущего языка
        if (currentLanguage.equals("en")) {
            english_btn.setText("English");
            russian_btn.setText("Русский");

        } else {
            english_btn.setText("Английский");
            russian_btn.setText("Русский");

        }
    }

    // Метод для установки локали приложения
    private void setLocale(Locale locale) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, displayMetrics);

        // Сохраняем выбранный язык в SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", locale.getLanguage());
        editor.apply();

        // Перезапускаем текущую активность для применения измененного языка
        recreate();
    }





    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class GetURLData extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            result_info.setText(R.string.waiting_message);
        }


        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    buffer.append(line).append("\n");

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();

                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }




        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
                Toast.makeText(MainActivity.this, R.string.error_getting_data, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(result);

                String temperature = getString(R.string.temperature_label) + jsonObject.getJSONObject("main").getDouble("temp") + " °C";
                String feelsLike = getString(R.string.feels_like) + jsonObject.getJSONObject("main").getDouble("feels_like") + " °C";
                String humidity = getString(R.string.humidity) + jsonObject.getJSONObject("main").getDouble("humidity") + "%";
                String windSpeed = getString(R.string.wind_speed) + jsonObject.getJSONObject("wind").getDouble("speed") + " " + getString(R.string.speed);

                result_info.setText(temperature + "\n" + feelsLike + "\n" + humidity + "\n" + windSpeed);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class GetLocationWeatherData extends AsyncTask<Location, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Location... locations) {
            if (locations.length == 0) {
                return null;
            }

            Location location = locations[0];
            String latitude = String.valueOf(location.getLatitude());
            String longitude = String.valueOf(location.getLongitude());
            String key = "9cc5ebc7eaa7d1a94b306710e24f85b6";
            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + key + "&units=metric&lang=" + currentLanguage;

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                java.net.URL apiUrl = new java.net.URL(url);
                connection = (HttpURLConnection) apiUrl.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    buffer.append(line).append("\n");

                return new JSONObject(buffer.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();

                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            if (result == null) {
                Toast.makeText(MainActivity.this, R.string.error_getting_data, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String city = result.getString("name");
                String temperature = getString(R.string.temperature_label) + result.getJSONObject("main").getDouble("temp") + " °C";
                String feelsLike = getString(R.string.feels_like) + result.getJSONObject("main").getDouble("feels_like") + " °C";

                // Обновляем элементы интерфейса напрямую
                TextView cityTextView = findViewById(R.id.city);
                TextView temperatureTextView = findViewById(R.id.temperature_info);
                TextView feelsLikeTextView = findViewById(R.id.feels_like);

                cityTextView.setText(city);
                temperatureTextView.setText(temperature);
                feelsLikeTextView.setText(feelsLike);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



}
