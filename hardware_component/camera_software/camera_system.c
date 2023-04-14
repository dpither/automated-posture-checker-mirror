#define DEBUG 0

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include "address_map_arm.h"
#include "interrupt_ID.h"
#include "rs232_driver_regs.h"
#include "jpeglib.h"

#define RAW_RGB_FILE "raw16_img.bin"
#define OUTPUT_FILE "output.jpg"
#define ESP32_RX_BUFF_SIZE 256
#define ESP32_IMAGE_MAX_SIZE 30000

#define WIDTH 320
#define HEIGHT 240

#define START_STOP_PIN_MASK 0x01

#define START_BYTE 0x80
#define STOP_BYTE 0x81 

#define printf_debug(format,args...) if (DEBUG) printf(format, ## args);


static char encoding_table[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                                'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                                'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                                'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                                'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                'w', 'x', 'y', 'z', '0', '1', '2', '3',
                                '4', '5', '6', '7', '8', '9', '+', '/'};
static int mod_table[] = {0, 2, 1};

char *input_file = RAW_RGB_FILE;
char *output_file = OUTPUT_FILE;

int fd = -1;               // used to open /dev/mem for access to physical addresses
void *LW_virtual;          // used to map physical addresses for the light-weight bridge
void *FPGA_ONCHIP_virtual; // used to map physical addresses for the ONCHIP memory

// Hardware device addresses
volatile uint32_t *RS232_UART_data_ptr;
volatile uint32_t *RS232_UART_ctrl_ptr;
volatile uint32_t *Q_ptr;
volatile uint32_t *LEDR_ptr;
volatile uint32_t *KEY_ptr;
volatile uint32_t *GPIO_CTRL_ptr;
volatile uint32_t *VIDEO_ptr;
volatile uint32_t *BUFF_ptr;
volatile uint32_t *VIDEO_ptr_en;

/* Prototypes for functions used to access physical memory addresses */
int open_physical(int);
void *map_physical(int, unsigned int, unsigned int);
void close_physical(int);
int unmap_physical(void *, unsigned int);


char * base64_encode(const unsigned char *data,
                    size_t input_length,
                    size_t *output_length) {
 
    *output_length = 4 * ((input_length + 2) / 3);
 
    char *encoded_data = malloc(*output_length);
    if (encoded_data == NULL) {
        printf_debug("Failed to allocate memory\n");
        exit(-1);
    }
    if (encoded_data == NULL) return NULL;
 
    for (int i = 0, j = 0; i < input_length;) {
 
        uint32_t octet_a = i < input_length ? (unsigned char)data[i++] : 0;
        uint32_t octet_b = i < input_length ? (unsigned char)data[i++] : 0;
        uint32_t octet_c = i < input_length ? (unsigned char)data[i++] : 0;
 
        uint32_t triple = (octet_a << 0x10) + (octet_b << 0x08) + octet_c;
 
        encoded_data[j++] = encoding_table[(triple >> 3 * 6) & 0x3F];
        encoded_data[j++] = encoding_table[(triple >> 2 * 6) & 0x3F];
        encoded_data[j++] = encoding_table[(triple >> 1 * 6) & 0x3F];
        encoded_data[j++] = encoding_table[(triple >> 0 * 6) & 0x3F];
    }
 
    for (int i = 0; i < mod_table[input_length % 3]; i++)
        encoded_data[*output_length - 1 - i] = '=';
 
    return encoded_data;
}


