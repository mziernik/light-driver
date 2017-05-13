#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/eeprom.h>
#include <util/delay.h>
#include "utils.h"
#include "uart.h"
#include "stdlib.h" //memset
#include <inttypes.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>

void uart_writeByte(uint8_t value) {
    while (!(UCSR0A & (1 << UDRE0))) {
    }

    UDR0 = value;
}


// zdarzenie wyslania bajtu

ISR(USART_TX_vect) {

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
