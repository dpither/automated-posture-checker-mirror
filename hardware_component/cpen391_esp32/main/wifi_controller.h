#ifndef _WIFI_CONTROLLER_H
#define _WIFI_CONTROLLER_H

#define DEFAULT_WIFI_SSID "EZBY";
#define DEFAULT_WIFI_PASSWORD = "jjjg6100";

void wifi_controller_init(void);

int wifi_controller_connect(char * ssid, uint8_t ssid_len, char * password, uint8_t pass_len);

void http_send_image_start(char * img_buffer);

#endif