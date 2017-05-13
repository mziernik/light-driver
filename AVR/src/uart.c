#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/eeprom.h>
#include <util/delay.h>
#include "utils.h"
#include "config.h"
#include "uart.h"
#include "driver.h"
#include "stdlib.h" //memset
#include <inttypes.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>

//#define UART_BUSY_PIN D,3

#define UART_WRITE_BUFFER_SIZE 255
#define UART_READ_BUFFER_SIZE 200

uint8_t writeBuffer[UART_WRITE_BUFFER_SIZE];
uint8_t writeBufferPtr = 0;

uint8_t readBuffer[UART_WRITE_BUFFER_SIZE];
uint8_t readBufferPtr = 0;

#define AUTO_CLEAR_COUNT 20 // LICZNIK AUTOMATYCZNEGO CZYSZCZENIA BUFORA
uint16_t autoClearCounter = AUTO_CLEAR_COUNT;

void uart_flush();
//------------------------------------------------------------------------------------
// dodaj bajt do wysłania

void uart_push(const uint8_t byte) {
    if (writeBufferPtr < UART_WRITE_BUFFER_SIZE)
        writeBuffer[writeBufferPtr++] = byte;

    uart_flush();
}

void uart_clearReadBuffer() {
    readBufferPtr = 0;
}

uint8_t uart_getReceivedDataSize() {

#ifdef AUTO_CLEAR_COUNT
    if (autoClearCounter) {
        --autoClearCounter;
        if (readBufferPtr && !autoClearCounter) { // jesli zliczono do 0 i cos nadal jest w buforze wejsciowym

#ifdef ON_UART_READ_BUFFER_CLEARED
            ON_UART_READ_BUFFER_CLEARED();
#endif

            uart_clearReadBuffer();
        }
    }
#endif

    return readBufferPtr;
}

uint8_t uart_getByte() {

    if (!readBufferPtr)
        return 0;

    uint8_t result = readBuffer[0];

    for (uint8_t i = 0; i < readBufferPtr; i++)
        readBuffer[i] = readBuffer[i + 1];

    --readBufferPtr;

    return result;

}

#ifdef UART_BUSY_PIN
uint8_t busyBlocked = 0;
#endif

/**
 * Wyslij kolejny bajt z kolejki uart-a. Jesli uart jest zajety to nie rob nic
 */
void uart_flush() {

    if (!writeBufferPtr)
        return;


#ifdef UART_BUSY_PIN
    if (!busyBlocked && !READ(UART_BUSY_PIN))
        return;

    busyBlocked = 1;
    OUTPUT(UART_BUSY_PIN);
    LOW(UART_BUSY_PIN);
#endif

    if (!(UCSR0A & (1 << UDRE0)))
        return;

    UDR0 = writeBuffer[0];

    for (uint8_t i = 0; i < writeBufferPtr; i++)
        writeBuffer[i] = writeBuffer[i + 1];

    --writeBufferPtr;

#ifdef UART_BUSY_PIN

    if (!writeBufferPtr) { // zakonczenie nadawania
        HIGH(UART_BUSY_PIN);
        INPUT(UART_BUSY_PIN);
        busyBlocked = 0;
    }
#endif

}

// zdarzenie wyslania bajtu

ISR(USART_TX_vect) {
    uart_flush();
}

ISR(USART_RX_vect) {
    if (readBufferPtr < UART_READ_BUFFER_SIZE)
        readBuffer[readBufferPtr++] = UDR0;



#ifdef AUTO_CLEAR_COUNT
    autoClearCounter = AUTO_CLEAR_COUNT;
#endif

}

uint8_t uart_getWriteBufferQueueSize() {
    return writeBufferPtr;
}

void uart_printNumber(const const uint16_t num) {

    const int size = 8;

    int8_t buff[size];

    memset(buff, 0, size);
    itoa(num, (char*) buff, 10);

    for (int i = 0; i < size; i++)
        if (buff[i] > 32)
            uart_writeByte(buff[i]);
}

void uart_printHex(uint16_t num) {

    const int size = 4;

    int8_t buff[size];

    memset(buff, 0, size);
    itoa(num, (char*) buff, 16);

    if (num < 0x10 || (num > 0xFF && num < 0x1000))
        uart_writeByte('0');

    for (int i = 0; i < size; i++)
        if (buff[i] > 32)
            uart_writeByte(buff[i]);
}

void uart_clrTerminal() {
    uart_writeByte(27);
    uart_printStr("[23");
}

void uart_resetTerminal() {
    uart_writeByte(27);
    uart_writeByte('c');
}

void uart_debugInt1(char *tag, uint16_t value) {
    uart_printStr(tag);
    uart_writeByte(' ');
    uart_printNumber(value);
    uart_printStr("\r\n");
}

void uart_debugInt2(char *tag, uint16_t value1, uint16_t value2) {
    uart_printStr(tag);
    uart_writeByte(' ');
    uart_printNumber(value1);
    uart_printStr(", ");
    uart_printNumber(value2);
    uart_printStr("\r\n");
}

void uart_init() {
    // enable RX and TX and set interrupts on rx complete
    UCSR0B = (1 << TXCIE0) | (1 << RXCIE0) | (1 << TXEN0) | (1 << RXEN0);

    // 8-bit, 1 stop bit, no parity, asynchronous UART
    UCSR0C = (1 << UCSZ01) | (1 << UCSZ00) | (0 << USBS0) | (0 << UPM01)
            | (0 << UPM00) | (0 << UMSEL01) | (0 << UMSEL00);

    UBRR0H = (uint8_t) (BAUD_PRESCALLER >> 8);
    UBRR0L = (uint8_t) (BAUD_PRESCALLER);

    sei();
}

void uart_writeByte(const uint8_t byte) {
    uart_push(byte);
}

void uart_printStr(const char* string) {
    while (*string != 0x00) {
        uart_writeByte(*string);
        string++;
    }
}

void uart_printLn(const char* string) {
    uart_printStr(string);
    uart_printStr("\r\n");
}
