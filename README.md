# Co to jest?
AdaptiveVideoStreaming to aplikacja stworzona na potrzeby pracy magisterskiej. Odnosi się ona do tematyki strumieniowania wideo w czasie rzeczywistym z adaptacją strumienia do aktualnie panujących warunków sieciowych. Aplikacja oparta jest o bibliotekę [libstreaming](https://github.com/fyhertz/libstreaming).
# Zasada działania
W aplikację zaimplementowano klasę BitrateAdapter, która jest odpowiedzialna za okresowy pomiar prędkości wysyłania poprzez przesył pliku jpg na serwer HTTP, oraz oszacowanie na podstawie tej wielkości optymalnej prędkości bitowej strumienia. Aplikacja na bieżąco wyświetla parametry połączenia oraz informację o ewentualnej zmianie prędkości bitowej. Aplikacja umożliwia strumieniowanie z przedniej bądź tylnej kamery, dodatkowo zaimplementowano w niej funkcję zoomu oraz autofocusu.
# Czego potrzebujesz?
Do odpowiedniego działania aplikacji niezbędny jest serwer RTSP (aplikacja testowana na serwerze Wowza Media Engine), oraz serwer HTTP (np. Apache), na którym należy umieścić prosty skrypt o nazwie "UploadSpeedMeasure.php" odbierajacy plik jpg.
 `<?php
  $file_path = "images/";
  $file_path = $file_path . basename( $_FILES['uploaded_file']['name']);
  if(move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file_path)) {
  echo "success";
  } else{ echo "fail";}
  ?>`
# Niezbędne uprawnienia
`   <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name=" android.permission.ACCESS_WIFI_STATE" />
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />`
