/* 
 * File:   main.c
 * Copyright: INOUE Hirokazu
 * GNU GPL Free Software
 *
 * Version 1.0   2014/08/16
 */

#include <stdio.h>
#include <stdlib.h>
#include <xc.h>
#include "serial-lib.h"

/*
 * PIC 16F1827
 *   pin 5 : VSS -> GND
 *   pin 7 : RB1 (Serial RX)
 *   pin 8 : RB2 (Serial TX)
 *   pin 9 : RB3 (PWM, to IR LED)
 *   pin14 : VDD -> 3.3V
 *   pin17 : RA0 -> Status LED
 *   pin18 : RA1 -> Status LED
 */


/* PIC Configuration 1 */
__CONFIG(FOSC_INTOSC &	// INTOSC oscillator: I/O function on CLKIN pin
		WDTE_OFF &		// WDT(Watchdog Timer) disabled
		PWRTE_ON &		// PWRT(Power-up Timer) disabled
		MCLRE_OFF &		// MCLR pin function is digital input
		CP_OFF &		// Program memory code protection is disabled
		CPD_OFF &		// Data memory code protection is disabled
		BOREN_OFF &		// BOR(Brown-out Reset) disabled
		CLKOUTEN_OFF &	// CLKOUT function is disabled. I/O or oscillator function on the CLKOUT pin
		IESO_OFF &		// Internal/External Switchover mode is disabled
		FCMEN_OFF);		// Fail-Safe Clock Monitor is disabled

/* PIC Configuration 2 */
__CONFIG(WRT_OFF &		// Flash Memory Self-Write Protection : OFF
		VCAPEN_OFF &	// VDDCORE pin functionality is disabled
		PLLEN_OFF &		// 4x PLL disabled
		STVREN_ON &		// Stack Overflow or Underflow will not cause a Reset
		BORV_HI &		// Brown-out Reset Voltage Selection : High Voltage
		DEBUG_OFF &		// In-Circuit Debugger disabled, ICSPCLK and ICSPDAT are general purpose I/O pins
		LVP_OFF);		// Low-voltage programming : disable

#ifndef _XTAL_FREQ
	/* 例：4MHzの場合、4000000 をセットする */
	#define _XTAL_FREQ 4000000
#endif

// serial_lib.c よりコピー
#define MAX_BUFFER_SIZE	179

// delay_us_** 関数中のwhileループ内はアセンブラコード8命令なので、8*4=32クロック消費
// 32クロック * (1sec/4,000,000Hz) = 8 マイクロ秒 を消費する

// 20マイクロ秒のcntr倍ディレイ
void delay_us_20(unsigned char cntr){
    while(1){
        __delay_us(13);         // 20us - 7us = 13us
        cntr--;
        if(cntr == 0) break;
    }
}

// 50マイクロ秒のcntr倍ディレイ
void delay_us_50(unsigned char cntr){
    while(1){
        __delay_us(43);         // 50us - 7us = 43us
        cntr--;
        if(cntr == 0) break;
    }
}

void delay_us_100(unsigned char cntr){
    while(1){
        __delay_us(93);         // 100us - 7us = 93us
        cntr--;
        if(cntr == 0) break;
    }
}

void decode_bit(unsigned char c, unsigned char t_hi, unsigned char t_lo_1, unsigned char t_lo_0){
    for(int i=0; i<8; i++){
        if(c & (0x80 >> i)){
            CCP1CON = 0b00001100;
            delay_us_20(t_hi);
            CCP1CON = 0b00000000;
            delay_us_20(t_lo_1);
        }
        else{
            CCP1CON = 0b00001100;
            delay_us_20(t_hi);
            CCP1CON = 0b00000000;
            delay_us_20(t_lo_0);

        }
    }
}

