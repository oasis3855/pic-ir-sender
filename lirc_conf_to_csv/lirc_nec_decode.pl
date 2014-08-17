#!/usr/bin/perl

# Convert Lirc conf file into csv file
#
# (C) INOUE Hirokazu
# GNU GPL Free Software
#
# Version 1.0   2014/08/16

use warnings;
use strict;
use File::Basename;

my $filename = '';
my $flag_verbose = 0;
my $flag_raw = 0;

if($#ARGV < 0 || $#ARGV >= 2){
    die("\n".basename($0)." - Decode IR remote controller raw code to NEC format\n\n" .
            "usage : ".basename($0)." [-v|-r] filename\n" .
            "  -v : verbose output\n" .
            "  -r : RAW data mode\n");
}

# 引数の解釈
if($#ARGV == 0){
    $filename = $ARGV[0];
}
elsif($ARGV[0] eq '-v'){
    $filename = $ARGV[1];
    $flag_verbose = 1;
}
elsif($ARGV[0] eq '-r'){
    $filename = $ARGV[1];
    $flag_raw = 1;
}
else{
    die("program arguments error\n");
}

sub_main($filename, $flag_verbose);
exit;

sub sub_main{
    my $filename = shift;
    my $flag_verbose = shift;

    unless( -f $filename ){ die("error : file is not exist\n"); }
    unless( -r $filename ){ die("error : file is not readable\n"); }
    if( -z $filename ){ die("error : file length is zero\n"); }

    open(FH, "< ".$filename) or die("file open error :$!");

    my @signal;
    my $keyname = '';

    while (my $line = <FH>){
        $line =~ s/[\r\n]+\z//;         # 行末改行の除去
        $line =~ s/^[ ]+//;             # 行頭空白の除去
        if($line =~ /^#/){ next; }      # コメント行スキップ
        my @item = split(/\s+/, $line); # 複数の空白文字対応(\s+)のsplit
        
        if($#item >= 0){
            if($item[0] =~ /[0-9]+/ && $keyname ne ''){ push(@signal, @item); }
            elsif($#signal >= 0 && $keyname ne ''){
                sub_parse_signal($flag_verbose, $keyname, @signal);
                if($#item == 1 && $item[0] eq 'name'){
                    $keyname = $item[1];
                }
                else{
                    $keyname = '';
                }
                @signal = ();
            }
            elsif($#item == 1 && $item[0] eq 'name'){
                $keyname = $item[1];
                @signal = ();
            }
            else{
                @signal = ();
                $keyname = '';
            }
        }
    }
    close(FH);

}


sub sub_parse_signal{
    my $flag_verbose = shift;
    my $keyname = shift;
    my @signal = @_;

    if($keyname eq '' || $#signal < 0){
        if($flag_verbose){ print "parse error : empty keyname or empty array_signal\n"; }
        return;
    }

    # 文字を数値に変換 ( '123' → 123 )
    for(my $i=0; $i<=$#signal; $i++){
        $signal[$i] += 0;   # 0を足して数値化
    }

    if($flag_verbose){ print "keyname = " . $keyname . "\n"; }

    if($flag_raw == 0){

        my @bits = ();

        # @signal をデコードしてデータ化し、@bits に格納する
        for(my $i=0; $i<=$#signal-1; $i+=2){
            if($signal[$i] > 3000 && $signal[$i] < 12000 && $signal[$i+1] > 1500 && $signal[$i+1] < 6000){
                # START BIT : default = 9000, 4500
                if($flag_verbose){ print "H"; }
            }
            elsif($signal[$i] > 200 && $signal[$i] < 850 && $signal[$i+1] > 200 && $signal[$i+1] < 850){
                if($flag_verbose){ print "0"; }
                push(@bits, '0');
            }
            elsif($signal[$i] > 200 && $signal[$i] < 850 && $signal[$i+1] > 1100 && $signal[$i+1] < 1800){
                if($flag_verbose){ print "1"; }
                push(@bits, '1');
            }
            else{
                if($flag_verbose){ print "X"; }
            }
        }
        if($flag_verbose){ print "\n"; }

        # @signalを画面表示する
        if($flag_verbose){
            foreach my $value (@signal){
                print $value . ",";
            }
            print "\n";
        }

        # @bitsが8ビットの倍数かどうかチェックする
        if($flag_verbose){ print "bits = ". ($#bits+1) . "\n"; }
        if(($#bits+1) % 8 != 0){
            if($flag_verbose){ print "parse error : signal is not 8 multiply bits\n"; }
            return;
        }

        # @bits を8ビットごとに切って、バイトコードに変換する
        if(!$flag_verbose){ print $keyname . " = "; }
        for(my $i=0; $i<($#bits+1)/8; $i++){
            my $bitstr = '0b';
            for(my $j=0; $j<=7; $j++){
                $bitstr .= $bits[$i*8+$j];
            }
            my $val = oct($bitstr);
            printf("%02X,", $val);
        }
        print "\n";
    }
    else{
        print $keyname . " = ";
        for(my $i=0; $i<$#signal; $i++){
            if($signal[$i]/50 > 254){
                print "error : signal length must be under 12700 usec.\n";
                return;
            }
            printf("%02X,", $signal[$i]/50);
        }
        print "\n";
    }
}