/* Send data over serial port */
void send_image_data()
{
    FILE *output_file_ptr = fopen(output_file, "rb");

    // Get size of JPG
    fseek(output_file_ptr, 0, SEEK_END);
    long fsize = ftell(output_file_ptr);

    // Rewind file pointer to beginning
    fseek(output_file_ptr, 0, SEEK_SET);

    // Read jpg into a buffer
    char * jpg_buffer = malloc(fsize);
    if (jpg_buffer == NULL) {
        printf_debug("Failed to allocate memory\n");
        exit(-1);
    }

    fread(jpg_buffer, fsize, 1, output_file_ptr);

    printf_debug("Encoding jpg to base64 format...\n");
    // Encode jpg to base64 format
    size_t base64_len;
    char * base64_jpg_buffer = base64_encode(jpg_buffer, fsize, &base64_len);

    printf_debug("Sending base64 encoded jpg to UART, encoded string size is %d...\n", base64_len);
    
    // Output base64 encoded jpg string to a file for debugging purposes
    FILE *base64_str_file = fopen("output_base64.txt", "w");
    fwrite(base64_jpg_buffer, 1, base64_len, base64_str_file);
    fclose(base64_str_file);

    // Signify start of image
    *RS232_UART_data_ptr = (START_BYTE >> RS232_DATA_DATA_OFST) & RS232_DATA_DATA_MSK;
    *LEDR_ptr |= 4;
    usleep(100000);
    // Send base64 encoded image to UART
    for (size_t i = 0; i < base64_len; i++) {
        if (i % ESP32_RX_BUFF_SIZE == 0) {
            // After sending a multiple of the RX buffer size of the esp32 chip, it is better to use a 
            // longer delay to allow the esp chip to read everything and empty its rx buffer
            usleep(10000);
        }
        *RS232_UART_data_ptr = (base64_jpg_buffer[i] >> RS232_DATA_DATA_OFST) & RS232_DATA_DATA_MSK;
        usleep(1); //This delay is necessary otherwise it would be too fast for the RS232 IP core
    }
    usleep(100000);
    // Signify end of image
    *RS232_UART_data_ptr = (STOP_BYTE >> RS232_DATA_DATA_OFST) & RS232_DATA_DATA_MSK;

    //release memory
    free(base64_jpg_buffer);
    free(jpg_buffer);

    fclose(output_file_ptr);
    printf_debug("Sent base64 encoded jpg over UART\n");
}

/* Compress from raw image format to jpg */
void compress_raw_to_jpg()
{
    printf_debug("Compressing raw image to jpg...\n");
    uint16_t *image_buffer_16bit = malloc(WIDTH * HEIGHT * 2);
    if (image_buffer_16bit == NULL) {
        printf_debug("Failed to allocate memory\n");
        exit(-1);
    }
    unsigned char *image_buffer_32bit = (unsigned char *)malloc(WIDTH * HEIGHT * 3);
    if (image_buffer_32bit == NULL) {
        printf_debug("Failed to allocate memory\n");
        exit(-1);
    }
    FILE *input_file_ptr = fopen(input_file, "rb");
    int bytes_read = fread(image_buffer_16bit, 1, WIDTH * HEIGHT * 2, input_file_ptr);
    if (bytes_read == 0) {
        printf_debug("Warning: Read 0 bytes from raw image file\n");
    }
    fclose(input_file_ptr);

    for (int i = 0; i < WIDTH * HEIGHT; i++)
    {
        uint16_t rgb16 = image_buffer_16bit[i];
        uint8_t r = (rgb16 >> 11) & 0x1f;
        uint8_t g = (rgb16 >> 5) & 0x3f;
        uint8_t b = rgb16 & 0x1f;
        image_buffer_32bit[i * 3] = (r << 3) | (r >> 2);
        image_buffer_32bit[i * 3 + 1] = (g << 2) | (g >> 4);
        image_buffer_32bit[i * 3 + 2] = (b << 3) | (b >> 2);
    }

    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);
    FILE *output_file_ptr = fopen(output_file, "wb");
    jpeg_stdio_dest(&cinfo, output_file_ptr);
    cinfo.image_width = WIDTH;
    cinfo.image_height = HEIGHT;
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_RGB;
    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, 75, TRUE);
    jpeg_start_compress(&cinfo, TRUE);

    JSAMPROW row_pointer[1];
    int row_stride;
    row_stride = WIDTH * 3;
    while (cinfo.next_scanline < cinfo.image_height)
    {
        row_pointer[0] = &image_buffer_32bit[cinfo.next_scanline * row_stride];
        (void)jpeg_write_scanlines(&cinfo, row_pointer, 1);
    }

    jpeg_finish_compress(&cinfo);
    fclose(output_file_ptr);
    jpeg_destroy_compress(&cinfo);
    free(image_buffer_16bit);
    free(image_buffer_32bit);
    printf_debug("Compressed raw image to jpg\n");
}

