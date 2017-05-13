#include <avr/io.h>
#include <util/delay.h>
#include <avr/power.h>
#include <avr/eeprom.h>
#include "config.h"
#include "uart.h"
#include "driver.h"
#include "utils.h"
#include <avr/wdt.h>
#include <stdint.h>
#include <avr/interrupt.h>

//#include <avr/iom328p.h>

void main_loop();
void setup_switches();
void quick_test();
void test_swirches();
void process_pwm();
void process_switches(uint8_t second);
uint8_t process_animations(pwm_chnl *c);

uint16_t switchesState = 0;
uint16_t prevSwitchesState = 0;

int main(void) {

    PORTC = 0x00; // wylacz
    DDRC = 0xFF; // ustaw jako wyjscia

#ifdef __AVR_ATmega328P__

#endif


    //while (1) { PORTC = 255; } // testowe - wlaczone

    //  wdt_reset();
    //  wdt_enable(WDTO_1S);
    // wylacz wydłóż timeout watchdoga !wazne (nie wylaczac)

    HIGH(BUSY);
    INPUT(BUSY);
    LOW(BUSY_LED);
    OUTPUT(BUSY_LED);

    setup_switches();

    power_adc_disable();
    power_spi_disable();
    power_timer0_disable();
    power_timer1_disable();
    power_timer2_disable();
    power_twi_disable();

    _delay_ms(30);
    wdt_enable(WDTO_15MS);

    uart_init();
    driver_init();
    
    sendHello();

    // po wlaczenu zasilania zaswiec i przyciemnij wszystkie zarowki
    for (uint8_t i = 0; i < 6; i++) {
        out[i].speed = 3;
        out[i].destination = 0;
        out[i].value = 80;
        out[i].animValue = 80;
        out[i].delay = 30 + (i + 2) * 15;
    }

    /*for (uint8_t i = 0; i < 6; i++) {
     out[i].speed = 4;
     out[i].destination = 4095;
     out[i].value = 0;
     out[i].animValue = 0;
     }
     */
    
    main_loop();
}

void main_loop() {

    uint8_t counter = 0;
    uint8_t pwmCntr = 0;

    for (;;) {
        wdt_reset();

        if (pwmCntr == 6)
            pwmCntr = 0;

        if (!process_animations(&out[pwmCntr++]))
            _delay_us(500);

        if (!process_animations(&out[pwmCntr++]))
            _delay_us(500);

        process_pwm();

        switch (counter++) {
            case 2:
                process_switches(0);
                break;
            case 5:
                process_switches(255);

                if (switchesState || prevSwitchesState) {
                    sendCommand(CMD_SWITCHES_STATE, 0, switchesState);
                    prevSwitchesState = switchesState;
                    switchesState = 0;

                }

                break;
        }

        uart_flush();
        driver_checkReceivedData();

        if (counter == 6)
            counter = 0;
    }
}

uint8_t process_animations(pwm_chnl *c) {

    if (c->value == c->destination)
        return 0;

    if (c->delay) {
        c->delay--;
        setBusy();
        return 0;
    }

    if (c->speed == 0) {
        c->value = c->destination;
        c->animValue = (double) c->destination;
        return 0;
    }

    if (c->speed == 15) {
        c->destination = c->value; // zatrzymaj animację
        return 0;
    }

    double step = (double) c->speed * (double) c->speed;

    setBusy();

    if (step > 0)
        step = (double) (c->value + 1) / step;

    step += 0.1;

    if (c->value < c->destination) {

        c->animValue += step;
        c->value = (uint16_t) c->animValue;

        if (c->value > c->destination) {
            c->value = c->destination;
            c->animValue = (double) c->value;
        }

        return 1;
    }

    if (c->value > c->destination) {

        if (c->animValue - step > 0)
            c->animValue -= step;
        else
            c->animValue = 0;

        c->value = (uint16_t) c->animValue;

        if (c->value < c->destination) {
            c->value = c->destination;
            c->animValue = (double) c->value;
        }

    }

    return 1;
}

uint8_t lastSwErrTS = 0;

void swError(uint8_t swNumber) {
    if (lastSwErrTS) {
        --lastSwErrTS;
        return;
    }
    error2(ERR_SWITCH_STATE, swNumber);
    lastSwErrTS = 150; // wysyla komunikat co ok 1s
    setBusy();
}

