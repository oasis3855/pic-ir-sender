## ![icon](../readme_pics/rpi-icon.png) 赤外線リモコン信号を読み取る (Raspberry Pi)<!-- omit in toc -->

---
[Home](https://oasis3855.github.io/webpage/) > [Software](https://oasis3855.github.io/webpage/software/index.html) > [Software Download](https://oasis3855.github.io/webpage/software/software-download.html) > [pic-ir_sender](../README.md) > ***ir_reader_rpi*** (this page)

<br />
<br />

Last Updated : July. 2024

- [概要](#概要)
- [赤外線信号のタイミングチャートと16進数データ](#赤外線信号のタイミングチャートと16進数データ)
  - [NECフォーマット、家電協(AEHA)フォーマットのタイミングチャート](#necフォーマット家電協aehaフォーマットのタイミングチャート)
  - [赤外線信号の16進数データ](#赤外線信号の16進数データ)
- [赤外線リモコン信号 送受信回路の作成](#赤外線リモコン信号-送受信回路の作成)
  - [トランジスタ周りの抵抗値計算](#トランジスタ周りの抵抗値計算)
- [リモコンの赤外線信号を受信し、タイミングチャートをテキストファイル化する](#リモコンの赤外線信号を受信しタイミングチャートをテキストファイル化する)
  - [LIRCを用いて赤外線信号を受信する方法](#lircを用いて赤外線信号を受信する方法)
    - [LIRCのインストールと初期設定 (Linux kernel 4.14以下)](#lircのインストールと初期設定-linux-kernel-414以下)
    - [LIRCのインストールと初期設定 (Linux kernel 4.19以降)](#lircのインストールと初期設定-linux-kernel-419以降)
    - [赤外線リモコン信号の受信テスト](#赤外線リモコン信号の受信テスト)
    - [赤外線リモコンのタイミングチャートをテキストファイルに保存する](#赤外線リモコンのタイミングチャートをテキストファイルに保存する)
  - [pigpioを用いて赤外線信号を受信する方法 (Linux kernel 6の場合)](#pigpioを用いて赤外線信号を受信する方法-linux-kernel-6の場合)
    - [赤外線リモコンのタイミングチャートをテキストファイルに保存する](#赤外線リモコンのタイミングチャートをテキストファイルに保存する-1)
- [テキスト化したタイミングチャートから、16進数データに変換する](#テキスト化したタイミングチャートから16進数データに変換する)
  - [irrecord(LIRC)で作成したタイミングチャートを16進数データに変換する](#irrecordlircで作成したタイミングチャートを16進数データに変換する)
  - [irrp.py(pigpio)で作成したタイミングチャートを16進数データに変換する](#irrppypigpioで作成したタイミングチャートを16進数データに変換する)
- [ドキュメントのバージョン情報](#ドキュメントのバージョン情報)
- [他のWebサイトで配布されいてるスクリプトのバックアップコピー](#他のwebサイトで配布されいてるスクリプトのバックアップコピー)


<br />
<br />

## 概要

Raspberry Piに赤外線受信モジュールを接続し、リモコン信号のHi/Loタイミング（タイミングチャート）をテキストファイル化する方法の説明。

<br />
<br />

## 赤外線信号のタイミングチャートと16進数データ

### NECフォーマット、家電協(AEHA)フォーマットのタイミングチャート

![タイミングチャートの例](./readme_pics/ir-timing.png#gh-light-mode-only)
![タイミングチャートの例](./readme_pics/ir-timing-dark.png#gh-dark-mode-only)

この赤外線信号を読み取ってテキスト化したタイミングチャートの例を次に示す。（NECフォーマットの例）

    9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, …

このタイミングチャートは、Hi=9066us, Lo=4390us, Hi=683us, ... という赤外線信号を意味している。

### 赤外線信号の16進数データ

タイミングチャートから、ヘッダーとトレイラーを除去し、データビットを16進数にデコードしたものを、赤外線信号のデータと呼ぶこととする。

![パケットの例](./readme_pics/ir-packet.png#gh-light-mode-only)
![パケットの例](./readme_pics/ir-packet-dark.png#gh-dark-mode-only)

タイミングチャートから赤外線信号データへ、デコードの段階を追って実例を示す。

タイミングチャート （数値の単位はマイクロ秒）

    [9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 39545]

リーダー部が「Hi=9000us, Lo=4500us」のNECフォーマット、「Hi=3200us, Lo=1600us」の家電協フォーマットなのか。この時点で判断して、T単位に変換する

T単位（T=約560マイクロ秒）

    [20, 10, 1, 1, 1, 3, 1, 1, 1, 3, 1, 3, 1, 3, 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 1, 1, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 90]

T単位表記のリーダーとトレイラーを除去して、データ部分のみを2進数表現する。

赤外線信号の2進数表記（1T・3T→1, 1T・1T→0, 16T+8T→header）

    [0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0]

0101 → 5, 1110 → E … と16進表記に変換する。

赤外線信号の16進数表記

    [5E, A1, D8, 27]

<br />
<br />

## 赤外線リモコン信号 送受信回路の作成

![回路図](./readme_pics/ir_receive_send_circuit.png#gh-light-mode-only)
![回路図](./readme_pics/ir_receive_send_circuit-dark.png#gh-dark-mode-only)

|名称|型番・仕様|個数|参考価格|
|---|---|---|---|
|赤外線受信モジュール|IRM-3638N3 (940nm,38kHz,Vcc=0〜6V)|1個|216円|
|赤外線LED|L53F3BT (940nm, V=5.0V, I=50mA)|1個|21円|
|NPNトランジスタ|2SC1815|1個|21円|
|炭素皮膜抵抗 Rc|51 Ω|1個|5円|
|炭素皮膜抵抗 Rb|2.4k Ω|1個|5円|

### トランジスタ周りの抵抗値計算 

**コレクタ抵抗（LED側）**

- LED電流値 IF = 50mA 〜 100mA
- LED順方向電圧 VF = 1.2〜1.5V → 1.5V
- 駆動電圧 Vcc = 5.0V

オームの法則より (Vcc - VF) / IF = ( 5.0 - 1.5 ) / [0.050 〜 0.100] = [70 Ω〜35 Ω] → 50 Ω 

**ベース抵抗（スイッチング抵抗）**

- ベース電圧 V = 3.3V 
- 半導体損失 0.6V 
- コレクタ電流 Ic = 70mA ← Rc=50Ωの場合 Ic = ( 5-1.5 ) / 50 = 0.070
- Hfe = 約200(テスタでの実測) → 安全率3として Hfe = 67 

オームの法則より Rb = ( V - 0.6 ) / ( Ic / (Hfe / 3) ) = (3.3 - 0.6) / ( 0.070/67 ) = 2584 Ω → 2.4 kΩ（E24系列） 

<br />
<br />

## リモコンの赤外線信号を受信し、タイミングチャートをテキストファイル化する

LIRCを用いる方法と、pigpioを用いる方法がある。

2024年現在はLIRCを用いる方法はエラーが起きて使えない状態。

<br />
<br />

### LIRCを用いて赤外線信号を受信する方法

lirc-rpi モジュールが存在するLinux kernel 4.14以下では、このセクションで説明する方法を採用する。そうでない場合は、次のセクションで説明するgpio-irモジュールを使う方法を試すか、次のメインセクションで説明するpigpioを用いる。

#### LIRCのインストールと初期設定 (Linux kernel 4.14以下)

LIRCパッケージのインストール

    sudo apt-get install lirc

Raspberry Piを再起動し、***lircdサービスを一旦停止***する

    sudo systemctl stop licd

回路をRaspberry Piに接続してから、モジュールをロードする。今回はGPIO 4, 17を使っているので次のようになる

    sudo modprobe lirc-rpi gpio_in_pin=4 gpio_out_pin=17

/boot/config.txt (/boot/firmware/config.txt) に設定を***追記***する

    # lirc (infrared remote control)
    dtoverlay=lirc-rpi
    dtparam=gpio_in_pin=4
    dtparam=gpio_out_pin=17

/etc/lirc/lirc_options.conf の設定を再確認する

    [lircd]
    nodaemon        = False
    driver          = default
    device          = /dev/lirc0
    #driver          = devinput
    #device          = auto

    output          = /var/run/lirc/lircd
    pidfile         = /var/run/lirc/lircd.pid
    plugindir       = /usr/lib/arm-linux-gnueabihf/lirc/plugins
    permission      = 666
    allow-simulate  = No
    repeat-max      = 600

Raspberry Piを再起動し、lircが正常起動しているか確認する

    $ ls -l /dev/lirc*
    crw-rw---T 1 root video 248, 0  7月 21 12:11 /dev/lirc0
    lrwxrwxrwx 1 root root      21  7月 21 12:57 /dev/lircd -> ../var/run/lirc/lircd

    $ lsmod | grep lirc
    lirc_rpi                10270  2
    lirc_dev                10325  1 lirc_rpi

    $ sudo cat /sys/kernel/debug/gpio | grep ir
    gpio-4   (lirc_rpi ir/in      ) in  hi
    gpio-17  (lirc_rpi ir/out     ) in  lo

<br />
<br />

#### LIRCのインストールと初期設定 (Linux kernel 4.19以降)

LIRCパッケージのインストール

    sudo apt-get install lirc


/boot/config.txt (/boot/firmware/config.txt) に設定を***追記***する

今回はGPIO 4, 17を使っているので次のようになる

    # lirc (infrared remote control)
    dtoverlay=gpio-ir,gpio_pin=4
    dtoverlay=gpio-ir-tx,gpio_pin=17

/etc/lirc/lirc_options.conf の設定を再確認する

    [lircd]
    nodaemon        = False
    driver          = default
    device          = /dev/lirc0
    #driver          = devinput
    #device          = auto

    output          = /var/run/lirc/lircd
    pidfile         = /var/run/lirc/lircd.pid
    plugindir       = /usr/lib/arm-linux-gnueabihf/lirc/plugins
    permission      = 666
    allow-simulate  = No
    repeat-max      = 600

送受信回路を接続し、Raspberry Piを再起動してから、lircが正常起動しているか確認する

    $ ls -l /dev/lirc*
    crw-rw---- 1 root video 251, 0 2024-06-28 19:51:32 /dev/lirc0
    crw-rw---- 1 root video 251, 1 2024-06-28 19:51:32 /dev/lirc1

    $ lsmod | grep gpio
    gpio_ir_recv           12288  0
    gpio_ir_tx             12288  0

    $ sudo cat /sys/kernel/debug/gpio|grep ir
    gpio-516 (GPIO4               |ir-receiver@4       ) in  hi IRQ ACTIVE LOW
    gpio-529 (GPIO17              |gpio-ir-transmitter@) out lo 

<br />
<br />

#### 赤外線リモコン信号の受信テスト

***lircdサービスを一旦停止***する

    sudo systemctl stop licd

赤外線リモコン信号をテスト受信する

    $ mode2 -d /dev/lirc0
    〜 リモコンのボタンを押すと、次のような読み取り結果が表示される 〜
    space 1830900
    pulse 9061
    space 4388
    pulse 785
    space 343
    pulse 728
    space 1543
    pulse 649
    space 453
    〜 以下省略 〜

<br />
<br />

#### 赤外線リモコンのタイミングチャートをテキストファイルに保存する

    $ irrecord -n -d /dev/lirc0 ~/test.conf

    Press RETURN to continue.  ← [ENTER]キーを押す

    Press RETURN now to start recording.     ← [ENTER]キーを押す
    
    ........................................................ ← 学習させたいリモコンのボタンを、どんどん押してゆく

    Found const length: 107659
    Please keep on pressing buttons like described above.
    
    irrecord: no data for 10 secs, aborting
    Creating config file in raw mode.
    Now enter the names for the buttons.
    
    Please enter the name for the next button (press <ENTER> to finish recording)
    Button-1 ← 受信したいボタン名[Button-1]をキーボードから入力
    
    Now hold down button "Button-1". ← それに対応するリモコンのボタンを押す
    Got it.
    Signal length is 67
    
    Please enter the name for the next button (press <ENTER> to finish recording)

作成されるテキストファイルは、次のようなものになる

    begin remote
    
    name  家電製品の機器名
    flags RAW_CODES|CONST_LENGTH
    eps            30
    aeps          100
    
    gap          107686
    
        begin raw_codes
    
            name Button-1
                9062    4378     702     427     676    1594
                650     495     649    1556     672    1595
                760    1464     672    1599     650     450
                700    1569     651     451     699    1547
                724     400     723     401     725     398
                779     349     700    1570     778     326
                673    1574     727     397     726    1524
                677    1602     677     424     675     450
                700     425     722    1524     724     401
                752    1501     775     364     729     372
                700    1571     649    1596     679    1569
                655
    
            name Button-2
                9058    4384     674     453     697    1549
                674     450     724    1544     675    1573
                649    1596     649    1576     701     424
                698    1570     649     475     702    1522
                675     456     703     417     681     441
                699     427     776    1471     673    1596
                676    1574     649     454     724    1528
                693    1547     671     452     698     428
                699     424     699     427     698     426
                696    1571     679     424     675     451
                697    1552     669    1576     669    1599
                650
    
            name Button-3
                9136    4313     674     451     726    1522
                725     398     723    1546     676    1570
    〜 中略 〜
                649     452     698    1549     674     448
                698    
    
        end raw_codes
    
    end remote

<br />
<br />

### pigpioを用いて赤外線信号を受信する方法 (Linux kernel 6の場合)

pigpioライブラリをaptコマンドでインストールする。

    sudo apt install pigpio
    sudo apt install python3-pigpio

pigpio の作者Webサイト（[http://abyz.me.uk/rpi/pigpio/examples.html](http://abyz.me.uk/rpi/pigpio/examples.html)）より、irrp_pyをダウンロードする。

- IR Record and Playback (irrp_py.zip)
  - 2015-12-21 This script may be used to record and play back arbitrary IR codes.

<br />
<br />

#### 赤外線リモコンのタイミングチャートをテキストファイルに保存する

リモコンのボタン2つ（button1, button2）を読み取って、テキストファイルに保存する

    $ python3 irrp.py -r -g 4 -f testfile.json --post 100 --no-confirm button1 button2

    Recording
    Press key for 'button1'
    Short code, probably a repeat, try again
    Okay
    Press key for 'button2'
    Short code, probably a repeat, try again
    Okay

保存されたテキストファイルの内容

    $ cat testfile.json 

    {"button1": [9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 39545, 9066, 2146, 683, 95866, 9066, 2146, 683, 95866, 9066, 2146, 683],

    "button2": [9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 39545, 9066, 2146, 683, 95866, 9066, 2146, 683, 95866, 9066, 2146, 683]}


<br />
<br />

## テキスト化したタイミングチャートから、16進数データに変換する

タイミングチャートを見て、NECフォーマットか家電協フォーマットか判断し、リーダーとトレイラーを除去してデータ部分を取り出す作業を、完全手動で行うこともできるが、ここではある程度自動化する方法を説明する。

<br />
<br />

### irrecord(LIRC)で作成したタイミングチャートを16進数データに変換する

拙作 [lirc_nec_decode.pl スクリプト](../lirc_conf_to_csv/)を利用して、16進数データに変換することができる。

**変換例１**

タイミングチャートを保存したファイル test_nec.conf

    begin remote

    name  家電製品の機器名
    flags RAW_CODES|CONST_LENGTH
    eps            30
    aeps          100

    gap          107686

        begin raw_codes

            name Button-1
                9062    4378     702     427     676    1594
                650     495     649    1556     672    1595
                760    1464     672    1599     650     450
                700    1569     651     451     699    1547
                724     400     723     401     725     398
                779     349     700    1570     778     326
                673    1574     727     397     726    1524
                677    1602     677     424     675     450
                700     425     722    1524     724     401
                752    1501     775     364     729     372
                700    1571     649    1596     679    1569
                655

            name Button-2
                9058    4384     674     453     697    1549
                674     450     724    1544     675    1573
                649    1596     649    1576     701     424
                698    1570     649     475     702    1522
                675     456     703     417     681     441
                699     427     776    1471     673    1596
                676    1574     649     454     724    1528
                693    1547     671     452     698     428
                699     424     699     427     698     426
                696    1571     679     424     675     451
                697    1552     669    1576     669    1599
                650

        end raw_codes

    end remote

lirc_nec_decode.plを使って16進数データ化する

    $ perl lirc_nec_decode.pl test_nec.conf 
    Button-1 = 5E,A1,58,A7,
    Button-2 = 5E,A1,D8,27,


<br />
<br />

### irrp.py(pigpio)で作成したタイミングチャートを16進数データに変換する

Qiitaの@takusan64氏が開設しているWebページ『[リモコンが家電を動かす仕組みを理解する【ラズパイで遊ぼう！】](https://qiita.com/takusan64/items/777fce520bde3c935637)』に掲載されいるdecode.pyを用いると、NECフォーマット／家電協フォーマットのタイミングチャートを16進数データに簡単に変換できる。

**変換例１**

タイミングチャートを保存したファイル test_nec.json

    {"button1": [9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 39545, 9066, 2146, 683, 95866, 9066, 2146, 683, 95866, 9066, 2146, 683]}

decode.pyを使って16進数データ化する

    $ python3 ./decode.py -t 438 -f test_nec.json button1
    T: [20, 10, 1, 1, 1, 3, 1, 1, 1, 3, 1, 3, 1, 3, 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 1, 1, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 90, 20, 4, 1, 218, 20, 4, 1, 218, 20, 4, 1]
    bin: [0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0]
    hex: ['5', 'E', 'A', '1', 'D', '8', '2', '7']

結果は 5E, A1, D8, 27

<br />
<br />

**変換例１**

タイミングチャートを保存したファイル test_aeha.json

    {"button1": [3548, 1663, 532, 338, 532, 338, 532, 1208, 532, 1208, 532, 338, 532, 1208, 532, 338, 532, 338, 532, 338, 532, 1208, 532, 338, 532, 338, 532, 1208, 532, 338, 532, 1208, 532, 338, 532, 1208, 532, 338, 532, 338, 532, 1208, 532, 338, 532, 338, 532, 338, 532, 338, 532, 1208, 532, 1208, 532, 1208, 532, 338, 532, 1208, 532, 1208, 532, 338, 532, 338, 532, 338, 532, 1208, 532, 1208, 532, 1208, 532, 1208, 532, 1208, 532, 338, 532, 338, 532]}

decode.pyを使って16進数データ化する

    $ python3 ./decode.py -t 338 -f test_aeha.json button1
    T: [10, 4, 1, 1, 1, 1, 1, 3, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 3, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 1, 1, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 1, 3, 1, 3, 1, 3, 1, 1, 1, 1, 1]
    bin: [0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0]
    hex: ['3', '4', '4', 'A', '9', '0', 'E', 'C', '7', 'C']

結果は 34, 4A, 90, EC, 7C

<br />
<br />

## ドキュメントのバージョン情報

- Version 1.0 (2014/08/16)
  - irrecord(LIRC)版の説明
- Version 2.0 (2024/07/15)
  - タイミングチャート、16進数データの解説図
  - irrp.py(pigpio)版説明
  - 解説サイトをGitHub（ここ）に移行

<br />
<br />

## 他のWebサイトで配布されいてるスクリプトのバックアップコピー

このセクションでリンクしているzipファイルは、他のWebサイトで配布されているスクリプトです。

個人が契約しているWebサーバは、本人死去などの場合に契約解除され、永久に失われてしまう可能性があるため、ぜひ残存してほしい必須のスクリプトを取り出して、このリポジトリ内にzip圧縮して保存しています。

***配布条件はそれぞれのWebサイトの規約によります。*** また、各Webサイトが存在している間は、そこからダウンロードし、このリポジトリからダウンロードする必要はありません。

- ![download](../readme_pics/soft-ico-download-darkmode.gif) [irrp_py.zip](./download/irrp_py.zip) : pigpio の作者Webサイト（[http://abyz.me.uk/rpi/pigpio/examples.html](http://abyz.me.uk/rpi/pigpio/examples.html)）より、irrp_py

- ![download](../readme_pics/soft-ico-download-darkmode.gif) [decode.py.zip](./download/decode.py.zip) : Qiitaの@takusan64氏が開設しているWebページ『[リモコンが家電を動かす仕組みを理解する【ラズパイで遊ぼう！】](https://qiita.com/takusan64/items/777fce520bde3c935637)』に掲載されいるdecode.py