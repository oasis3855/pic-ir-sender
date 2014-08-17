/*
 * File:   serial-lib.h   for Microchip PIC XC8
 */

#ifndef SERIAL_LIB_H
#define	SERIAL_LIB_H

#ifdef	__cplusplus
extern "C" {
#endif

extern char flag_rs232c_received;    // ■■ 受信機能を使わない場合はコメントアウト
extern char rs232c_buffer[];	    // ■■ 受信機能を使わない場合はコメントアウト
void rs232c_receive_interrupt_start(void);
void rs232c_receive_interrupt_stop(void);

void rs232c_init(const char bps);
void rs232c_putch(const char c);
void rs232c_puts(const char * s);


#ifdef	__cplusplus
}
#endif

#endif	/* SERIAL_LIB_H */

