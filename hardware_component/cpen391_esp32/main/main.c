#include <string.h>
#include <sys/param.h>
#include <stdlib.h>
#include <ctype.h>
#include "esp_log.h"
#include "esp_event.h"
#include "nvs_flash.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"

#include "wifi_controller.h"
#include "ble_controller.h"
#include "uart_controller.h"
#include "driver/gpio.h"

#define TAG "MAIN"

void app_main(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
      ESP_ERROR_CHECK(nvs_flash_erase());
      ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    wifi_controller_init();
    ble_controller_init();
    uart_controller_init();

    vTaskDelay( 3000 / portTICK_PERIOD_MS );

    gpio_config_t io_conf = {};
    //disable interrupt
    io_conf.intr_type = GPIO_INTR_DISABLE;
    //set as output mode
    io_conf.mode = GPIO_MODE_OUTPUT;
    //bit mask of the pins that you want to set,e.g.GPIO18/19
    io_conf.pin_bit_mask = CTRL_PIN_SEL;
    //disable pull-down mode
    io_conf.pull_down_en = 0;
    //disable pull-up mode
    io_conf.pull_up_en = 0;
    //configure GPIO with the given settings
    gpio_config(&io_conf);

    gpio_set_level(START_STOP_PIN, 0);
    gpio_set_level(LED_PIN, 1);

    //ESP_LOGI(TAG, "All initialization completed, sending a test HTTP POST request in 5 seconds....");

    //vTaskDelay( 5000 / portTICK_PERIOD_MS );
    //http_test_task();
}
