#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"
#include "esp_log.h"
#include "driver/uart.h"
#include "string.h"
#include "driver/gpio.h"
#include "wifi_controller.h"

#define IMAGE_MAX_SIZE 30000
#define START_BYTE 0x80
#define STOP_BYTE 0x81 

static const int RX_BUF_SIZE = 256;

static char image_data[IMAGE_MAX_SIZE];
int image_data_idx;

#define TXD_PIN (GPIO_NUM_14)
#define RXD_PIN (GPIO_NUM_12)

#define START_STOP_PIN (GPIO_NUM_26)
#define CTRL_PIN_SEL  (1ULL<<START_STOP_PIN)

static void rx_task(void *arg)
{
    static const char *RX_TASK_TAG = "RX_TASK";
    esp_log_level_set(RX_TASK_TAG, ESP_LOG_INFO);
    
    char firstByte;
    bool incoming_image = false;
    while (1) {
        int rxBytes;
        if (!incoming_image) {
            rxBytes = uart_read_bytes(UART_NUM_1, &firstByte, 1, 1000 / portTICK_PERIOD_MS);
            if (rxBytes > 0 && firstByte == START_BYTE) {
                ESP_LOGI(RX_TASK_TAG, "Read %d bytes", rxBytes);
                ESP_LOGI(RX_TASK_TAG, "Image incoming...");
                incoming_image = true;
            }
        } else {
            rxBytes = uart_read_bytes(UART_NUM_1, &image_data[image_data_idx], IMAGE_MAX_SIZE - image_data_idx, 1000 / portTICK_PERIOD_MS);
            if (rxBytes > 0) {
                ESP_LOGI(RX_TASK_TAG, "Read %d bytes", rxBytes);
                //ESP_LOG_BUFFER_HEXDUMP(RX_TASK_TAG, &image_data[image_data_idx], rxBytes, ESP_LOG_INFO);
                image_data_idx += rxBytes;

                if (image_data_idx >= IMAGE_MAX_SIZE) {
                    ESP_LOGI(RX_TASK_TAG, "\n----\n----\nCRTICAL WARNING Image limit reached!\n---\n----\n");
                    //ESP_LOG_BUFFER_HEXDUMP(RX_TASK_TAG, &image_data[0], IMAGE_MAX_SIZE, ESP_LOG_INFO);
                    image_data_idx = 0;
                }

                if (image_data[image_data_idx - 1] == STOP_BYTE) {
                    ESP_LOGI(RX_TASK_TAG, "Image Ended, forwarding to http");
                    image_data[image_data_idx - 1] = 0; //add 0 to null terminate
                    http_send_image_start(&image_data[0]);
                    incoming_image = false;
                    image_data_idx = 0;
                }
            }
        }
    }
}

void uart_controller_init(void) {
    const uart_config_t uart_config = {
        .baud_rate = 115200,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };

    // We won't use a buffer for sending data.
    uart_driver_install(UART_NUM_1, RX_BUF_SIZE * 2, 0, 0, NULL, 0);
    uart_param_config(UART_NUM_1, &uart_config);
    uart_set_pin(UART_NUM_1, TXD_PIN, RXD_PIN, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);


    xTaskCreate(rx_task, "uart_rx_task", 1024*2, NULL, configMAX_PRIORITIES, NULL);
}