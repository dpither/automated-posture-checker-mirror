#ifndef _BLE_CONTROLLER_H
#define _BLE_CONTROLLER_H

#include <stdint.h>

// CTRL pins
#define START_STOP_PIN (GPIO_NUM_26)
#define LED_PIN (GPIO_NUM_2)
#define CTRL_PIN_SEL  (1ULL << START_STOP_PIN | 1ULL << LED_PIN)

// protocol from phone:
#define BLE_START_COMMAND 0x01
#define BLE_STOP_COMMAND 0x02
#define BLE_SEND_TEST_PHOTO 0x03
#define BLE_WIFI_CONFIGURE 0x04

#define WIFI_SSID_MAX_LEN 31
#define WIFI_PASS_MAX_LEN 31

extern uint32_t g_session_id;
extern uint32_t g_user_id;

typedef struct {
    uint32_t session_id;
    uint32_t user_id;
    uint8_t wifi_ssid_length;
    char wifi_ssid[WIFI_SSID_MAX_LEN];
    uint8_t wifi_password_length;
    char wifi_password[WIFI_PASS_MAX_LEN];
} configure_args_t;


void ble_controller_init(void);

#endif