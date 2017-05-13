#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/eeprom.h>
#include <util/delay.h>
#include "config.h"
#include "uart.h"
#include "utils.h"
#include <inttypes.h>
#include <string.h>
#include <stdio.h>
#include "driver.h"

void driver_checkReceivedData();

void processCommand(uint8_t command, uint8_t channel, uint16_t value);
void clearBuffer();
void sendCommand(uint8_t command, uint8_t params, uint16_t value);
void error(uint8_t code);
void error2(uint8_t code, uint8_t params);
uint8_t process_uart_queue();
uint8_t processSetPwm(uint8_t channels, uint16_t value, uint8_t params);

uint8_t deviceID = DEVICE_ID;
pwm_chnl out[6];
uint8_t groups[16]; // 16 zdefiniowanych grup
uint16_t helloCmdCounter;
uint16_t instanceId;

void sendHello() {
    helloCmdCounter = deviceID * 5;
}

uint8_t busyCounter;

void setBusy() {
    LOW(BUSY_LED);
    busyCounter = 35;
}

void driver_init() {
    instanceId = DEVICE_ID;

    uint8_t v = eeprom_read_byte(0);

    if (v != 0 && v != 255)
        deviceID = v;

    for (uint8_t i = 0; i < 16; i++) {
        v = eeprom_read_byte((void *) (int) (i + 1));
        if (v < 255)
            groups[i] = v;
    }

}

void onUartBufferClear() {
    //	error(ERR_UART_BUFFER_CLEARED);
}

//#define DEBUG

void beginTextMessage() {
    uart_writeByte(CMD_TEXT << 4); // textMessage
    uart_writeByte(deviceID);
    //(command << 4) | (params & 0x0F);
}

void endTextMessage() {
    uart_writeByte(0); // zakonczenie nadawania
}

void sendMessage(char* string) {
    beginTextMessage();
    uart_printStr(string);
    endTextMessage();
}

void sendCommand(uint8_t command, uint8_t params, uint16_t value) {

#ifdef DEBUG
    uart_printStr("CMD ");
    uart_printNumber(command);
    uart_printStr(" ");
    uart_printHex(value);
    uart_printLn("");
    return;
#endif

    uint8_t buff[4];
    uint8_t crc = 0;

    buff[0] = (command << 4) | (params & 0x0F);
    buff[1] = deviceID;
    buff[2] = value >> 8;
    buff[3] = value;

    for (uint8_t i = 0; i < 4; i++) {
        crc ^= buff[i];
        uart_push(buff[i]);
    }
    uart_push(crc);

    setBusy();

    //	infof("s: %02X %02X %02X %02X %02X", buff[0], buff[1], buff[2], buff[3],			crc);
}

void infof(const char *fmt, ...) {
    beginTextMessage();

    char buffer[50];
    va_list args;
    va_start(args, fmt);
    for (uint8_t i = 0; i < vsprintf(buffer, fmt, args); i++) {
        if (!buffer[i])
            break;
        uart_push(buffer[i]);
    }
    va_end(args);
    endTextMessage();
}

void info(const char *str) {
    beginTextMessage();
    uart_printStr(str);
    endTextMessage();
}

