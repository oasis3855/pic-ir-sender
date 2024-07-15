## ![icon](../readme_pics/linux-tux-icon.png) LIRCタイミングチャートを16進数データに変換する<!-- omit in toc -->

---
[Home](https://oasis3855.github.io/webpage/) > [Software](https://oasis3855.github.io/webpage/software/index.html) > [Software Download](https://oasis3855.github.io/webpage/software/software-download.html) > [pic-ir_sender](../README.md) > ***lirc_conf_to_csv*** (this page)

<br />
<br />

Last Updated : Aug. 2014

- [ソフトウエアのダウンロード](#ソフトウエアのダウンロード)
- [概要](#概要)
- [赤外線信号のタイミングチャートと16進数データ](#赤外線信号のタイミングチャートと16進数データ)
  - [NECフォーマット、家電協(AEHA)フォーマットのタイミングチャート](#necフォーマット家電協aehaフォーマットのタイミングチャート)
- [赤外線信号の16進数データ](#赤外線信号の16進数データ)
- [irrecord(LIRC)で作成したタイミングチャートを16進数データに変換する](#irrecordlircで作成したタイミングチャートを16進数データに変換する)
- [バージョン情報](#バージョン情報)
- [ライセンス](#ライセンス)


<br />
<br />

## ソフトウエアのダウンロード

- ![download icon](../readme_pics/soft-ico-download-darkmode.gif)   [このGitHubリポジトリを参照する](../lirc_conf_to_csv/) 

- ![download icon](../readme_pics/soft-ico-download-darkmode.gif)   [GoogleDriveを参照する](https://drive.google.com/drive/folders/0B7BSijZJ2TAHV0VGQ3QwdmlBWUU)

<br />
<br />

## 概要

Raspberry Piに赤外線受信モジュールを接続し、リモコン信号を読み取ったHi/Loタイミング（タイミングチャート）を16進数データファイル化する方法の説明。

<br />
<br />

## 赤外線信号のタイミングチャートと16進数データ

### NECフォーマット、家電協(AEHA)フォーマットのタイミングチャート

![タイミングチャートの例](../ir_reader_rpi/readme_pics/ir-timing.png#gh-light-mode-only)
![タイミングチャートの例](../ir_reader_rpi/readme_pics/ir-timing-dark.png#gh-dark-mode-only)

この赤外線信号を読み取ってテキスト化したタイミングチャートの例を次に示す。（NECフォーマットの例）

    9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, …

このタイミングチャートは、Hi=9066us, Lo=4390us, Hi=683us, ... という赤外線信号を意味している。

## 赤外線信号の16進数データ

タイミングチャートから、ヘッダーとトレイラーを除去し、データビットを16進数にデコードしたものを、赤外線信号のデータと呼ぶこととする。

![パケットの例](../ir_reader_rpi/readme_pics/ir-packet.png#gh-light-mode-only)
![パケットの例](../ir_reader_rpi/readme_pics/ir-packet-dark.png#gh-dark-mode-only)

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

## irrecord(LIRC)で作成したタイミングチャートを16進数データに変換する

現在、このリポジトリで配布しているスクリプトはNECフォーマット用のみである。

**変換例**

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

## バージョン情報

- Version 1.0   2014/08/16

<br />
<br />

## ライセンス

このソフトウエアは [GNU General Public License v3ライセンスで公開する](https://gpl.mhatta.org/gpl.ja.html) フリーソフトウエア