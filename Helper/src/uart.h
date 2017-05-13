#ifndef UART_H
#define UART_H

#define USART_BAUDRATE 9600
#define BAUD_PRESCALLER (((F_CPU / (USART_BAUDRATE * 16UL))) - 1)

extern uint8_t uart_getWriteBufferQueueSize();
extern uint8_t uart_getByte();
extern void uart_push(const uint8_t byte);
extern uint8_t uart_getReceivedDataSize();
extern void process_commands();
extern void uart_printNumber(const uint16_t num);
extern void uart_printHex(const uint16_t num);
extern void uart_clearTerminal();
extern void uart_init();
extern void uart_flush();
extern void uart_writeByte(const uint8_t u8Data);
extern void uart_printStr(const char* StringPtr);
extern void uart_printLn(const char* string);
extern void uart_debugInt1(char *tag, uint16_t value);
extern void uart_debugInt2(char *tag, uint16_t value1, uint16_t value2);

extern uint8_t uart_readByte();


#endif