void process_switches(uint8_t second) {

#ifndef USE_FIRST_SW_PORT
    return;
#endif

#ifdef USE_FIRST_SW_PORT

    // sprawdz, czy nie ma zwarc (czy nie sa zerami)
    if (!READ(SW1)) {
        swError(1);
        return;
    }
    if (!READ(SW2)) {
        swError(2);
        return;
    }
    if (!READ(SW3)) {
        swError(3);
        return;
    }
    if (!READ(SW4)) {
        swError(4);
        return;
    }

#endif
#ifdef USE_SECOND_SW_PORT
    if (!READ(SW5)) {
        swError(5);
        return;
    }
    if (!READ(SW6)) {
        swError(6);
        return;
    }
    if (!READ(SW7)) {
        swError(7);
        return;
    }
    if (!READ(SW8)) {
        swError(8);
        return;
    }
#endif

    INPUT(SWA);
    HIGH(SWA);
    if (!READ(SWA)) {
        swError(10);
        return;
    }

    INPUT(SWB);
    HIGH(SWB);
    if (!READ(SWB)) {
        swError(11);
        return;
    }

    if (!second) {
        OUTPUT(SWA);
        LOW(SWA);
        INPUT(SWB);
        HIGH(SWB);
#ifdef USE_FIRST_SW_PORT
        if (!READ(SW1))
            switchesState |= (1 << 0);
        if (!READ(SW2))
            switchesState |= (1 << 1);
        if (!READ(SW3))
            switchesState |= (1 << 2);
        if (!READ(SW4))
            switchesState |= (1 << 3);
#endif

#ifdef USE_SECOND_SW_PORT
        if (!READ(SW5))
            switchesState |= (1 << 8);
        if (!READ(SW6))
            switchesState |= (1 << 9);
        if (!READ(SW7))
            switchesState |= (1 << 10);
        if (!READ(SW8))
            switchesState |= (1 << 11);
#endif
    }

    if (second) {

        OUTPUT(SWB);
        LOW(SWB);
        INPUT(SWA);
        HIGH(SWA);
#ifdef USE_FIRST_SW_PORT

        if (!READ(SW1))
            switchesState |= (1 << 4);
        if (!READ(SW2))
            switchesState |= (1 << 5);
        if (!READ(SW3))
            switchesState |= (1 << 6);
        if (!READ(SW4))
            switchesState |= (1 << 7);
#endif
#ifdef USE_SECOND_SW_PORT
        if (!READ(SW5))
            switchesState |= (1 << 12);
        if (!READ(SW6))
            switchesState |= (1 << 13);
        if (!READ(SW7))
            switchesState |= (1 << 14);
        if (!READ(SW8))
            switchesState |= (1 << 15);
#endif
    }

    INPUT(SWA);
    HIGH(SWA);
    INPUT(SWB);
    HIGH(SWB);

}

void setup_switches() {
#ifdef USE_FIRST_SW_PORT
    INPUT(SWA);
    INPUT(SWB);
    INPUT(SW1);
    INPUT(SW2);
    INPUT(SW3);
    INPUT(SW4);

    HIGH(SWA);
    HIGH(SWB);
    HIGH(SW1);
    HIGH(SW2);
    HIGH(SW3);
    HIGH(SW4);

#endif

#ifdef USE_SECOND_SW_PORT
    INPUT(SW5);
    INPUT(SW6);
    INPUT(SW7);
    INPUT(SW8);
    HIGH(SW5);
    HIGH(SW6);
    HIGH(SW7);
    HIGH(SW8);
#endif

}

void process_pwm() {

    /*
     * #define out0  C,5
     #define out1  C,2
     #define out2  C,0
     #define out3  C,1
     #define out4  C,4
     #define out5  C,3
     */

    const uint16_t pwm0 = 4095 - out[2].value;
    const uint16_t pwm1 = 4095 - out[3].value;
    const uint16_t pwm2 = 4095 - out[1].value;
    const uint16_t pwm3 = out[5].value;
    const uint16_t pwm4 = out[4].value;
    const uint16_t pwm5 = out[0].value;

    uint8_t port = 0x38;

    for (uint16_t i = 0; i < 4095; i++) {

        if (pwm0 == i)
            port |= (1 << 0);

        if (pwm1 == i)
            port |= (1 << 1);

        if (pwm2 == i)
            port |= (1 << 2);

        if (pwm3 == i)
            port &= ~(1 << 3);

        if (pwm4 == i)
            port &= ~(1 << 4);

        if (pwm5 == i)
            port &= ~(1 << 5);

        //port = i > 4000 ? _BV(6) : 0;

        PORTC = port;

    }

    port = 0;

    if (out[2].value > 4092)
        port |= 0x01;

    if (out[3].value > 4092)
        port |= 0x02;

    if (out[1].value > 4092)
        port |= 0x04;

    if (out[5].value > 4092)
        port |= 0x08;

    if (out[4].value > 4092)
        port |= 0x10;

    if (out[0].value > 4092)
        port |= 0x20;

    PORTC = port;
}