void driver_checkReceivedData() {

#define BUFF_SIZE 6

    if (busyCounter) {
        --busyCounter;
        if (!busyCounter)
            HIGH(BUSY_LED);
    }

    if (helloCmdCounter) {
        --helloCmdCounter;
        if (!helloCmdCounter)
            sendCommand(CMD_HELLO, SOFTWARE_VERSION, instanceId);
    }

    if (!uart_getReceivedDataSize())
        return; // nic nie odebrano

    if (uart_getReceivedDataSize() < BUFF_SIZE)
        return;

    uint8_t xor = 0;
    uint8_t buff[BUFF_SIZE];
    // przepisz bufor
    for (uint8_t i = 0; i < BUFF_SIZE; i++)
        buff[i] = uart_getByte();

    // oblicz crc
    for (uint8_t i = 0; i < BUFF_SIZE - 1; i++)
        xor ^= buff[i];

    //	infof("r: %02X %02X %02X %02X %02X %02X", buff[0], buff[1], buff[2], buff[3], buff[4], buff[5]);

    uint8_t command = buff[0] >> 4;
    uint8_t params = buff[0] & 0x0F;
    uint8_t id = buff[1];
    uint8_t channels = buff[2];
    uint16_t value = buff[3] << 8 | buff[4];

    //infof("CMD %d, id: %d, chnls: %d, par: %d, val: %d", command, id, channels, params, value);

    // sprawdź CRC
    if (xor != buff[BUFF_SIZE - 1]) {
        //error(ERR_WRONG_CRC);
        return;
    }

    // --------------------- komendy broadcastowe -----------------------
    // ustaw stany pwm dla grup
    if (command == CMD_SET_PWM_GROUPS) {
        uint16_t grps = buff[1] << 8 | buff[2];

        for (uint8_t i = 0; i < 16; i++)
            if (grps & (1 << i))
                processSetPwm(groups[i], value, params);
        return;
    }

    if (command == CMD_HELLO && id == 0 && params == 15 && value == 0xEEEE) {
        sendHello();
        return;
    }

    if (command == CMD_SET_ID && id > 0 && params == 1 && channels == 0xAB
            && value == 0xA29D) {
        deviceID = id;
        eeprom_write_byte(0, id);
        sendCommand(CMD_SET_ID, 0, deviceID);
        return;
    }

    // ----------- komendy dedykowane dla danego terminala --------------------

    if (id != deviceID)
        return;

    setBusy();

    uint8_t ok = 0;

    switch (command) {

        case CMD_HELLO:
            if (value == 0xCCCC) {
                sendCommand(CMD_HELLO, SOFTWARE_VERSION, instanceId);
                return;
            }
            break;

        case CMD_SET_PWM: // ustaw stan wyjscia
            ok |= processSetPwm(channels, value, params);
            if (!ok)
                error(ERR_WRONG_CHANNELS);
            return;

        case CMD_GET_PWM: // pobierz stan pwm
            for (uint8_t i = 0; i < 6; i++)
                if (channels & (1 << i)) {
                    ok = 1;
                    sendCommand(CMD_GET_PWM, i, out[i].value);
                }
            if (!ok)
                error(ERR_WRONG_CHANNELS);

            return;

        case CMD_GROUPS:
            if (params == 1) { // zapisz grupy
                if (value >= 16) {
                    error(ERR_INCORRECT_GROUP_NUMBER);
                    return;
                }
                groups[(uint8_t) value] = channels;
                eeprom_write_byte((void *) (int) ((uint8_t) value + 1), channels);
                return;
            }

            if (params == 2) { // pobierz grupy
                for (uint8_t i = 0; i < 16; i++)
                    if (value & (1 << i))
                        sendCommand(CMD_GROUPS, i, groups[i]);
                return;
            }

            break;

        case CMD_RESET_DEVICE:
            if (params == 11 && channels == 222 && value == 0xEDCB) {
                for (;;) { // zapetlamy program, zresetuje sie automatycznie przez WatchDog-a
                }
                return;
            }
            break;

        case CMD_INSTANCE_ID:
            if (params == 1) {
                instanceId = value;
                return;
            }
            if (params == 2) {
                sendCommand(CMD_INSTANCE_ID, 0, instanceId);
                return;
            }
            break;

        case CMD_EEPROM_DATA:
            if (params == 1 && channels < 16) {

                // zapisz dowolne dane od komorki 20. max 512  bajtow (256 x 2 bajty)
                eeprom_write_word((void *) (int) (channels * 2 + 20), value);
                return;
            }
            if (params == 2 && channels < 16) {
                value = eeprom_read_word((void *) (int) (channels * 2 + 20));
                sendCommand(CMD_EEPROM_DATA, channels, value);
                return;
            }
            break;

    }

    error(ERR_UNKNOWN_COMMAND);
}

uint8_t processSetPwm(uint8_t channels, uint16_t value, uint8_t params) {
    uint8_t ok = 0;
    for (uint8_t i = 0; i < 6; i++)
        if (channels & (1 << i)) {
            ok = 1;
            //out[i].value = value;
            out[i].destination = value;
            out[i].speed = params;
            out[i].delay = 0;
        }
    return ok;
}

void error(uint8_t code) {
    error2(code, 0);
}

void error2(uint8_t code, uint8_t params) {
    sendCommand(CMD_ERROR_CODE, params, code);
}
