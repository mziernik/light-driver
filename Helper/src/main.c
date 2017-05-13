#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include "utils.h"
#include "uart.h"

// --------------- USB ----------------------
#define USB_RX  D, 0 // 2
#define USB_TX  D, 1 // 3	


// ------------------- Czujnik PIR ------------
#define PIR_PWR  D, 7 // 13
#define PIR_PWM  D, 6 // 12
#define PIR_STATE D, 5 // 11

// ------------ Ładowarka 1 --------------
#define BAT_PWR  C, 0 // 23
#define BAT_STATE D, 3 // 5
#define BAT_2  D, 4 // 6

// ------------------ Złącze --------------
#define SOCKET_A C, 1 // 24
#define SOCKET_B C, 2 // 25

#define BUZZER  C, 1

char busPwrState = 0;

void init(void);

//-----------------------------------------------------

int data[2];
int dataPtr = 0;

int main(void) {
    init();
    _delay_ms(10);
    
    int status = 0;
    int counter = 0;

    beep(1);

    while (1) {

        int sts = 0;

        if (READ(PIR_STATE))
            sts |= 0x01;

        if (READ(BAT_STATE))
            sts |= 0x02;

        ++counter;

        if (sts != status || counter > 3000) {
            counter = 0;
            uart_writeByte(sts);
            status = sts;
        }
        _delay_ms(10);
    }
}

ISR(USART_RX_vect) {


    data[dataPtr++] = UDR0;

    if (dataPtr < 2)
        return;

    dataPtr = 0;


    int mode = data[0];
    int value = data[1];


    switch (mode) {
        case 1: // ustaw wartość PWM
        {
            OCR0A = 255 - value;
            break;
        }

        case 2://flagi wyjść
        {
            //------------ PIR  ----------------
            if (value & 0x01)
                LOW(PIR_PWR); // wylacz
            else
                HIGH(PIR_PWR); // wylacz

            //------------  BAT  --------------
            if (value & 0x02)
                LOW(BAT_PWR); // wylacz
            else
                HIGH(BAT_PWR); // wylacz
            break;
        }


    }

}

void init(void) {

    PORTB = 255;
    PORTC = 255;
    PORTD = 255;
    // --------------- USB ----------------------

    INPUT(USB_RX);
    OUTPUT(USB_TX);


    // ------------------- Czujnik PIR ------------
    OUTPUT(PIR_PWR);
    OUTPUT(PIR_PWM);
    LOW(PIR_PWM);
    INPUT(PIR_STATE);

    // ------------ �adowarka 1 --------------
    OUTPUT(BAT_PWR);
    INPUT(BAT_STATE);
    INPUT(BAT_2);

    // ------------------ Z��cze --------------

    INPUT(SOCKET_A);
    INPUT(SOCKET_B);


    //---------- PWM ---------------
    TCCR0A |= ((1 << COM0A1) | (1 << COM0A0) | (1 << WGM01) | (1 << WGM00)); // COM0A1 - COM0A0 (Set OC0A on Compare Match, clear OC0A at TOP) WGM01 - WGM00 (set fast PWM)
    TCCR0B |= (1 << CS01); // Start timer at Fcpu / 256
    OCR0A = 255;


    // ---------- timer dla beep-a --------
    TCCR2A |= (1 << WGM22); // Configure timer 2 for CTC mode
    TCCR2B |= 7; // Start timer at Fcpu/64


    uart_init();

    sei();
}

// ===================== beep ==================
volatile uint8_t beepCountdown;

void beep(int time) {
    OUTPUT(BUZZER);
    LOW(BUZZER);
    beepCountdown = (time * 3);
    OCR2A = 255;
    OCR2B = 255;
    TIMSK2 |= (1 << OCIE2A); // Enable CTC interrupt

}

ISR(TIMER2_COMPA_vect) {
    if (!beepCountdown)
        return;

    --beepCountdown;

    if (beepCountdown)
        return;

    HIGH(BUZZER);
}


