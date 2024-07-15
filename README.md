## 赤外線リモコンのエミュレーションのための送信回路サンプル<!-- omit in toc -->
---

[Home](https://oasis3855.github.io/webpage/) > [Software](https://oasis3855.github.io/webpage/software/index.html) > [Software Download](https://oasis3855.github.io/webpage/software/software-download.html) > ***pic-ir_sender*** (this page)

<br />
<br />

- [概要](#概要)
- [リモコンの赤外線信号を読み取り、タイミングチャートを作成する](#リモコンの赤外線信号を読み取りタイミングチャートを作成する)
- [タイミングチャートを16進数データに変換する](#タイミングチャートを16進数データに変換する)
- [Bluetooth Serial接続で制御する赤外線リモコン出力回路 (PIC 16F1827)](#bluetooth-serial接続で制御する赤外線リモコン出力回路-pic-16f1827)
- [Bluetooth Serial接続で制御する赤外線リモコン出力コントローラ (Android)](#bluetooth-serial接続で制御する赤外線リモコン出力コントローラ-android)
- [サンプルCSVファイル](#サンプルcsvファイル)


<br />
<br />

## 概要

赤外線リモコンの出力を読み取ってデータ化して保存し、Androidスマートフォンで赤外線信号を出力して家電機器などを操作する、一連のプログラムや電子回路。

実用的なデバイスを作ることではなく、赤外線リモコンのコードを解析し、コピーしてマルチリモコンを作るための技術的データ取得を目的としている。

![機能マトリクス](./readme_pics/ir-sender-matrix.png#gh-light-mode-only)
![機能マトリクス](./readme_pics/ir-sender-matrix-dark.png#gh-dark-mode-only)

<br />
<br />

## リモコンの赤外線信号を読み取り、タイミングチャートを作成する

**[ir_reader_rpi](./ir_reader_rpi/) で詳しく説明している**

リモコンの赤外線信号は、次のようなフォーマットで送信されている

![タイミングチャートの例](./ir_reader_rpi/readme_pics/ir-timing.png#gh-light-mode-only)
![タイミングチャートの例](./ir_reader_rpi/readme_pics/ir-timing-dark.png#gh-dark-mode-only)

この赤外線信号を、テキストファイルのタイミングチャート （数値の単位はマイクロ秒）に保存する


    [9066, 4390, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 438, 683, 1571, 683, 1571, 683, 438, 683, 438, 683, 438, 683, 438, 683, 438, 683, 1571, 683, 438, 683, 438, 683, 1571, 683, 1571, 683, 1571, 683, 39545]

<br />
<br />

## タイミングチャートを16進数データに変換する

**pigpioを使う場合は、[ir_reader_rpi](./ir_reader_rpi/) の後段で詳しく説明している**

**LIRCを使う場合は、[lirc_conf_to_csv](./lirc_conf_to_csv/)で詳しく説明している**


赤外線信号で送信されているパケットデータは、次のようなフォーマットである

![パケットの例](./ir_reader_rpi/readme_pics/ir-packet.png#gh-light-mode-only)
![パケットの例](./ir_reader_rpi/readme_pics/ir-packet-dark.png#gh-dark-mode-only)

タイミングチャートから16進数データへ、次のような段階を経て変換していく

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

## Bluetooth Serial接続で制御する赤外線リモコン出力回路 (PIC 16F1827)

**[ir_sender_16f1827](./ir_sender_16f1827/)で詳細に説明している**

Androidスマートフォンのアプリ [ir_sender_android](./ir_sender_android/) から Bluetooth を介してコントロールすることができる、赤外線リモコン信号出力デバイス

<br />
<br />

## Bluetooth Serial接続で制御する赤外線リモコン出力コントローラ (Android)

**[ir_sender_android](./ir_sender_android/)で詳細に説明している**

Bluetooth を介してコントロールすることができる、赤外線リモコン信号出力デバイス [ir_sender_16f1827](./ir_sender_16f1827/) を用いて、簡単にリモコン信号を送信できるAndroidアプリ

<br />
<br />

## サンプルCSVファイル

**[sample-csv](./sample-csv/)を参照する**

[ir_sender_android](./ir_sender_android/)で読み込む、実際の赤外線リモコンの信号を送信できるCSVファイル


