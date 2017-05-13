#ifndef CONFIG_H
#define CONFIG_H


//============== identyfikator terminala ==================
#define DEVICE_ID 32

#define USE_FIRST_SW_PORT // odkomentowac jesli port jest uzywany



#define SOFTWARE_VERSION 8

#define BUSY_LED D,2


#define SWA  B,5 // 1 linia wspolna
#define SWB  B,0 // 2 linia wspolna
#ifdef USE_FIRST_SW_PORT
#define SW1  B,3
#define SW2  B,4
#define SW3  D,7
#define SW4  D,6
#endif

#ifdef USE_SECOND_SW_PORT
#define SW5  B,1
#define SW6  B,2
#define SW7  D,2
#define SW8  D,5
#endif

#define PWM1  C,5
#define PWM2  C,2
#define PWM3  C,0
#define PWM4  C,1
#define PWM5  C,4
#define PWM6  C,3

#define BUSY  D,3
#define EMPTY D,4

#define UART_BUSY_PIN D,3
/*
// Makra upraszczaj�ce dost�p do port�w
// *** PORT
#define PORT(x) SPORT(x)
#define SPORT(x) (PORT##x)
// *** PIN
#define PIN(x) SPIN(x)
#define SPIN(x) (PIN##x)
// *** DDR
#define DDR(x) SDDR(x)
#define SDDR(x) (DDR##x)
*/
#endif
