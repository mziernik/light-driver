#ifndef MISC_H
#define MISC_H

typedef struct pwm_chnl_struct {
	uint8_t speed; // opoznienie animacji
	uint16_t destination;
	uint16_t value;
	double animValue;
	//uint8_t compensation;
	uint8_t delay;
} pwm_chnl;



typedef struct word_struct {
	uint16_t hi;
	uint16_t lo;
} word;


#define CMD_HELLO 0
#define CMD_SET_ID 1
#define CMD_SET_PWM 2
#define CMD_GET_PWM 3
#define CMD_SET_PWM_GROUPS 4
#define CMD_GROUPS 5
#define CMD_SWITCHES_STATE 6
#define CMD_ERROR_CODE 7
#define CMD_TEXT 8
#define CMD_RESET_DEVICE 9
#define CMD_INSTANCE_ID 10
#define CMD_EEPROM_DATA 11



#define ERR_UNKNOWN_COMMAND  1
#define ERR_WRONG_CRC  2
#define ERR_WRONG_CHANNELS  3  //
#define ERR_INCORRECT_GROUP_NUMBER  4
#define ERR_UART_BUFFER_CLEARED  5
#define ERR_SWITCH_STATE  6 // ZWARCIE

#define ON_UART_READ_BUFFER_CLEARED onUartBufferClear


extern pwm_chnl out[6];
extern uint8_t groups[16];
extern void setBusy();

extern uint16_t commandSwitchState;
extern void infof(const char *fmt, ...);
extern void info(const char *string);
extern void sendCommand(uint8_t command, uint8_t params, uint16_t value);
extern void driver_checkReceivedData();
extern void sendHello();
extern void onUartBufferClear();

extern void sendMessage(char* string);

extern void error(uint8_t code);
extern void error2(uint8_t code, uint8_t params);

extern void driver_init();
extern void beginTextMessage();
extern void endTextMessage();

extern uint8_t process_uart_queue();
extern void driver_onByteReceived(uint8_t value);

#endif // MISC_H
