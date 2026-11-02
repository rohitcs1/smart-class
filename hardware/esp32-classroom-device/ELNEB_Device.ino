/*
 * ELNEB classroom device starter.
 * The dashboard-generated variant replaces the configuration values below.
 * Install MFRC522 and ArduinoJson libraries before uploading.
 */
#include <WiFi.h>
#include <HTTPClient.h>
#include <SPI.h>
#include <MFRC522.h>

const char* WIFI_SSID = "REPLACE_WITH_WIFI_SSID";
const char* WIFI_PASSWORD = "REPLACE_WITH_WIFI_PASSWORD";
const char* DEVICE_CODE = "REPLACE_WITH_DEVICE_ID";
const char* SETUP_TOKEN = "REPLACE_WITH_SETUP_TOKEN";
const char* API_BASE_URL = "http://localhost:8080/api";

constexpr uint8_t RFID_SS_PIN = 5;
constexpr uint8_t RFID_RST_PIN = 22;
constexpr uint8_t STATUS_LED_PIN = 2;
MFRC522 rfid(RFID_SS_PIN, RFID_RST_PIN);

void connectWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long started = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - started < 30000) { delay(500); digitalWrite(STATUS_LED_PIN, !digitalRead(STATUS_LED_PIN)); }
}

void setup() {
  pinMode(STATUS_LED_PIN, OUTPUT);
  Serial.begin(115200);
  SPI.begin();
  rfid.PCD_Init();
  connectWifi();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) { connectWifi(); return; }
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) { delay(100); return; }
  String uid;
  for (byte i = 0; i < rfid.uid.size; i++) { if (rfid.uid.uidByte[i] < 0x10) uid += "0"; uid += String(rfid.uid.uidByte[i], HEX); }
  HTTPClient http;
  http.begin(String(API_BASE_URL) + "/device-events");
  http.addHeader("Content-Type", "application/json");
  String body = "{\"deviceCode\":\"" + String(DEVICE_CODE) + "\",\"setupToken\":\"" + String(SETUP_TOKEN) + "\",\"rfidUid\":\"" + uid + "\"}";
  int response = http.POST(body);
  digitalWrite(STATUS_LED_PIN, response >= 200 && response < 300);
  http.end();
  rfid.PICC_HaltA();
  delay(1000);
}