int init(void)
{
    // Create virtual memory access to the FPGA light-weight bridge
    if ((fd = open_physical(fd)) == -1)
        return (-1);
    if ((LW_virtual = map_physical(fd, LW_BRIDGE_BASE, LW_BRIDGE_SPAN)) == NULL)
        return (-1);
    if ((FPGA_ONCHIP_virtual = map_physical(fd, FPGA_ONCHIP_BASE, FPGA_ONCHIP_SPAN)) == NULL)
        return (-1);

    // Set virtual address pointer to I/O port
    RS232_UART_data_ptr = ((uint32_t *)(LW_virtual + RS232_UART_BASE)) + RS232_DATA_REG;
    RS232_UART_ctrl_ptr = ((uint32_t *)(LW_virtual + RS232_UART_BASE)) + RS232_CONTROL_REG;
    Q_ptr = (uint32_t *)(LW_virtual + Q_BASE);
    VIDEO_ptr = (uint32_t *)(LW_virtual + VIDEO_IN_BASE);
    KEY_ptr = (uint32_t *)(LW_virtual + KEY_BASE);
    GPIO_CTRL_ptr = (uint32_t *)(LW_virtual + GPIO_CTRL_BASE);
    LEDR_ptr = (uint32_t *) (LW_virtual + LEDR_BASE);
    BUFF_ptr = (uint32_t *)(FPGA_ONCHIP_virtual);

    VIDEO_ptr_en = (uint32_t *)((void *)VIDEO_ptr + 0xC);

    *LEDR_ptr |= 1;

    return 0;
}

void cleanup(void)
{
    unmap_physical(LW_virtual, LW_BRIDGE_SPAN); // release the physical-memory mapping
    close_physical(fd);                         // close /dev/mem
}

void take_picture(void)
{
    FILE * raw_img_file = fopen(input_file, "w");
    if (raw_img_file == NULL)
    {
        printf_debug("Error: unable to open output file\n");
        exit(-1);
    }

    // Enable Camera
    *(VIDEO_ptr_en) = (*(VIDEO_ptr_en) | 1 << 2);

    // Sleep for a bit for camera to capture image
    sleep(1);

    // Stop Camera
    *(VIDEO_ptr_en) = (*(VIDEO_ptr_en) | 0 << 2);

    // Read Framebuffer
    printf_debug("Reading Framebuffer...\n");
    for (int y = 0; y < HEIGHT; y++)
    {
        fwrite((void *)BUFF_ptr + (y << 10), (size_t)2, WIDTH, raw_img_file);
    }
    fclose(raw_img_file);
}

int main(void)
{
    int retVal = init();
    if (retVal != 0) {
        printf_debug("Error initializing...\n");
        return -1;
    }

    uint8_t gpio_ctrl;
    while (1) {
        gpio_ctrl = *GPIO_CTRL_ptr;
        if (gpio_ctrl & START_STOP_PIN_MASK) {
            printf_debug("System is ON: Begin picture taking and upload\n");
            take_picture();
            compress_raw_to_jpg();
            send_image_data();
            printf_debug("Sleeping for 10 seconds before checking again\n");
            sleep(10);
            *LEDR_ptr |= 2;
        } else {
            *LEDR_ptr &= 0xFFFFFFFD;
        }
    }

    cleanup();
    return 0;
}

// Open /dev/mem, if not already done, to give access to physical addresses
int open_physical(int fd)
{
    if (fd == -1)
        if ((fd = open("/dev/mem", (O_RDWR | O_SYNC))) == -1)
        {
            printf_debug("ERROR: could not open \"/dev/mem\"...\n");
            return (-1);
        }
    return fd;
}

// Close /dev/mem to give access to physical addresses
void close_physical(int fd)
{
    close(fd);
}

/*
 * Establish a virtual address mapping for the physical addresses starting at base, and
 * extending by span bytes.
 */
void *map_physical(int fd, unsigned int base, unsigned int span)
{
    void *virtual_base;

    // Get a mapping from physical addresses to virtual addresses
    virtual_base = mmap(NULL, span, (PROT_READ | PROT_WRITE), MAP_SHARED, fd, base);
    if (virtual_base == MAP_FAILED)
    {
        printf_debug("ERROR: mmap() failed...\n");
        close(fd);
        return (NULL);
    }
    return virtual_base;
}

/*
 * Close the previously-opened virtual address mapping
 */
int unmap_physical(void *virtual_base, unsigned int span)
{
    if (munmap(virtual_base, span) != 0)
    {
        printf_debug("ERROR: munmap() failed...\n");
        return (-1);
    }
    return 0;
}
