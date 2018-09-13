# Co to jest?
AdaptiveVideoStreaming to aplikacja stworzona na potrzeby pracy magisterskiej. Odnosi się ona do tematyki strumieniowania wideo w czasie rzeczywistym z adaptacją strumienia do aktualnie panujących warunków sieciowych. Aplikacja oparta jest o bibliotekę [libstreaming](https://github.com/fyhertz/libstreaming).
# Zasada działania
W aplikację zaimplementowano klasę BitrateAdapter, która jest odpowiedzialna za okresowy pomiar prędkości wysyłania poprzez przesył pliku jpg na serwer HTTP, oraz oszacowanie na podstawie tej wielkości optymalnej prędkości bitowej strumienia. Aplikacja na bieżąco wyświetla parametry połączenia oraz informację o ewentualnej zmianie prędkości bitowej. Aplikacja umożliwia strumieniowanie z przedniej bądź tylnej kamery, dodatkowo zaimplementowano w niej funkcję zoomu oraz autofocusu.
# Czego potrzebujesz?
Do odpowiedniego działania aplikacji niezbędny jest serwer RTSP (aplikacja testowana na serwerze Wowza Media Engine), oraz serwer HTTP (np. Apache), na którym należy umieścić prosty skrypt o nazwie "UploadSpeedMeasure.php" odbierajacy plik jpg.
 ```php
 <?php
  $file_path = "images/";
  $file_path = $file_path . basename( $_FILES['uploaded_file']['name']);
  if(move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file_path)) {
  echo "success";
  } else{ echo "fail";}
  ?>
  ```
# Niezbędne uprawnienia
 ```java  
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name=" android.permission.ACCESS_WIFI_STATE" />
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />
  ```
# Aktywności
![MainActivity](https://scontent-waw1-1.xx.fbcdn.net/v/t1.15752-9/34624291_1946025225430777_9019862343042990080_n.png?_nc_cat=0&oh=b289056ab6692036b9dbcc028a211e49&oe=5C292096)
![SettingsActivity](https://scontent-waw1-1.xx.fbcdn.net/v/t1.15752-9/34585050_1946025315430768_1234728536967741440_n.png?_nc_cat=0&oh=b9d7f682ca119a6ddc755526ed3f0323&oe=5C2698F2)