int main(int argc, char** argv) {
    // 基本機能の設定
    OSCCON = 0b01101010;        // 内部オシレーター 4MHz
    TRISA = 0b00010000;         // IOポートRA0-RA4,RA6-RA7を出力モード, RA5は入力専用
    ANSELA = 0b00000000;        // A/D変換をANA0-ANA4を無効
    PORTA = 0;

    TRISB = 0b00000010;         // IOポート RB0,RB2-RB7を出力モード, RB1を入力モード(RX)
    ANSELB = 0b00000000;        // A/D変換をANB1-ANB7を無効
    APFCON0bits.RXDTSEL = 0;    // シリアルポート RXをRB1ピンに割付
    APFCON1bits.TXCKSEL = 0;    // シリアルポート TXをRB2ピンに割付
    PORTB = 0;

    CCP1SEL = 0;                // CCP1(PWM)のP1Aで用いるピン 0:RB3, 1:RB0
    CCP1CON = 0b00000000;       // PWM(シングル)
    T2CON   = 0b00000000;       // TMR2プリスケーラ値を1倍に設定(bit 0~1), Timer2はこの時点ではOFF(bit 2)
    // PR2 = Fosc / (Fpwm * 4 * Prescale) + 1 = 4000000 / (38000 * 4 * 1) - 1 = 25.3
    PR2 = 0x19;                 // 25.3→25
    CCPR1L = 0x0c;              // PR2=0x19の場合デューティ値は6bitで、デューティ比50%の設定
    CCPR1H  = 0;
    TMR2 = 0;                   // Timer2カウンタを0

    TMR2ON = 1;

    char i, j;              // ループ処理用カウンタ一時変数
    char data_length;       // rs232c_buffer[] のサイズ(Bytes)

    for(i=0; i<5; i++){
        PORTAbits.RA0 = 1;  // 状態表示LED(Yellow)
        PORTAbits.RA1 = 1;  // 状態表示LED(Red)
        // 1秒待つ
        __delay_ms(300);
        PORTAbits.RA0 = 0;  // 状態表示LED(Yellow)
        PORTAbits.RA1 = 0;  // 状態表示LED(Red)
        __delay_ms(300);
    }

    rs232c_init(9);         // init 9600bps (Bluetooth Serial Adapter default speed)
    // RS-232C受信割り込みの有効化
    rs232c_receive_interrupt_start();

    while(1){
        if(flag_rs232c_received){
            rs232c_receive_interrupt_stop();

            // シリアル受信バッファ内のデータ バイト数を得る
            for(i=0, data_length=0; i<MAX_BUFFER_SIZE; i++){
                if(rs232c_buffer[i] == (char)0)
                {
                    data_length = i;
                    break;
                }
            }
            // 受信データが11バイトより大きい場合 (5+3+3=11: タイミング+モード+データ)
            if(data_length>11 && i<MAX_BUFFER_SIZE-1){
                PORTAbits.RA0 = 1;  // 状態表示LED(Yellow)

                // PWM周波数、デューティ比を設定
                PR2 = rs232c_buffer[6];
                CCPR1L = rs232c_buffer[7];

                if((rs232c_buffer[5] & 0x0f)  == 1){      // *** NEC/家電協コード送信
                    for(j=(rs232c_buffer[5] & 0x10) ? 0 : 1; j<=1; j++){     // 2回連続送信の判定
                        // START BIT
                        CCP1CON = 0b00001100;
                        delay_us_100(rs232c_buffer[0]);
                        CCP1CON = 0b00000000;
                        delay_us_100(rs232c_buffer[1]);

                        // DATA (データは8バイト目以降）
                        for(i=8; i < data_length; i++){
                            decode_bit(rs232c_buffer[i], rs232c_buffer[2], rs232c_buffer[3], rs232c_buffer[4]);
                        }

                        // STOP BIT
                        CCP1CON = 0b00001100;
                        delay_us_20(rs232c_buffer[2]);
                        CCP1CON = 0b00000000;
                        __delay_ms(30);
                    }
                }
                else if((rs232c_buffer[5] & 0x0f) == 2){     // PWMのテストコードを連続送信
                    CCP1CON = 0b00001100;
                    // PWMをONにしたまま放置する
                }
                else if((rs232c_buffer[5] & 0x0f) == 4){     // 0,1のテストコードを20バイト分送信
                    // 101010... 信号テスト
                    for(i=0; i<20; i++){
                        decode_bit((char)0xaa, rs232c_buffer[2], rs232c_buffer[3], rs232c_buffer[4]);
                    }
                }
                else if((rs232c_buffer[5] & 0x0f) == 8){     // RAWコードを送信
                    char led_on = 1;
                    // DATA (データは8バイト目以降）
                    for(i=8; i < data_length; i++){
                        if(led_on){ CCP1CON = 0b00001100; led_on = 0; }
                        else{ CCP1CON = 0b00000000; led_on = 1; }
                        delay_us_50(rs232c_buffer[i]);
                    }
                    CCP1CON = 0b00000000;
                }

                PORTAbits.RA0 = 0;  // 状態表示LED(Yellow)
                rs232c_puts("ok\n");

            }
            else{
                // 受信エラーの場合、点滅
                for(i=0; i<5; i++){
                    PORTAbits.RA1 = 1;  // 状態表示LED(Red)
                    __delay_ms(300);
                    PORTAbits.RA1 = 0;  // 状態表示LED(Red)
                    __delay_ms(300);
                }
                rs232c_puts("NG\n");
            }
            rs232c_receive_interrupt_start();
        }

        __delay_ms(480);
        rs232c_puts("standby\n");

        PORTAbits.RA1 = 1;  // 状態表示LED(Red)
        __delay_ms(20);
        PORTAbits.RA1 = 0;  // 状態表示LED(Red)
    }

    return (EXIT_SUCCESS);
}

